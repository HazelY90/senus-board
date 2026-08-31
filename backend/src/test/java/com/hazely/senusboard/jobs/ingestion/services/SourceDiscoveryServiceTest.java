package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.jobs.ingestion.IngestionProperties;
import com.hazely.senusboard.jobs.ingestion.dtos.DownloadedDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SourceDiscoveryServiceTest {

    @TempDir
    Path dir;

    @Test
    void reusesExistingFileByName() throws Exception {
        Path file = dir.resolve("report.pdf");
        Files.writeString(file, "existing content");
        IngestionProperties props = new IngestionProperties();
        props.setDocumentDir(dir);
        SourceDiscoveryService service = new SourceDiscoveryService(props, new ObjectMapper());
        URI url = URI.create("https://example.com/files/report.pdf");

        DownloadedDocument result = service.findExisting(url, dir.toAbsolutePath().normalize());

        assertThat(result).isNotNull();
        assertThat(result.file()).isEqualTo(file.toAbsolutePath().normalize());
        assertThat(result.sourceUrl()).isEqualTo(url);
        assertThat(Files.readString(file)).isEqualTo("existing content");
    }
}
