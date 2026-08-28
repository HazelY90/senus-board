package com.hazely.senusboard.jobs.ingestion;

import java.nio.file.Path;

/** Associates one local source document with its structured AI extraction result. */
public record DocumentExtraction(Path file, AiExtractionResult result) {
}
