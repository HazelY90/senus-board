package com.hazely.senusboard.jobs.ingestion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents provider-neutral structured output for one source document.
 */
public record AiExtractionResult(
        String publicationDate,
        List<ReportingPeriod> reportingPeriods,
        List<ExtractionItem> extractionItems
) {

    /** Represents a reporting period candidate found in the document. */
    public record ReportingPeriod(
            String code,
            String label,
            String periodType,
            String startDate,
            String endDate
    ) {
    }

    /** Represents one metric candidate awaiting backend validation. */
    public record ExtractionItem(
            String periodCode,
            String metricCode,
            String rawValue,
            BigDecimal numericValue,
            String unit,
            String dimensionType,
            String dimensionCode,
            int sourcePage,
            String sourceText,
            BigDecimal confidence
    ) {
    }
}
