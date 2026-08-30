package com.hazely.senusboard.jobs.ingestion.dtos;

import java.net.URI;
import java.nio.file.Path;

/** Describes one downloaded source document and its provenance. */
public record DownloadedDocument(Path file, URI sourceUrl, String documentType) {
}
