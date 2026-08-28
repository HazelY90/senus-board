package com.hazely.senusboard.jobs.ingestion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import tools.jackson.databind.ObjectMapper;
import com.hazely.senusboard.repositories.DimensionRepository;
import com.hazely.senusboard.repositories.ExtractionItemRepository;
import com.hazely.senusboard.repositories.IngestionRunRepository;
import com.hazely.senusboard.repositories.MetricRepository;
import com.hazely.senusboard.repositories.MetricValueRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import com.hazely.senusboard.repositories.SourceDocumentRepository;

import java.io.IOException;

/**
 * Defines the configuration boundary for the one-time ingestion job.
 *
 * <p>This class activates ingestion configuration without containing document processing or
 * persistence logic.</p>
 */
@Configuration(proxyBeanMethods = false)
@Profile("ingestion")
@ConditionalOnProperty(prefix = "app.job.ingestion", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({IngestionProperties.class, OpenAiProperties.class})
public class IngestionJob {

    /**
     * Creates the local source discovery and download service.
     */
    @Bean
    SourceDiscoveryService sourceDiscoveryService(IngestionProperties props, ObjectMapper mapper) {
        return new SourceDiscoveryService(props, mapper);
    }

    /**
     * Creates the OpenAI provider adapter with classpath prompt and schema resources.
     */
    @Bean
    AiClient aiClient(OpenAiProperties props, ObjectMapper mapper) throws IOException {
        return new OpenAiClient(props, mapper);
    }

    /**
     * Creates the extraction orchestrator and catalogue provider.
     */
    @Bean
    ExtractionService extractionService(
            AiClient aiClient,
            MetricRepository metricRepo,
            DimensionRepository dimensionRepo,
            ExtractionPersistenceService persistenceService,
            PromotionService promotionService
    ) {
        return new ExtractionService(
                aiClient,
                metricRepo,
                dimensionRepo,
                persistenceService,
                promotionService
        );
    }

    /**
     * Creates the transactional persistence boundary for ingestion state and extracted items.
     */
    @Bean
    ExtractionPersistenceService extractionPersistenceService(
            SourceDocumentRepository sourceRepo,
            IngestionRunRepository runRepo,
            ExtractionItemRepository itemRepo,
            MetricRepository metricRepo,
            DimensionRepository dimensionRepo,
            ReportingPeriodRepository periodRepo,
            OpenAiProperties openAiProps
    ) {
        return new ExtractionPersistenceService(
                sourceRepo,
                runRepo,
                itemRepo,
                metricRepo,
                dimensionRepo,
                periodRepo,
                openAiProps
        );
    }

    /** Creates the automatic confirmation and formal-value promotion service. */
    @Bean
    PromotionService promotionService(
            ExtractionItemRepository itemRepo,
            ReportingPeriodRepository periodRepo,
            MetricRepository metricRepo,
            MetricValueRepository valueRepo
    ) {
        return new PromotionService(itemRepo, periodRepo, metricRepo, valueRepo);
    }

    /**
     * Starts one ingestion execution after the application context is ready.
     */
    @Bean
    IngestionRunner ingestionRunner(
            SourceDiscoveryService sourceService,
            ExtractionService extractionService
    ) {
        return new IngestionRunner(sourceService, extractionService);
    }
}
