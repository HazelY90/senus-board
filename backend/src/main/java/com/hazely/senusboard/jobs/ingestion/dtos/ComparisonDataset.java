package com.hazely.senusboard.jobs.ingestion.dtos;

import java.math.BigDecimal;
import java.util.List;

/** Contains ordered reporting-period comparisons supplied for AI analytics. */
public record ComparisonDataset(List<ComparisonData> comparisons) {

    /** Contains two periods and their deterministic metric changes. */
    public record ComparisonData(
            String basePeriodCode,
            String targetPeriodCode,
            AnalyticsDataset.PeriodData basePeriod,
            AnalyticsDataset.PeriodData targetPeriod,
            Changes changes
    ) {
    }

    /** Groups deterministic changes by dashboard category. */
    public record Changes(
            List<MetricChange> growth,
            List<MetricChange> profitability,
            List<MetricChange> liquidity,
            List<MetricChange> capital
    ) {
    }

    /** Contains one reported or calculated metric comparison. */
    public record MetricChange(
            String source,
            String metric,
            BigDecimal baseValue,
            BigDecimal targetValue,
            BigDecimal absoluteChange,
            BigDecimal percentageChange
    ) {
    }
}
