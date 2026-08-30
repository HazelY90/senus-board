package com.hazely.senusboard.jobs.ingestion.dtos;

import java.util.List;

/** Represents provider-neutral analytics for reporting-period comparisons. */
public record AiComparisonResult(List<ComparisonAnalytics> comparisons) {

    /** Contains AI analytics for one ordered reporting-period comparison. */
    public record ComparisonAnalytics(
            String basePeriodCode,
            String targetPeriodCode,
            String growthAnalytics,
            String profitabilityAnalytics,
            String liquidityAnalytics,
            String capitalAnalytics,
            String totalAnalytics
    ) {
    }
}
