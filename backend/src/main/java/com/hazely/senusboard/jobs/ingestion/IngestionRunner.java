package com.hazely.senusboard.jobs.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

/**
 * Coordinates one complete ingestion execution.
 *
 * <p>The runner will read command arguments, invoke the extraction workflow, map failures to an
 * exit status, and allow the non-web Spring process to terminate when the job finishes.</p>
 */
public class IngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionRunner.class);

    private final SourceDiscoveryService sourceService;
    private final ExtractionService extractionService;

    public IngestionRunner(SourceDiscoveryService sourceService, ExtractionService extractionService) {
        this.sourceService = sourceService;
        this.extractionService = extractionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<DownloadedDocument> docs = sourceService.fetch();
        log.info("Downloaded {} source document(s)", docs.size());
        List<DocumentExtraction> results = extractionService.extract(docs);
        int itemCount = results.stream()
                .mapToInt(result -> result.result().extractionItems().size())
                .sum();
        log.info("Extracted {} candidate item(s) from {} document(s)", itemCount, results.size());
    }
}
