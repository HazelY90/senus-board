package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.jobs.ingestion.AiClient;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AnalyticsDataset;
import com.hazely.senusboard.jobs.ingestion.dtos.ComparisonDataset;
import com.hazely.senusboard.jobs.ingestion.dtos.ComparisonPair;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Builds supported period comparisons and coordinates their AI analytics generation. */
public class ComparisonAnalyticsService {

    private static final int SCALE = 4;
    private static final int CALC_SCALE = 12;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String REPORTED = "reported";
    private static final String CALCULATED = "calculated";
    private static final List<ComparisonPair> PAIRS = List.of(
            new ComparisonPair("FY2024", "FY2025"),
            new ComparisonPair("HY2025", "HY2026")
    );

    private final AiClient aiClient;
    private final AnalyticsService analyticsService;
    private final ComparisonPersistenceService persistenceService;
    private final ObjectMapper mapper;

    public ComparisonAnalyticsService(
            AiClient aiClient,
            AnalyticsService analyticsService,
            ComparisonPersistenceService persistenceService,
            ObjectMapper mapper
    ) {
        this.aiClient = aiClient;
        this.analyticsService = analyticsService;
        this.persistenceService = persistenceService;
        this.mapper = mapper;
    }

    /** Requests and stores analytics for changed supported comparison inputs. */
    public void analyze() throws IOException, InterruptedException {
        AnalyticsDataset data = analyticsService.load();
        Map<String, AnalyticsDataset.PeriodData> periods = new LinkedHashMap<>();
        for (AnalyticsDataset.PeriodData period : data.periods()) {
            periods.put(period.code(), period);
        }

        List<ComparisonDataset.ComparisonData> items = new ArrayList<>();
        Map<ComparisonPair, String> hashes = new LinkedHashMap<>();
        for (ComparisonPair pair : PAIRS) {
            AnalyticsDataset.PeriodData base = periods.get(pair.basePeriodCode());
            AnalyticsDataset.PeriodData target = periods.get(pair.targetPeriodCode());
            if (base == null || target == null) {
                continue;
            }
            validate(base, target);
            ComparisonDataset.ComparisonData item = comparison(pair, base, target);
            String hash = hash(item);
            if (!persistenceService.isCurrent(period(base), period(target), hash)) {
                items.add(item);
                hashes.put(pair, hash);
            }
        }

        if (items.isEmpty()) {
            return;
        }
        persistenceService.save(
                aiClient.compare(new ComparisonDataset(List.copyOf(items))),
                Map.copyOf(hashes)
        );
    }

    private ReportingPeriodEntity period(AnalyticsDataset.PeriodData data) {
        return analyticsService.period(data.code());
    }

    private ComparisonDataset.ComparisonData comparison(
            ComparisonPair pair,
            AnalyticsDataset.PeriodData base,
            AnalyticsDataset.PeriodData target
    ) {
        return new ComparisonDataset.ComparisonData(
                pair.basePeriodCode(),
                pair.targetPeriodCode(),
                base,
                target,
                changes(base, target)
        );
    }

    private ComparisonDataset.Changes changes(
            AnalyticsDataset.PeriodData base,
            AnalyticsDataset.PeriodData target
    ) {
        return new ComparisonDataset.Changes(
                growth(base.growth(), target.growth()),
                profitability(base.profitability(), target.profitability()),
                liquidity(base.liquidity(), target.liquidity()),
                capital(base.capital(), target.capital())
        );
    }

    private List<ComparisonDataset.MetricChange> growth(
            AnalyticsDataset.Growth base,
            AnalyticsDataset.Growth target
    ) {
        return List.of(change(
                REPORTED,
                "revenue",
                value(base == null ? null : base.reported(), AiExtractionResult.Growth::revenue),
                value(target == null ? null : target.reported(), AiExtractionResult.Growth::revenue)
        ), change(
                CALCULATED,
                "revenueGrowth",
                value(base == null ? null : base.calculated(), AnalyticsDataset.CalculatedGrowth::revenueGrowth),
                value(target == null ? null : target.calculated(), AnalyticsDataset.CalculatedGrowth::revenueGrowth)
        ));
    }

    private List<ComparisonDataset.MetricChange> profitability(
            AnalyticsDataset.Profitability base,
            AnalyticsDataset.Profitability target
    ) {
        AiExtractionResult.Profitability baseReported = base == null ? null : base.reported();
        AiExtractionResult.Profitability targetReported = target == null ? null : target.reported();
        AnalyticsDataset.CalculatedProfitability baseCalc = base == null ? null : base.calculated();
        AnalyticsDataset.CalculatedProfitability targetCalc = target == null ? null : target.calculated();
        return List.of(
                change(REPORTED, "grossProfit", value(baseReported, AiExtractionResult.Profitability::grossProfit), value(targetReported, AiExtractionResult.Profitability::grossProfit)),
                change(REPORTED, "grossMargin", value(baseReported, AiExtractionResult.Profitability::grossMargin), value(targetReported, AiExtractionResult.Profitability::grossMargin)),
                change(REPORTED, "operatingLoss", value(baseReported, AiExtractionResult.Profitability::operatingLoss), value(targetReported, AiExtractionResult.Profitability::operatingLoss)),
                change(REPORTED, "costOfSales", value(baseReported, AiExtractionResult.Profitability::costOfSales), value(targetReported, AiExtractionResult.Profitability::costOfSales)),
                change(REPORTED, "administrativeExpenses", value(baseReported, AiExtractionResult.Profitability::administrativeExpenses), value(targetReported, AiExtractionResult.Profitability::administrativeExpenses)),
                change(CALCULATED, "grossMargin", value(baseCalc, AnalyticsDataset.CalculatedProfitability::grossMargin), value(targetCalc, AnalyticsDataset.CalculatedProfitability::grossMargin)),
                change(CALCULATED, "operatingMargin", value(baseCalc, AnalyticsDataset.CalculatedProfitability::operatingMargin), value(targetCalc, AnalyticsDataset.CalculatedProfitability::operatingMargin)),
                change(CALCULATED, "costOfSalesRatio", value(baseCalc, AnalyticsDataset.CalculatedProfitability::costOfSalesRatio), value(targetCalc, AnalyticsDataset.CalculatedProfitability::costOfSalesRatio)),
                change(CALCULATED, "administrativeExpenseRatio", value(baseCalc, AnalyticsDataset.CalculatedProfitability::administrativeExpenseRatio), value(targetCalc, AnalyticsDataset.CalculatedProfitability::administrativeExpenseRatio))
        );
    }

    private List<ComparisonDataset.MetricChange> liquidity(
            AnalyticsDataset.Liquidity base,
            AnalyticsDataset.Liquidity target
    ) {
        AiExtractionResult.Liquidity baseReported = base == null ? null : base.reported();
        AiExtractionResult.Liquidity targetReported = target == null ? null : target.reported();
        AnalyticsDataset.CalculatedLiquidity baseCalc = base == null ? null : base.calculated();
        AnalyticsDataset.CalculatedLiquidity targetCalc = target == null ? null : target.calculated();
        return List.of(
                change(REPORTED, "cashBalance", value(baseReported, AiExtractionResult.Liquidity::cashBalance), value(targetReported, AiExtractionResult.Liquidity::cashBalance)),
                change(REPORTED, "operatingCashFlow", value(baseReported, AiExtractionResult.Liquidity::operatingCashFlow), value(targetReported, AiExtractionResult.Liquidity::operatingCashFlow)),
                change(REPORTED, "workingCapitalMovement", value(baseReported, AiExtractionResult.Liquidity::workingCapitalMovement), value(targetReported, AiExtractionResult.Liquidity::workingCapitalMovement)),
                change(REPORTED, "currentAssets", value(baseReported, AiExtractionResult.Liquidity::currentAssets), value(targetReported, AiExtractionResult.Liquidity::currentAssets)),
                change(REPORTED, "currentLiabilities", value(baseReported, AiExtractionResult.Liquidity::currentLiabilities), value(targetReported, AiExtractionResult.Liquidity::currentLiabilities)),
                change(REPORTED, "netCurrentPosition", value(baseReported, AiExtractionResult.Liquidity::netCurrentPosition), value(targetReported, AiExtractionResult.Liquidity::netCurrentPosition)),
                change(REPORTED, "capitalExpenditure", value(baseReported, AiExtractionResult.Liquidity::capitalExpenditure), value(targetReported, AiExtractionResult.Liquidity::capitalExpenditure)),
                change(CALCULATED, "operatingCashFlowMargin", value(baseCalc, AnalyticsDataset.CalculatedLiquidity::operatingCashFlowMargin), value(targetCalc, AnalyticsDataset.CalculatedLiquidity::operatingCashFlowMargin)),
                change(CALCULATED, "freeCashFlow", value(baseCalc, AnalyticsDataset.CalculatedLiquidity::freeCashFlow), value(targetCalc, AnalyticsDataset.CalculatedLiquidity::freeCashFlow)),
                change(CALCULATED, "freeCashFlowMargin", value(baseCalc, AnalyticsDataset.CalculatedLiquidity::freeCashFlowMargin), value(targetCalc, AnalyticsDataset.CalculatedLiquidity::freeCashFlowMargin)),
                change(CALCULATED, "currentRatio", value(baseCalc, AnalyticsDataset.CalculatedLiquidity::currentRatio), value(targetCalc, AnalyticsDataset.CalculatedLiquidity::currentRatio)),
                change(CALCULATED, "cashRatio", value(baseCalc, AnalyticsDataset.CalculatedLiquidity::cashRatio), value(targetCalc, AnalyticsDataset.CalculatedLiquidity::cashRatio))
        );
    }

    private List<ComparisonDataset.MetricChange> capital(
            AnalyticsDataset.Capital base,
            AnalyticsDataset.Capital target
    ) {
        AiExtractionResult.Capital baseReported = base == null ? null : base.reported();
        AiExtractionResult.Capital targetReported = target == null ? null : target.reported();
        AnalyticsDataset.CalculatedCapital baseCalc = base == null ? null : base.calculated();
        AnalyticsDataset.CalculatedCapital targetCalc = target == null ? null : target.calculated();
        return List.of(
                change(REPORTED, "bankDebt", value(baseReported, AiExtractionResult.Capital::bankDebt), value(targetReported, AiExtractionResult.Capital::bankDebt)),
                change(REPORTED, "loanMovement", value(baseReported, AiExtractionResult.Capital::loanMovement), value(targetReported, AiExtractionResult.Capital::loanMovement)),
                change(REPORTED, "interestExpense", value(baseReported, AiExtractionResult.Capital::interestExpense), value(targetReported, AiExtractionResult.Capital::interestExpense)),
                change(REPORTED, "netAssetPosition", value(baseReported, AiExtractionResult.Capital::netAssetPosition), value(targetReported, AiExtractionResult.Capital::netAssetPosition)),
                change(CALCULATED, "netCash", value(baseCalc, AnalyticsDataset.CalculatedCapital::netCash), value(targetCalc, AnalyticsDataset.CalculatedCapital::netCash))
        );
    }

    private ComparisonDataset.MetricChange change(
            String source,
            String metric,
            BigDecimal base,
            BigDecimal target
    ) {
        BigDecimal absolute = base == null || target == null
                ? null
                : target.subtract(base).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal percentage = absolute == null || base.compareTo(BigDecimal.ZERO) == 0
                ? null
                : absolute.divide(base.abs(), CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);
        return new ComparisonDataset.MetricChange(
                source,
                metric,
                base,
                target,
                absolute,
                percentage
        );
    }

    private void validate(
            AnalyticsDataset.PeriodData base,
            AnalyticsDataset.PeriodData target
    ) {
        if (!base.periodType().equals(target.periodType())) {
            throw new IllegalArgumentException("Comparison period types must match");
        }
        if (!java.time.LocalDate.parse(base.endDate())
                .isBefore(java.time.LocalDate.parse(target.endDate()))) {
            throw new IllegalArgumentException("Comparison periods are not ordered");
        }
    }

    private String hash(ComparisonDataset.ComparisonData item) throws IOException {
        try {
            byte[] data = mapper.writeValueAsBytes(item);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private <T> BigDecimal value(T row, Function<T, BigDecimal> reader) {
        return row == null ? null : reader.apply(row);
    }
}
