package com.hazely.senusboard.jobs.ingestion;

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
    AiExtractionResult extract(Path file, ExtractionCatalogue catalogue)
            throws IOException, InterruptedException;
}
