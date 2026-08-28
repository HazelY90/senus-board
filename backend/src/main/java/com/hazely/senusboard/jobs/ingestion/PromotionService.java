package com.hazely.senusboard.jobs.ingestion;

import com.hazely.senusboard.entities.DimensionEntity;
import com.hazely.senusboard.entities.ExtractionItemEntity;
import com.hazely.senusboard.entities.MetricEntity;
import com.hazely.senusboard.entities.MetricValueEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.entities.enums.IngestionStatus;
import com.hazely.senusboard.entities.enums.ValidationStatus;
import com.hazely.senusboard.entities.enums.ValueStatus;
import com.hazely.senusboard.repositories.ExtractionItemRepository;
import com.hazely.senusboard.repositories.MetricRepository;
import com.hazely.senusboard.repositories.MetricValueRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Confirms pending extraction items and promotes them into formal metric values.
 *
 * <p>This service will resolve existing period, metric, dimension, and source records before
 * writing a metric value in one short transaction. Promotion must be idempotent so repeated job
 * execution does not duplicate formal data.</p>
 */
public class PromotionService {

    private final ExtractionItemRepository itemRepo;
    private final ReportingPeriodRepository periodRepo;
    private final MetricRepository metricRepo;
    private final MetricValueRepository valueRepo;

    public PromotionService(
            ExtractionItemRepository itemRepo,
            ReportingPeriodRepository periodRepo,
            MetricRepository metricRepo,
            MetricValueRepository valueRepo
    ) {
        this.itemRepo = itemRepo;
        this.periodRepo = periodRepo;
        this.metricRepo = metricRepo;
        this.valueRepo = valueRepo;
    }

    /** Confirms every pending item in a completed run and writes its formal metric value. */
    @Transactional
    public void promote(Long runId) {
        List<ExtractionItemEntity> items = itemRepo.findAllByIngestionRunIdAndValidationStatus(
                runId,
                ValidationStatus.PENDING
        );
        if (items.isEmpty()) {
            return;
        }
        if (items.getFirst().getIngestionRun().getStatus() != IngestionStatus.COMPLETED) {
            throw new IllegalStateException("Only completed ingestion runs can be promoted");
        }

        Map<String, ReportingPeriodEntity> periods = periodMap();
        Map<String, MetricEntity> metrics = metricMap();

        Set<ValueKey> keys = new HashSet<>();
        List<MetricValueEntity> values = new ArrayList<>();
        for (ExtractionItemEntity item : items) {
            if (item.getDimension() == null) {
                item.setValidationStatus(ValidationStatus.REJECTED);
                continue;
            }
            ReportingPeriodEntity period = require(periods, item.getPeriodCode(), "period");
            MetricEntity metric = require(metrics, item.getMetricCode(), "metric");
            DimensionEntity dimension = item.getDimension();
            if (metric.getUnit() != item.getUnit()) {
                throw new IllegalArgumentException("Metric unit does not match catalogue: " + metric.getCode());
            }

            ValueKey key = new ValueKey(period.getCode(), metric.getCode(), dimension.getId());
            if (!keys.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate metric value in ingestion run: " + period.getCode() + "/" + metric.getCode()
                );
            }

            MetricValueEntity value = valueRepo.findByPeriodAndMetricAndDimension(period, metric, dimension)
                    .orElseGet(MetricValueEntity::new);
            value.setPeriod(period);
            value.setMetric(metric);
            value.setDimension(dimension);
            value.setValue(item.getNumericValue());
            value.setValueStatus(ValueStatus.REPORTED);
            value.setSourceDocument(item.getIngestionRun().getSourceDocument());
            value.setSourcePage(item.getSourcePage());
            value.setComments(null);
            value.setExtractionItem(item);
            values.add(value);
        }

        if (!values.isEmpty()) {
            valueRepo.saveAll(values);
        }
        items.stream()
                .filter(item -> item.getValidationStatus() == ValidationStatus.PENDING)
                .forEach(item -> item.setValidationStatus(ValidationStatus.VERIFIED));
        itemRepo.saveAll(items);
    }

    private Map<String, ReportingPeriodEntity> periodMap() {
        Map<String, ReportingPeriodEntity> periods = new HashMap<>();
        periodRepo.findAll().forEach(period -> periods.put(period.getCode(), period));
        return periods;
    }

    private Map<String, MetricEntity> metricMap() {
        Map<String, MetricEntity> metrics = new HashMap<>();
        metricRepo.findAll().forEach(metric -> metrics.put(metric.getCode(), metric));
        return metrics;
    }

    private <T> T require(Map<String, T> values, String code, String label) {
        T value = values.get(code);
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + label + ": " + code);
        }
        return value;
    }

    private record ValueKey(String periodCode, String metricCode, Long dimensionId) {
    }
}
