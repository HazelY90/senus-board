package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.entities.GrowthEntity;
import com.hazely.senusboard.entities.IngestionRunEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.entities.SourceDocumentEntity;
import com.hazely.senusboard.entities.enums.IngestionStatus;
import com.hazely.senusboard.entities.enums.PeriodType;
import com.hazely.senusboard.jobs.ingestion.OpenAiProperties;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.DownloadedDocument;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.IngestionRunRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import com.hazely.senusboard.repositories.SourceDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtractionPersistenceServiceTest {

    @TempDir
    Path dir;

    @Test
    void persistsCategoriesWithoutReplacingValuesWithNull() throws Exception {
        SourceDocumentRepository sourceRepo = mock(SourceDocumentRepository.class);
        IngestionRunRepository runRepo = mock(IngestionRunRepository.class);
        ReportingPeriodRepository periodRepo = mock(ReportingPeriodRepository.class);
        GrowthRepository growthRepo = mock(GrowthRepository.class);
        ProfitabilityRepository profitRepo = mock(ProfitabilityRepository.class);
        LiquidityRepository liquidityRepo = mock(LiquidityRepository.class);
        CapitalRepository capitalRepo = mock(CapitalRepository.class);
        CalculationService calculationService = mock(CalculationService.class);
        OpenAiProperties props = new OpenAiProperties();
        props.setModel("test-model");
        GrowthEntity existingGrowth = new GrowthEntity();
        existingGrowth.setRevenue(new BigDecimal("700000"));

        AtomicReference<IngestionRunEntity> runRef = new AtomicReference<>();
        when(sourceRepo.findByFileHash(any())).thenReturn(Optional.empty());
        when(sourceRepo.save(any())).thenAnswer(invocation -> {
            SourceDocumentEntity source = invocation.getArgument(0);
            source.setId(10L);
            return source;
        });
        when(runRepo.save(any())).thenAnswer(invocation -> {
            IngestionRunEntity run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(20L);
            }
            runRef.set(run);
            return run;
        });
        when(runRepo.findById(20L)).thenAnswer(invocation -> Optional.of(runRef.get()));
        when(periodRepo.findByCode("FY2025")).thenReturn(Optional.empty());
        when(periodRepo.findByCode("HY2026")).thenReturn(Optional.empty());
        when(periodRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(growthRepo.findByReportingPeriod(any())).thenAnswer(invocation -> {
            ReportingPeriodEntity period = invocation.getArgument(0);
            return period.getCode().equals("FY2025")
                    ? Optional.of(existingGrowth)
                    : Optional.empty();
        });
        when(profitRepo.findByReportingPeriod(any())).thenReturn(Optional.empty());
        when(liquidityRepo.findByReportingPeriod(any())).thenReturn(Optional.empty());
        when(capitalRepo.findByReportingPeriod(any())).thenReturn(Optional.empty());

        ExtractionPersistenceService service = new ExtractionPersistenceService(
                sourceRepo,
                runRepo,
                periodRepo,
                growthRepo,
                profitRepo,
                liquidityRepo,
                capitalRepo,
                props,
                calculationService
        );
        Path file = dir.resolve("report.pdf");
        Files.writeString(file, "source content");
        DownloadedDocument doc = new DownloadedDocument(
                file,
                URI.create("https://example.com/report.pdf"),
                "application/pdf"
        );

        Long runId = service.start(doc);
        AiExtractionResult result = new AiExtractionResult(
                "2025-11-19",
                "FY2025 annual results with FY2024 comparative values.",
                List.of(
                        new AiExtractionResult.PeriodData(
                                "2024-07-01",
                                "2025-06-30",
                                new AiExtractionResult.Growth(null),
                                new AiExtractionResult.Profitability(
                                        new BigDecimal("648450"),
                                        new BigDecimal("77.5"),
                                        new BigDecimal("-633694"),
                                        new BigDecimal("-188541"),
                                        new BigDecimal("-1286058")
                                ),
                                new AiExtractionResult.Liquidity(
                                        new BigDecimal("140135"),
                                        new BigDecimal("-374820"),
                                        new BigDecimal("212467"),
                                        new BigDecimal("263138"),
                                        new BigDecimal("-243846"),
                                        new BigDecimal("19292"),
                                        new BigDecimal("-4451")
                                ),
                                new AiExtractionResult.Capital(
                                        new BigDecimal("83655"),
                                        new BigDecimal("93767"),
                                        new BigDecimal("-2074"),
                                        new BigDecimal("-15575")
                                )
                        ),
                        new AiExtractionResult.PeriodData(
                                "2025-07-01",
                                "2025-12-31",
                                new AiExtractionResult.Growth(new BigDecimal("354813")),
                                null,
                                null,
                                null
                        ),
                        new AiExtractionResult.PeriodData(
                                "2025-07-01",
                                "2025-12-08",
                                new AiExtractionResult.Growth(new BigDecimal("999999")),
                                null,
                                null,
                                null
                        )
                )
        );
        service.complete(runId, result);

        assertThat(runId).isEqualTo(20L);
        IngestionRunEntity run = runRef.get();
        assertThat(run.getStatus()).isEqualTo(IngestionStatus.COMPLETED);
        assertThat(run.getSourceDocument().getPublicationDate()).isEqualTo(LocalDate.of(2025, 11, 19));
        assertThat(run.getSourceDocument().getAiSummary()).contains("FY2025 annual results");
        assertThat(Path.of(run.getSourceDocument().getLocalPath())).isRelative();

        ArgumentCaptor<ReportingPeriodEntity> periodCaptor =
                ArgumentCaptor.forClass(ReportingPeriodEntity.class);
        verify(periodRepo, times(2)).save(periodCaptor.capture());
        ReportingPeriodEntity fullYear = periodCaptor.getAllValues().stream()
                .filter(period -> period.getCode().equals("FY2025"))
                .findFirst()
                .orElseThrow();
        assertThat(fullYear.getLabel()).isEqualTo("Full Year 2025");
        assertThat(fullYear.getPeriodType()).isEqualTo(PeriodType.FULL_YEAR);
        assertThat(fullYear.getStartDate()).isEqualTo(LocalDate.of(2024, 7, 1));
        assertThat(fullYear.getEndDate()).isEqualTo(LocalDate.of(2025, 6, 30));
        ReportingPeriodEntity halfYear = periodCaptor.getAllValues().stream()
                .filter(period -> period.getCode().equals("HY2026"))
                .findFirst()
                .orElseThrow();
        assertThat(halfYear.getLabel()).isEqualTo("Half Year 2026");
        assertThat(halfYear.getPeriodType()).isEqualTo(PeriodType.HALF_YEAR);
        assertThat(halfYear.getStartDate()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(halfYear.getEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));

        ArgumentCaptor<GrowthEntity> growthCaptor = ArgumentCaptor.forClass(GrowthEntity.class);
        verify(growthRepo, times(2)).save(growthCaptor.capture());
        GrowthEntity fullGrowth = growthCaptor.getAllValues().stream()
                .filter(growth -> growth.getReportingPeriod() == fullYear)
                .findFirst()
                .orElseThrow();
        assertThat(fullGrowth.getRevenue()).isEqualByComparingTo("700000");
        GrowthEntity halfGrowth = growthCaptor.getAllValues().stream()
                .filter(growth -> growth.getReportingPeriod() == halfYear)
                .findFirst()
                .orElseThrow();
        assertThat(halfGrowth.getRevenue()).isEqualByComparingTo("354813");
        verify(profitRepo).save(any());
        verify(liquidityRepo).save(any());
        verify(capitalRepo).save(any());
        verify(calculationService).recalculate();
    }
}
