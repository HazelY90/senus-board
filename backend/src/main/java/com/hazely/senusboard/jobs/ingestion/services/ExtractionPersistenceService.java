package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.entities.CapitalEntity;
import com.hazely.senusboard.entities.GrowthEntity;
import com.hazely.senusboard.entities.IngestionRunEntity;
import com.hazely.senusboard.entities.LiquidityEntity;
import com.hazely.senusboard.entities.ProfitabilityEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.entities.SourceDocumentEntity;
import com.hazely.senusboard.entities.enums.IngestionStatus;
import com.hazely.senusboard.entities.enums.PeriodType;
import com.hazely.senusboard.jobs.ingestion.OpenAiProperties;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.DownloadedDocument;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.IngestionRunRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import com.hazely.senusboard.repositories.SourceDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Persists source provenance, ingestion state, periods, and stable category values. */
public class ExtractionPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionPersistenceService.class);

    private final SourceDocumentRepository sourceRepo;
    private final IngestionRunRepository runRepo;
    private final ReportingPeriodRepository periodRepo;
    private final GrowthRepository growthRepo;
    private final ProfitabilityRepository profitRepo;
    private final LiquidityRepository liquidityRepo;
    private final CapitalRepository capitalRepo;
    private final OpenAiProperties openAiProps;
    private final CalculationService calculationService;

    public ExtractionPersistenceService(
            SourceDocumentRepository sourceRepo,
            IngestionRunRepository runRepo,
            ReportingPeriodRepository periodRepo,
            GrowthRepository growthRepo,
            ProfitabilityRepository profitRepo,
            LiquidityRepository liquidityRepo,
            CapitalRepository capitalRepo,
            OpenAiProperties openAiProps,
            CalculationService calculationService
    ) {
        this.sourceRepo = sourceRepo;
        this.runRepo = runRepo;
        this.periodRepo = periodRepo;
        this.growthRepo = growthRepo;
        this.profitRepo = profitRepo;
        this.liquidityRepo = liquidityRepo;
        this.capitalRepo = capitalRepo;
        this.openAiProps = openAiProps;
        this.calculationService = calculationService;
    }

    /** Creates or reuses source metadata and starts a fresh run for the downloaded file. */
    @Transactional(rollbackFor = Exception.class)
    public Long start(DownloadedDocument doc) throws IOException {
        Path file = doc.file().toAbsolutePath().normalize();
        String hash = hash(file);
        SourceDocumentEntity source = sourceRepo.findByFileHash(hash)
                .orElseGet(SourceDocumentEntity::new);
        source.setName(file.getFileName().toString());
        source.setDocumentType(doc.documentType());
        source.setSourceUrl(doc.sourceUrl().toString());
        source.setLocalPath(relativePath(file));
        source.setFileHash(hash);
        source = sourceRepo.save(source);

        IngestionRunEntity run = new IngestionRunEntity();
        run.setSourceDocument(source);
        run.setModelName(openAiProps.getModel());
        run.setStatus(IngestionStatus.RUNNING);
        run.setStartedAt(Instant.now());
        return runRepo.save(run).getId();
    }

    /** Validates and upserts all period category rows, then completes the run atomically. */
    @Transactional
    public void complete(Long runId, AiExtractionResult result) {
        IngestionRunEntity run = getRun(runId);
        requireState(run, IngestionStatus.RUNNING);
        if (result == null) {
            throw new IllegalArgumentException("result is required");
        }

        run.getSourceDocument().setPublicationDate(parseDate(result.publicationDate()));
        run.getSourceDocument().setAiSummary(requireText(result.aiSummary(), "aiSummary"));
        savePeriods(requirePeriods(result.periods()));
        calculationService.recalculate();
        run.setStatus(IngestionStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(null);
        runRepo.save(run);
    }

    /** Marks a started run as failed after extraction or persistence rejects the result. */
    @Transactional
    public void fail(Long runId, Throwable error) {
        IngestionRunEntity run = getRun(runId);
        run.setStatus(IngestionStatus.FAILED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(errorMessage(error));
        runRepo.save(run);
    }

    private void savePeriods(List<AiExtractionResult.PeriodData> items) {
        Set<String> codes = new HashSet<>();
        for (AiExtractionResult.PeriodData item : items) {
            AlignedPeriod aligned = alignPeriod(item);
            if (aligned == null) {
                continue;
            }
            if (!codes.add(aligned.code())) {
                throw new IllegalArgumentException("Duplicate reporting period: " + aligned.code());
            }
            if (isEmpty(item)) {
                throw new IllegalArgumentException(
                        "Reporting period has no supported values: " + aligned.code()
                );
            }
            ReportingPeriodEntity period = savePeriod(aligned);
            saveGrowth(period, item.growth());
            saveProfit(period, item.profitability());
            saveLiquidity(period, item.liquidity());
            saveCapital(period, item.capital());
        }
    }

    private ReportingPeriodEntity savePeriod(AlignedPeriod aligned) {
        ReportingPeriodEntity period = periodRepo.findByCode(aligned.code())
                .orElseGet(ReportingPeriodEntity::new);
        period.setCode(aligned.code());
        period.setLabel(aligned.label());
        period.setPeriodType(aligned.type());
        period.setStartDate(aligned.start());
        period.setEndDate(aligned.end());
        return periodRepo.save(period);
    }

    /** Derives a stable period identity from exact supported fiscal boundaries. */
    private AlignedPeriod alignPeriod(AiExtractionResult.PeriodData item) {
        LocalDate start = LocalDate.parse(requireText(item.startDate(), "periods.startDate"));
        LocalDate end = LocalDate.parse(requireText(item.endDate(), "periods.endDate"));
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Reporting period startDate must not follow endDate");
        }

        int year = end.getYear();
        if (start.equals(LocalDate.of(year - 1, 7, 1))
                && end.equals(LocalDate.of(year, 6, 30))) {
            return new AlignedPeriod(
                    "FY" + year,
                    "Full Year " + year,
                    PeriodType.FULL_YEAR,
                    start,
                    end
            );
        }
        if (start.equals(LocalDate.of(year, 7, 1))
                && end.equals(LocalDate.of(year, 12, 31))) {
            int fiscalYear = year + 1;
            return new AlignedPeriod(
                    "HY" + fiscalYear,
                    "Half Year " + fiscalYear,
                    PeriodType.HALF_YEAR,
                    start,
                    end
            );
        }

        log.warn("Skipping unsupported reporting period {} to {}", start, end);
        return null;
    }

    private void saveGrowth(ReportingPeriodEntity period, AiExtractionResult.Growth data) {
        if (data == null) {
            return;
        }
        GrowthEntity row = growthRepo.findByReportingPeriod(period).orElseGet(GrowthEntity::new);
        row.setReportingPeriod(period);
        if (data.revenue() != null) {
            row.setRevenue(data.revenue());
        }
        growthRepo.save(row);
    }

    private void saveProfit(ReportingPeriodEntity period, AiExtractionResult.Profitability data) {
        if (data == null) {
            return;
        }
        ProfitabilityEntity row = profitRepo.findByReportingPeriod(period)
                .orElseGet(ProfitabilityEntity::new);
        row.setReportingPeriod(period);
        if (data.grossProfit() != null) {
            row.setGrossProfit(data.grossProfit());
        }
        if (data.grossMargin() != null) {
            row.setGrossMargin(data.grossMargin());
        }
        if (data.operatingLoss() != null) {
            row.setOperatingLoss(data.operatingLoss());
        }
        if (data.costOfSales() != null) {
            row.setCostOfSales(data.costOfSales());
        }
        if (data.administrativeExpenses() != null) {
            row.setAdministrativeExpenses(data.administrativeExpenses());
        }
        profitRepo.save(row);
    }

    private void saveLiquidity(ReportingPeriodEntity period, AiExtractionResult.Liquidity data) {
        if (data == null) {
            return;
        }
        LiquidityEntity row = liquidityRepo.findByReportingPeriod(period)
                .orElseGet(LiquidityEntity::new);
        row.setReportingPeriod(period);
        if (data.cashBalance() != null) {
            row.setCashBalance(data.cashBalance());
        }
        if (data.operatingCashFlow() != null) {
            row.setOperatingCashFlow(data.operatingCashFlow());
        }
        if (data.workingCapitalMovement() != null) {
            row.setWorkingCapitalMovement(data.workingCapitalMovement());
        }
        if (data.currentAssets() != null) {
            row.setCurrentAssets(data.currentAssets());
        }
        if (data.currentLiabilities() != null) {
            row.setCurrentLiabilities(data.currentLiabilities());
        }
        if (data.netCurrentPosition() != null) {
            row.setNetCurrentPosition(data.netCurrentPosition());
        }
        if (data.capitalExpenditure() != null) {
            row.setCapitalExpenditure(data.capitalExpenditure());
        }
        liquidityRepo.save(row);
    }

    private void saveCapital(ReportingPeriodEntity period, AiExtractionResult.Capital data) {
        if (data == null) {
            return;
        }
        CapitalEntity row = capitalRepo.findByReportingPeriod(period).orElseGet(CapitalEntity::new);
        row.setReportingPeriod(period);
        if (data.bankDebt() != null) {
            row.setBankDebt(data.bankDebt());
        }
        if (data.loanMovement() != null) {
            row.setLoanMovement(data.loanMovement());
        }
        if (data.interestExpense() != null) {
            row.setInterestExpense(data.interestExpense());
        }
        if (data.netAssetPosition() != null) {
            row.setNetAssetPosition(data.netAssetPosition());
        }
        capitalRepo.save(row);
    }

    private List<AiExtractionResult.PeriodData> requirePeriods(
            List<AiExtractionResult.PeriodData> periods
    ) {
        if (periods == null) {
            throw new IllegalArgumentException("periods is required");
        }
        return periods;
    }

    private boolean isEmpty(AiExtractionResult.PeriodData item) {
        AiExtractionResult.Growth growth = item.growth();
        AiExtractionResult.Profitability profit = item.profitability();
        AiExtractionResult.Liquidity liquidity = item.liquidity();
        AiExtractionResult.Capital capital = item.capital();
        return (growth == null || growth.revenue() == null)
                && (profit == null || (
                profit.grossProfit() == null
                        && profit.grossMargin() == null
                        && profit.operatingLoss() == null
                        && profit.costOfSales() == null
                        && profit.administrativeExpenses() == null
        ))
                && (liquidity == null || (
                liquidity.cashBalance() == null
                        && liquidity.operatingCashFlow() == null
                        && liquidity.workingCapitalMovement() == null
                        && liquidity.currentAssets() == null
                        && liquidity.currentLiabilities() == null
                        && liquidity.netCurrentPosition() == null
                        && liquidity.capitalExpenditure() == null
        ))
                && (capital == null || (
                capital.bankDebt() == null
                        && capital.loanMovement() == null
                        && capital.interestExpense() == null
                        && capital.netAssetPosition() == null
        ));
    }

    private IngestionRunEntity getRun(Long runId) {
        return runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ingestion run: " + runId));
    }

    private void requireState(IngestionRunEntity run, IngestionStatus status) {
        if (run.getStatus() != status) {
            throw new IllegalStateException("Ingestion run is not " + status);
        }
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String hash(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Converts a document path to one relative to the application working directory. */
    private String relativePath(Path file) {
        Path base = Path.of("").toAbsolutePath().normalize();
        return base.relativize(file).toString();
    }

    private String errorMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }

    /** Contains a backend-controlled reporting period identity. */
    private record AlignedPeriod(
            String code,
            String label,
            PeriodType type,
            LocalDate start,
            LocalDate end
    ) {
    }
}
