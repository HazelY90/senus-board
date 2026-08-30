package com.hazely.senusboard.jobs.ingestion.dtos;

/** Identifies one ordered reporting-period comparison. */
public record ComparisonPair(String basePeriodCode, String targetPeriodCode) {
}
