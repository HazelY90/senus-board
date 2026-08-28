package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.jobs.ingestion.dtos.AiAnalyticsResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AnalyticsDataset;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Defines the AI provider boundary used by the ingestion workflow.
 *
 * <p>An implementation will build structured extraction requests, call the provider, and return
 * provider-neutral results. It must not contain database persistence or review decisions.</p>
 */
public interface AiClient {

    /**
     * Extracts structured metric candidates from one local source document.
     */
    AiExtractionResult extract(Path file) throws IOException, InterruptedException;

    /**
     * Generates analytics from the complete stored reporting dataset.
     */
    AiAnalyticsResult analyze(AnalyticsDataset data) throws IOException, InterruptedException;
}
