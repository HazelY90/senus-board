package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.entities.DimensionEntity;
import com.hazely.senusboard.entities.ExtractionItemEntity;
import com.hazely.senusboard.entities.IngestionRunEntity;
import com.hazely.senusboard.entities.MetricEntity;
import com.hazely.senusboard.entities.MetricValueEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.entities.SourceDocumentEntity;
import com.hazely.senusboard.entities.enums.DimensionType;
import com.hazely.senusboard.entities.enums.IngestionStatus;
import com.hazely.senusboard.entities.enums.MetricUnit;
import com.hazely.senusboard.entities.enums.ValidationStatus;
import com.hazely.senusboard.entities.enums.ValueStatus;
import com.hazely.senusboard.repositories.ExtractionItemRepository;
import com.hazely.senusboard.repositories.MetricRepository;
import com.hazely.senusboard.repositories.MetricValueRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PromotionServiceTest {

    @Test
    void promotesPendingItemToReportedValue() {
        ExtractionItemRepository itemRepo = mock(ExtractionItemRepository.class);
        ReportingPeriodRepository periodRepo = mock(ReportingPeriodRepository.class);
        MetricRepository metricRepo = mock(MetricRepository.class);
        MetricValueRepository valueRepo = mock(MetricValueRepository.class);

        ReportingPeriodEntity period = new ReportingPeriodEntity();
        period.setId(1L);
        period.setCode("FY2024");
        MetricEntity metric = new MetricEntity();
        metric.setId(2L);
        metric.setCode("REVENUE");
        metric.setUnit(MetricUnit.EUR);
        DimensionEntity total = new DimensionEntity();
        total.setId(3L);
        total.setDimensionType(DimensionType.TOTAL);
        total.setCode("TOTAL");
        SourceDocumentEntity source = new SourceDocumentEntity();
        source.setId(4L);
        IngestionRunEntity run = new IngestionRunEntity();
        run.setId(5L);
        run.setStatus(IngestionStatus.COMPLETED);
        run.setSourceDocument(source);
        ExtractionItemEntity item = new ExtractionItemEntity();
        item.setId(6L);
        item.setIngestionRun(run);
        item.setPeriodCode("FY2024");
        item.setMetricCode("REVENUE");
        item.setNumericValue(new BigDecimal("1500000"));
        item.setUnit(MetricUnit.EUR);
        item.setDimension(total);
        item.setSourcePage(3);
        item.setValidationStatus(ValidationStatus.PENDING);

        when(itemRepo.findAllByIngestionRunIdAndValidationStatus(5L, ValidationStatus.PENDING))
                .thenReturn(List.of(item));
        when(periodRepo.findAll()).thenReturn(List.of(period));
        when(metricRepo.findAll()).thenReturn(List.of(metric));
        when(valueRepo.findByPeriodAndMetricAndDimension(period, metric, total))
                .thenReturn(Optional.empty());

        PromotionService service = new PromotionService(
                itemRepo,
                periodRepo,
                metricRepo,
                valueRepo
        );
        service.promote(5L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MetricValueEntity>> valuesCaptor = ArgumentCaptor.forClass(List.class);
        verify(valueRepo).saveAll(valuesCaptor.capture());
        MetricValueEntity value = valuesCaptor.getValue().getFirst();
        assertThat(value.getPeriod()).isSameAs(period);
        assertThat(value.getMetric()).isSameAs(metric);
        assertThat(value.getDimension()).isSameAs(total);
        assertThat(value.getValue()).isEqualByComparingTo("1500000");
        assertThat(value.getValueStatus()).isEqualTo(ValueStatus.REPORTED);
        assertThat(value.getSourceDocument()).isSameAs(source);
        assertThat(value.getSourcePage()).isEqualTo(3);
        assertThat(value.getExtractionItem()).isSameAs(item);
        assertThat(item.getValidationStatus()).isEqualTo(ValidationStatus.VERIFIED);
        verify(itemRepo).saveAll(any());
    }

    @Test
    void rejectsItemWithoutDimension() {
        ExtractionItemRepository itemRepo = mock(ExtractionItemRepository.class);
        ReportingPeriodRepository periodRepo = mock(ReportingPeriodRepository.class);
        MetricRepository metricRepo = mock(MetricRepository.class);
        MetricValueRepository valueRepo = mock(MetricValueRepository.class);
        IngestionRunEntity run = new IngestionRunEntity();
        run.setStatus(IngestionStatus.COMPLETED);
        ExtractionItemEntity item = new ExtractionItemEntity();
        item.setIngestionRun(run);
        item.setValidationStatus(ValidationStatus.PENDING);
        when(itemRepo.findAllByIngestionRunIdAndValidationStatus(7L, ValidationStatus.PENDING))
                .thenReturn(List.of(item));

        PromotionService service = new PromotionService(itemRepo, periodRepo, metricRepo, valueRepo);
        service.promote(7L);

        assertThat(item.getValidationStatus()).isEqualTo(ValidationStatus.REJECTED);
        verify(itemRepo).saveAll(List.of(item));
        verifyNoInteractions(valueRepo);
    }
}
