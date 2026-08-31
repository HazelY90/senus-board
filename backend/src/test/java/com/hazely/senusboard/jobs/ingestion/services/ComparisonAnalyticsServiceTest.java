package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.entities.enums.PeriodType;
import com.hazely.senusboard.jobs.ingestion.AiClient;
import com.hazely.senusboard.jobs.ingestion.dtos.AiComparisonResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AnalyticsDataset;
import com.hazely.senusboard.jobs.ingestion.dtos.ComparisonDataset;
import com.hazely.senusboard.jobs.ingestion.dtos.ComparisonPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparisonAnalyticsServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private ComparisonPersistenceService persistenceService;

    @Test
    void analyzesChangedComparison() throws Exception {
        AnalyticsDataset.PeriodData baseData = periodData(
                "FY2024",
                "2023-07-01",
                "2024-06-30",
                "100",
                "10"
        );
        AnalyticsDataset.PeriodData targetData = periodData(
                "FY2025",
                "2024-07-01",
                "2025-06-30",
                "125",
                "25"
        );
        ReportingPeriodEntity base = period(1L, "FY2024", LocalDate.of(2024, 6, 30));
        ReportingPeriodEntity target = period(2L, "FY2025", LocalDate.of(2025, 6, 30));
        when(analyticsService.load()).thenReturn(new AnalyticsDataset(List.of(baseData, targetData)));
        when(analyticsService.period("FY2024")).thenReturn(base);
        when(analyticsService.period("FY2025")).thenReturn(target);
        when(persistenceService.isCurrent(any(), any(), any())).thenReturn(false);
        AiComparisonResult result = result();
        when(aiClient.compare(any())).thenReturn(result);
        ComparisonAnalyticsService service = new ComparisonAnalyticsService(
                aiClient,
                analyticsService,
                persistenceService,
                new ObjectMapper()
        );

        service.analyze();

        ArgumentCaptor<ComparisonDataset> dataCaptor = ArgumentCaptor.forClass(ComparisonDataset.class);
        verify(aiClient).compare(dataCaptor.capture());
        ComparisonDataset.ComparisonData item = dataCaptor.getValue().comparisons().getFirst();
        assertThat(item.basePeriodCode()).isEqualTo("FY2024");
        assertThat(item.targetPeriodCode()).isEqualTo("FY2025");
        assertThat(item.changes().growth().getFirst().absoluteChange())
                .isEqualByComparingTo("25.0000");
        assertThat(item.changes().growth().getFirst().percentageChange())
                .isEqualByComparingTo("25.0000");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<ComparisonPair, String>> hashCaptor = ArgumentCaptor.forClass(Map.class);
        verify(persistenceService).save(any(), hashCaptor.capture());
        assertThat(hashCaptor.getValue())
                .containsOnlyKeys(new ComparisonPair("FY2024", "FY2025"));
        assertThat(hashCaptor.getValue().values().iterator().next()).hasSize(64);
    }

    @Test
    void skipsCurrentComparison() throws Exception {
        AnalyticsDataset.PeriodData baseData = periodData(
                "FY2024",
                "2023-07-01",
                "2024-06-30",
                "100",
                "10"
        );
        AnalyticsDataset.PeriodData targetData = periodData(
                "FY2025",
                "2024-07-01",
                "2025-06-30",
                "125",
                "25"
        );
        ReportingPeriodEntity base = period(1L, "FY2024", LocalDate.of(2024, 6, 30));
        ReportingPeriodEntity target = period(2L, "FY2025", LocalDate.of(2025, 6, 30));
        when(analyticsService.load()).thenReturn(new AnalyticsDataset(List.of(baseData, targetData)));
        when(analyticsService.period("FY2024")).thenReturn(base);
        when(analyticsService.period("FY2025")).thenReturn(target);
        when(persistenceService.isCurrent(any(), any(), any())).thenReturn(true);
        ComparisonAnalyticsService service = new ComparisonAnalyticsService(
                aiClient,
                analyticsService,
                persistenceService,
                new ObjectMapper()
        );

        service.analyze();

        verify(aiClient, never()).compare(any());
        verify(persistenceService, never()).save(any(), any());
    }

    private AnalyticsDataset.PeriodData periodData(
            String code,
            String start,
            String end,
            String revenue,
            String growth
    ) {
        return new AnalyticsDataset.PeriodData(
                code,
                code,
                PeriodType.FULL_YEAR.name(),
                start,
                end,
                new AnalyticsDataset.Growth(
                        new AiExtractionResult.Growth(new BigDecimal(revenue)),
                        new AnalyticsDataset.CalculatedGrowth(new BigDecimal(growth))
                ),
                null,
                null,
                null
        );
    }

    private ReportingPeriodEntity period(Long id, String code, LocalDate end) {
        ReportingPeriodEntity period = new ReportingPeriodEntity();
        period.setId(id);
        period.setCode(code);
        period.setPeriodType(PeriodType.FULL_YEAR);
        period.setEndDate(end);
        return period;
    }

    private AiComparisonResult result() {
        return new AiComparisonResult(List.of(new AiComparisonResult.ComparisonAnalytics(
                "FY2024",
                "FY2025",
                "Growth",
                null,
                null,
                null,
                "Total"
        )));
    }
}
