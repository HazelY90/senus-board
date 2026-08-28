package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.repositories.DimensionRepository;
import com.hazely.senusboard.repositories.MetricRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates extraction from configured source documents.
 *
 * <p>This service will create an ingestion run, load document content, call {@link AiClient}, and
 * persist candidate values, and promote confirmed items into formal metric values. External AI
 * calls remain outside long-running database transactions.</p>
 */
public class ExtractionService {

    private final AiClient aiClient;
    private final MetricRepository metricRepo;
    private final DimensionRepository dimensionRepo;
    private final ExtractionPersistenceService persistenceService;
    private final PromotionService promotionService;

    public ExtractionService(
            AiClient aiClient,
            MetricRepository metricRepo,
            DimensionRepository dimensionRepo,
            ExtractionPersistenceService persistenceService,
            PromotionService promotionService
    ) {
        this.aiClient = aiClient;
        this.metricRepo = metricRepo;
        this.dimensionRepo = dimensionRepo;
        this.persistenceService = persistenceService;
        this.promotionService = promotionService;
    }

    /**
     * Sends each source document to the AI provider with the current database catalogue.
     */
    public List<DocumentExtraction> extract(List<DownloadedDocument> docs)
            throws IOException, InterruptedException {
        ExtractionCatalogue catalogue = catalogue();
        List<DocumentExtraction> results = new ArrayList<>();
        for (DownloadedDocument doc : docs) {
            Long runId = persistenceService.start(doc);
            try {
                AiExtractionResult result = aiClient.extract(doc.file(), catalogue);
                persistenceService.complete(runId, result);
                promotionService.promote(runId);
                results.add(new DocumentExtraction(doc.file(), result));
            } catch (IOException | InterruptedException | RuntimeException ex) {
                persistenceService.fail(runId, ex);
                throw ex;
            }
        }
        return List.copyOf(results);
    }

    private ExtractionCatalogue catalogue() {
        List<ExtractionCatalogue.Metric> metrics = metricRepo.findAll().stream()
                .sorted(Comparator.comparing(metric -> metric.getCode()))
                .map(metric -> new ExtractionCatalogue.Metric(
                        metric.getCode(),
                        metric.getName(),
                        metric.getCategory().name(),
                        metric.getUnit().name(),
                        metric.getDescription()
                ))
                .toList();
        List<ExtractionCatalogue.Dimension> dimensions = dimensionRepo.findAll().stream()
                .sorted(Comparator.comparing(dimension -> dimension.getCode()))
                .map(dimension -> new ExtractionCatalogue.Dimension(
                        dimension.getDimensionType().name(),
                        dimension.getCode(),
                        dimension.getLabel()
                ))
                .toList();
        return new ExtractionCatalogue(metrics, dimensions);
    }
}
