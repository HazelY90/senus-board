package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.entities.DimensionEntity;
import com.hazely.senusboard.entities.ExtractionItemEntity;
import com.hazely.senusboard.entities.IngestionRunEntity;
import com.hazely.senusboard.entities.MetricEntity;
import com.hazely.senusboard.entities.SourceDocumentEntity;
import com.hazely.senusboard.entities.enums.DimensionType;
import com.hazely.senusboard.entities.enums.IngestionStatus;
import com.hazely.senusboard.entities.enums.MetricUnit;
import com.hazely.senusboard.entities.enums.PeriodType;
import com.hazely.senusboard.entities.enums.ValidationStatus;
import com.hazely.senusboard.repositories.DimensionRepository;
import com.hazely.senusboard.repositories.ExtractionItemRepository;
import com.hazely.senusboard.repositories.IngestionRunRepository;
import com.hazely.senusboard.repositories.MetricRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtractionPersistenceServiceTest {

    @TempDir
    Path dir;

    @Test
    void persistsSourceRunAndPendingItems() throws Exception {
        SourceDocumentRepository sourceRepo = mock(SourceDocumentRepository.class);
        IngestionRunRepository runRepo = mock(IngestionRunRepository.class);
        ExtractionItemRepository itemRepo = mock(ExtractionItemRepository.class);
        MetricRepository metricRepo = mock(MetricRepository.class);
        DimensionRepository dimensionRepo = mock(DimensionRepository.class);
        ReportingPeriodRepository periodRepo = mock(ReportingPeriodRepository.class);
        OpenAiProperties props = new OpenAiProperties();
        props.setModel("test-model");

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

        MetricEntity metric = new MetricEntity();
        metric.setCode("REVENUE");
        metric.setUnit(MetricUnit.EUR);
        when(metricRepo.findAll()).thenReturn(List.of(metric));

        DimensionEntity dimension = new DimensionEntity();
        dimension.setDimensionType(DimensionType.TOTAL);
        dimension.setCode("TOTAL");
        when(dimensionRepo.findAll()).thenReturn(List.of(dimension));
        when(periodRepo.findByCode("FY2024")).thenReturn(Optional.empty());

        ExtractionPersistenceService service = new ExtractionPersistenceService(
                sourceRepo,
                runRepo,
                itemRepo,
                metricRepo,
                dimensionRepo,
                periodRepo,
                props
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
                "2025-03-01",
                List.of(new AiExtractionResult.ReportingPeriod(
                        "FY2024", "FY 2024", "FULL_YEAR", "2024-01-01", "2024-12-31"
                )),
                List.of(new AiExtractionResult.ExtractionItem(
                        "FY2024",
                        "REVENUE",
                        "EUR 1.5m",
                        new BigDecimal("1500000"),
                        "EUR",
                        "TOTAL",
                        "TOTAL",
                        3,
                        "Revenue was EUR 1.5m.",
                        new BigDecimal("0.9900")
                ))
        );
        service.complete(runId, result);

        assertThat(runId).isEqualTo(20L);
        IngestionRunEntity run = runRef.get();
        assertThat(run.getStatus()).isEqualTo(IngestionStatus.COMPLETED);
        assertThat(run.getCompletedAt()).isNotNull();
        assertThat(run.getSourceDocument().getPublicationDate()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(run.getSourceDocument().getFileHash()).hasSize(64);
        assertThat(run.getSourceDocument().getSourceUrl()).isEqualTo("https://example.com/report.pdf");

        ArgumentCaptor<com.hazely.senusboard.entities.ReportingPeriodEntity> periodCaptor =
                ArgumentCaptor.forClass(com.hazely.senusboard.entities.ReportingPeriodEntity.class);
        verify(periodRepo).save(periodCaptor.capture());
        assertThat(periodCaptor.getValue().getCode()).isEqualTo("FY2024");
        assertThat(periodCaptor.getValue().getPeriodType()).isEqualTo(PeriodType.FULL_YEAR);
        assertThat(periodCaptor.getValue().getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(periodCaptor.getValue().getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExtractionItemEntity>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(itemRepo).saveAll(itemsCaptor.capture());
        ExtractionItemEntity item = itemsCaptor.getValue().getFirst();
        assertThat(item.getIngestionRun()).isSameAs(run);
        assertThat(item.getDimension()).isSameAs(dimension);
        assertThat(item.getValidationStatus()).isEqualTo(ValidationStatus.PENDING);
        assertThat(item.getSourcePage()).isEqualTo(3);
    }
}
