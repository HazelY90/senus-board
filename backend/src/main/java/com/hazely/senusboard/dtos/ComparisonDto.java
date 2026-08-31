package com.hazely.senusboard.dtos;

import com.hazely.senusboard.entities.enums.PeriodType;

import java.time.LocalDate;

/** Returns stored AI analytics for one ordered reporting-period comparison. */
public record ComparisonDto(
        PeriodDto basePeriod,
        PeriodDto targetPeriod,
        AnalyticsDto analytics
) {

    /** Identifies one reporting period represented by the comparison. */
    public record PeriodDto(
            String code,
            String label,
            PeriodType type,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    /** Contains AI narrative for the four categories and their combined result. */
    public record AnalyticsDto(
            String growthAnalytics,
            String profitabilityAnalytics,
            String liquidityAnalytics,
            String capitalAnalytics,
            String totalAnalytics
    ) {
    }
}
