package com.hazely.senusboard.jobs.ingestion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.nio.file.Path;

/**
 * Holds external configuration for the ingestion job.
 *
 * <p>The properties are loaded from {@code app.job.ingestion}. Sensitive values belong in
 * provider-specific configuration and must never be logged.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.job.ingestion")
public class IngestionProperties {

    private boolean isEnabled;

    @NotNull
    private URI sourceUrl;

    @NotNull
    private Path documentDir;

    @Positive
    private long maxFileBytes = 50L * 1024 * 1024;

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public URI getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(URI sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Path getDocumentDir() {
        return documentDir;
    }

    public void setDocumentDir(Path documentDir) {
        this.documentDir = documentDir;
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
    }
}
