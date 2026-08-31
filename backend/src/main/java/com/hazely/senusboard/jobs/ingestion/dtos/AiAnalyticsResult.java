package com.hazely.senusboard.jobs.ingestion.dtos;

import java.util.List;

/** Represents provider-neutral analytics for stored reporting periods. */
public record AiAnalyticsResult(List<PeriodAnalytics> periods) {

    /** Contains AI analytics for one reporting period. */
    public record PeriodAnalytics(
            String periodCode,
            String growthAnalytics,
            String profitabilityAnalytics,
            String liquidityAnalytics,
            String capitalAnalytics,
            String totalAnalytics
    ) {
    }
}
