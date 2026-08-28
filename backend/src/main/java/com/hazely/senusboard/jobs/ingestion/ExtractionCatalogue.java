package com.hazely.senusboard.jobs.ingestion;

import java.util.List;

/**
 * Contains the metric and dimension identities that the AI provider may return.
 */
public record ExtractionCatalogue(List<Metric> metrics, List<Dimension> dimensions) {

    /** Describes one allowed metric. */
    public record Metric(String code, String name, String category, String unit, String description) {
    }

    /** Describes one allowed dimension member. */
    public record Dimension(String type, String code, String label) {
    }
}
