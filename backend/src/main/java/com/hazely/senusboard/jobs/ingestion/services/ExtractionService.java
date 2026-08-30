package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.jobs.ingestion.AiClient;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.DocumentExtraction;
import com.hazely.senusboard.jobs.ingestion.dtos.DownloadedDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Orchestrates structured extraction from configured source documents. */
public class ExtractionService {

    private final AiClient aiClient;
    private final ExtractionPersistenceService persistenceService;
    private final AnalyticsService analyticsService;
    private final ComparisonAnalyticsService comparisonService;

    public ExtractionService(
            AiClient aiClient,
            ExtractionPersistenceService persistenceService,
            AnalyticsService analyticsService,
            ComparisonAnalyticsService comparisonService
    ) {
        this.aiClient = aiClient;
        this.persistenceService = persistenceService;
        this.analyticsService = analyticsService;
        this.comparisonService = comparisonService;
    }

    /** Extracts and stores stable category rows from each source document. */
    public List<DocumentExtraction> extract(List<DownloadedDocument> docs)
            throws IOException, InterruptedException {
        List<DocumentExtraction> results = new ArrayList<>();
        for (DownloadedDocument doc : docs) {
            Long runId = persistenceService.start(doc);
            try {
                AiExtractionResult result = aiClient.extract(doc.file());
                persistenceService.complete(runId, result);
                results.add(new DocumentExtraction(doc.file(), result));
            } catch (IOException | InterruptedException | RuntimeException ex) {
                persistenceService.fail(runId, ex);
                throw ex;
            }
        }
        if (!results.isEmpty()) {
            analyticsService.analyze();
            comparisonService.analyze();
        }
        return List.copyOf(results);
    }
}
