package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.jobs.ingestion.services.AnalyticsPersistenceService;
import com.hazely.senusboard.jobs.ingestion.services.AnalyticsService;
import com.hazely.senusboard.jobs.ingestion.services.CalculationService;
import com.hazely.senusboard.jobs.ingestion.services.ExtractionPersistenceService;
import com.hazely.senusboard.jobs.ingestion.services.ExtractionService;
import com.hazely.senusboard.jobs.ingestion.services.SourceDiscoveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import tools.jackson.databind.ObjectMapper;
import com.hazely.senusboard.repositories.AnalyticsRepository;
import com.hazely.senusboard.repositories.CalculatedCapitalRepository;
import com.hazely.senusboard.repositories.CalculatedGrowthRepository;
import com.hazely.senusboard.repositories.CalculatedLiquidityRepository;
import com.hazely.senusboard.repositories.CalculatedProfitabilityRepository;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.IngestionRunRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
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
     * Creates the extraction orchestrator.
     */
    @Bean
    ExtractionService extractionService(
            AiClient aiClient,
            ExtractionPersistenceService persistenceService,
            AnalyticsService analyticsService
    ) {
        return new ExtractionService(aiClient, persistenceService, analyticsService);
    }

    /**
     * Creates deterministic calculation support for every reporting period.
     */
    @Bean
    CalculationService calculationService(
            ReportingPeriodRepository periodRepo,
            GrowthRepository growthRepo,
            ProfitabilityRepository profitRepo,
            LiquidityRepository liquidityRepo,
            CapitalRepository capitalRepo,
            CalculatedGrowthRepository calcGrowthRepo,
            CalculatedProfitabilityRepository calcProfitRepo,
            CalculatedLiquidityRepository calcLiquidityRepo,
            CalculatedCapitalRepository calcCapitalRepo
    ) {
        return new CalculationService(
                periodRepo,
                growthRepo,
                profitRepo,
                liquidityRepo,
                capitalRepo,
                calcGrowthRepo,
                calcProfitRepo,
                calcLiquidityRepo,
                calcCapitalRepo
        );
    }

    /**
     * Creates the transactional persistence boundary for ingestion state and category values.
     */
    @Bean
    ExtractionPersistenceService extractionPersistenceService(
            SourceDocumentRepository sourceRepo,
            IngestionRunRepository runRepo,
            ReportingPeriodRepository periodRepo,
            GrowthRepository growthRepo,
            ProfitabilityRepository profitRepo,
            LiquidityRepository liquidityRepo,
            CapitalRepository capitalRepo,
            OpenAiProperties openAiProps,
            CalculationService calculationService
    ) {
        return new ExtractionPersistenceService(
                sourceRepo,
                runRepo,
                periodRepo,
                growthRepo,
                profitRepo,
                liquidityRepo,
                capitalRepo,
                openAiProps,
                calculationService
        );
    }

    /**
     * Creates the independent analytics persistence boundary.
     */
    @Bean
    AnalyticsPersistenceService analyticsPersistenceService(
            ReportingPeriodRepository periodRepo,
            AnalyticsRepository analyticsRepo
    ) {
        return new AnalyticsPersistenceService(periodRepo, analyticsRepo);
    }

    /**
     * Creates complete-dataset analytics orchestration.
     */
    @Bean
    AnalyticsService analyticsService(
            AiClient aiClient,
            AnalyticsPersistenceService persistenceService,
            ReportingPeriodRepository periodRepo,
            GrowthRepository growthRepo,
            ProfitabilityRepository profitRepo,
            LiquidityRepository liquidityRepo,
            CapitalRepository capitalRepo,
            CalculatedGrowthRepository calcGrowthRepo,
            CalculatedProfitabilityRepository calcProfitRepo,
            CalculatedLiquidityRepository calcLiquidityRepo,
            CalculatedCapitalRepository calcCapitalRepo
    ) {
        return new AnalyticsService(
                aiClient,
                persistenceService,
                periodRepo,
                growthRepo,
                profitRepo,
                liquidityRepo,
                capitalRepo,
                calcGrowthRepo,
                calcProfitRepo,
                calcLiquidityRepo,
                calcCapitalRepo
        );
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
