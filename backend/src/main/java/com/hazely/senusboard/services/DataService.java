package com.hazely.senusboard.services;

import com.hazely.senusboard.dtos.ComparisonDto;
import com.hazely.senusboard.dtos.DataDto;
import com.hazely.senusboard.dtos.DocumentDownloadDto;
import com.hazely.senusboard.dtos.DocumentsDto;
import com.hazely.senusboard.dtos.ReportingPeriodsDto;
import com.hazely.senusboard.entities.AnalyticsEntity;
import com.hazely.senusboard.entities.CalculatedCapitalEntity;
import com.hazely.senusboard.entities.CalculatedGrowthEntity;
import com.hazely.senusboard.entities.CalculatedLiquidityEntity;
import com.hazely.senusboard.entities.CalculatedProfitabilityEntity;
import com.hazely.senusboard.entities.CapitalEntity;
import com.hazely.senusboard.entities.ComparisonAnalyticsEntity;
import com.hazely.senusboard.entities.GrowthEntity;
import com.hazely.senusboard.entities.LiquidityEntity;
import com.hazely.senusboard.entities.ProfitabilityEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.entities.SourceDocumentEntity;
import com.hazely.senusboard.repositories.AnalyticsRepository;
import com.hazely.senusboard.repositories.CalculatedCapitalRepository;
import com.hazely.senusboard.repositories.CalculatedGrowthRepository;
import com.hazely.senusboard.repositories.CalculatedLiquidityRepository;
import com.hazely.senusboard.repositories.CalculatedProfitabilityRepository;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.ComparisonAnalyticsRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import com.hazely.senusboard.repositories.SourceDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Loads and combines all data stored for one reporting period. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DataService {

    private static final Set<PeriodPair> PAIRS = Set.of(
            new PeriodPair("FY2024", "FY2025"),
            new PeriodPair("HY2025", "HY2026")
    );

    private final ReportingPeriodRepository periodRepo;
    private final GrowthRepository growthRepo;
    private final CalculatedGrowthRepository growthCalcRepo;
    private final ProfitabilityRepository profitabilityRepo;
    private final CalculatedProfitabilityRepository profitabilityCalcRepo;
    private final LiquidityRepository liquidityRepo;
    private final CalculatedLiquidityRepository liquidityCalcRepo;
    private final CapitalRepository capitalRepo;
    private final CalculatedCapitalRepository capitalCalcRepo;
    private final AnalyticsRepository analyticsRepo;
    private final ComparisonAnalyticsRepository comparisonRepo;
    private final SourceDocumentRepository sourceRepo;

    @Value("${app.baseUrl}")
    private String baseUrl;

    /** Returns the complete dataset for a canonical period code. */
    public DataDto getData(String code) {
        String value = cleanCode(code);
        ReportingPeriodEntity period = periodRepo.findByCode(value)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reporting period not found: " + value
                ));

        GrowthEntity growth = growthRepo.findByReportingPeriod(period).orElse(null);
        CalculatedGrowthEntity growthCalc = growthCalcRepo.findByReportingPeriod(period).orElse(null);
        ProfitabilityEntity profitability = profitabilityRepo.findByReportingPeriod(period).orElse(null);
        CalculatedProfitabilityEntity profitabilityCalc = profitabilityCalcRepo
                .findByReportingPeriod(period)
                .orElse(null);
        LiquidityEntity liquidity = liquidityRepo.findByReportingPeriod(period).orElse(null);
        CalculatedLiquidityEntity liquidityCalc = liquidityCalcRepo.findByReportingPeriod(period).orElse(null);
        CapitalEntity capital = capitalRepo.findByReportingPeriod(period).orElse(null);
        CalculatedCapitalEntity capitalCalc = capitalCalcRepo.findByReportingPeriod(period).orElse(null);
        AnalyticsEntity analytics = analyticsRepo.findByReportingPeriod(period).orElse(null);

        return new DataDto(
                mapPeriod(period),
                mapGrowth(growth, growthCalc),
                mapProfitability(profitability, profitabilityCalc),
                mapLiquidity(liquidity, liquidityCalc),
                mapCapital(capital, capitalCalc),
                mapAnalytics(analytics)
        );
    }

    /** Returns stored analytics for one supported ordered period comparison. */
    public ComparisonDto getComparison(String baseCode, String targetCode) {
        String baseValue = cleanCode(baseCode);
        String targetValue = cleanCode(targetCode);
        ReportingPeriodEntity base = getPeriod(baseValue);
        ReportingPeriodEntity target = getPeriod(targetValue);
        validateComparison(base, target);

        ComparisonAnalyticsEntity analytics = comparisonRepo
                .findByBasePeriodAndTargetPeriod(base, target)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comparison analytics not found: " + baseValue + " to " + targetValue
                ));
        return new ComparisonDto(
                mapComparisonPeriod(base),
                mapComparisonPeriod(target),
                new ComparisonDto.AnalyticsDto(
                        analytics.getGrowthAnalytics(),
                        analytics.getProfitabilityAnalytics(),
                        analytics.getLiquidityAnalytics(),
                        analytics.getCapitalAnalytics(),
                        analytics.getTotalAnalytics()
                )
        );
    }

    /** Returns available periods with the latest period selected by default. */
    public ReportingPeriodsDto getPeriods() {
        List<ReportingPeriodEntity> periods = periodRepo.findAll().stream()
                .sorted(Comparator
                        .comparing(ReportingPeriodEntity::getEndDate)
                        .reversed()
                        .thenComparing(ReportingPeriodEntity::getStartDate, Comparator.reverseOrder())
                        .thenComparing(ReportingPeriodEntity::getCode))
                .toList();

        List<ReportingPeriodsDto.PeriodDto> data = periods.stream()
                .map(period -> mapOption(period, period == periods.getFirst()))
                .toList();
        return new ReportingPeriodsDto(data);
    }

    /** Returns source documents ordered by the latest publication and creation dates. */
    public DocumentsDto getDocuments() {
        List<DocumentsDto.DocumentDto> documents = sourceRepo.findAll().stream()
                .sorted(Comparator
                        .comparing(
                                SourceDocumentEntity::getPublicationDate,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                SourceDocumentEntity::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .map(this::mapDocument)
                .toList();
        return new DocumentsDto(documents);
    }

    /** Returns a validated local source document for download. */
    public DocumentDownloadDto getDownload(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document ID must be positive");
        }
        SourceDocumentEntity document = sourceRepo.findById(id)
                .orElseThrow(() -> documentNotFound(id));
        Path file = resolveFile(document.getLocalPath());
        if (file == null) {
            throw documentNotFound(id);
        }

        try {
            String detected = Files.probeContentType(file);
            MediaType type = parseType(detected);
            return new DocumentDownloadDto(
                    new FileSystemResource(file),
                    safeName(document.getName()),
                    type,
                    Files.size(file)
            );
        } catch (IOException ex) {
            throw documentNotFound(id);
        }
    }

    private String cleanCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period code is required");
        }
        return code.trim();
    }

    private ReportingPeriodEntity getPeriod(String code) {
        return periodRepo.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reporting period not found: " + code
                ));
    }

    private void validateComparison(
            ReportingPeriodEntity base,
            ReportingPeriodEntity target
    ) {
        if (base.getCode().equals(target.getCode())) {
            throw comparisonError("Comparison periods must be different");
        }
        if (base.getPeriodType() != target.getPeriodType()) {
            throw comparisonError("Comparison period types must match");
        }
        if (!base.getEndDate().isBefore(target.getEndDate())) {
            throw comparisonError("Comparison periods are not ordered");
        }
        if (!PAIRS.contains(new PeriodPair(base.getCode(), target.getCode()))) {
            throw comparisonError("Comparison period pair is not supported");
        }
    }

    private ResponseStatusException comparisonError(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private DataDto.PeriodDto mapPeriod(ReportingPeriodEntity period) {
        return new DataDto.PeriodDto(
                period.getCode(),
                period.getLabel(),
                period.getPeriodType(),
                period.getStartDate(),
                period.getEndDate()
        );
    }

    private ComparisonDto.PeriodDto mapComparisonPeriod(ReportingPeriodEntity period) {
        return new ComparisonDto.PeriodDto(
                period.getCode(),
                period.getLabel(),
                period.getPeriodType(),
                period.getStartDate(),
                period.getEndDate()
        );
    }

    private ReportingPeriodsDto.PeriodDto mapOption(
            ReportingPeriodEntity period,
            boolean isDefault
    ) {
        return new ReportingPeriodsDto.PeriodDto(
                period.getCode(),
                period.getLabel(),
                period.getPeriodType(),
                period.getStartDate(),
                period.getEndDate(),
                isDefault
        );
    }

    private DocumentsDto.DocumentDto mapDocument(SourceDocumentEntity document) {
        String url = resolveFile(document.getLocalPath()) == null
                ? null
                : "/api/v1/data/documents/" + document.getId() + "/download";
        return new DocumentsDto.DocumentDto(
                document.getName(),
                document.getDocumentType(),
                document.getPublicationDate(),
                document.getAiSummary(),
                url
        );
    }

    private Path resolveFile(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path relative = Path.of(value);
            if (relative.isAbsolute()) {
                return null;
            }
            Path base = Path.of(baseUrl).toAbsolutePath().normalize();
            Path file = base.resolve(relative).normalize();
            if (!file.startsWith(base)
                    || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || !Files.isReadable(file)) {
                return null;
            }
            Path realBase = base.toRealPath();
            Path realFile = file.toRealPath();
            return realFile.startsWith(realBase) ? realFile : null;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private MediaType parseType(String value) {
        if (value == null || value.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(value);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) {
            return "document";
        }
        String name = value.replaceAll("[\\p{Cntrl}/\\\\]", "_").trim();
        return name.isBlank() ? "document" : name;
    }

    private ResponseStatusException documentNotFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + id);
    }

    private DataDto.GrowthDto mapGrowth(GrowthEntity data, CalculatedGrowthEntity calc) {
        return new DataDto.GrowthDto(
                data == null ? null : data.getRevenue(),
                new DataDto.GrowthCalcDto(calc == null ? null : calc.getRevenueGrowth())
        );
    }

    private DataDto.ProfitabilityDto mapProfitability(
            ProfitabilityEntity data,
            CalculatedProfitabilityEntity calc
    ) {
        return new DataDto.ProfitabilityDto(
                data == null ? null : data.getGrossProfit(),
                data == null ? null : data.getGrossMargin(),
                data == null ? null : data.getOperatingLoss(),
                data == null ? null : data.getCostOfSales(),
                data == null ? null : data.getAdministrativeExpenses(),
                new DataDto.ProfitabilityCalcDto(
                        calc == null ? null : calc.getCalculatedGrossMargin(),
                        calc == null ? null : calc.getOperatingMargin(),
                        calc == null ? null : calc.getCostOfSalesRatio(),
                        calc == null ? null : calc.getAdministrativeExpenseRatio()
                )
        );
    }

    private DataDto.LiquidityDto mapLiquidity(LiquidityEntity data, CalculatedLiquidityEntity calc) {
        return new DataDto.LiquidityDto(
                data == null ? null : data.getCashBalance(),
                data == null ? null : data.getOperatingCashFlow(),
                data == null ? null : data.getWorkingCapitalMovement(),
                data == null ? null : data.getCurrentAssets(),
                data == null ? null : data.getCurrentLiabilities(),
                data == null ? null : data.getNetCurrentPosition(),
                data == null ? null : data.getCapitalExpenditure(),
                new DataDto.LiquidityCalcDto(
                        calc == null ? null : calc.getOperatingCashFlowMargin(),
                        calc == null ? null : calc.getFreeCashFlow(),
                        calc == null ? null : calc.getFreeCashFlowMargin(),
                        calc == null ? null : calc.getCurrentRatio(),
                        calc == null ? null : calc.getCashRatio()
                )
        );
    }

    private DataDto.CapitalDto mapCapital(CapitalEntity data, CalculatedCapitalEntity calc) {
        return new DataDto.CapitalDto(
                data == null ? null : data.getBankDebt(),
                data == null ? null : data.getLoanMovement(),
                data == null ? null : data.getInterestExpense(),
                data == null ? null : data.getNetAssetPosition(),
                new DataDto.CapitalCalcDto(calc == null ? null : calc.getNetCash())
        );
    }

    private DataDto.AnalyticsDto mapAnalytics(AnalyticsEntity data) {
        return new DataDto.AnalyticsDto(
                data == null ? null : data.getGrowthAnalytics(),
                data == null ? null : data.getProfitabilityAnalytics(),
                data == null ? null : data.getLiquidityAnalytics(),
                data == null ? null : data.getCapitalAnalytics(),
                data == null ? null : data.getTotalAnalytics()
        );
    }

    private record PeriodPair(String base, String target) {
    }
}
