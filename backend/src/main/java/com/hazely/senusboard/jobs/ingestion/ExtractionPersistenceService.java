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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persists source provenance, ingestion lifecycle state, and pending extraction items. */
public class ExtractionPersistenceService {

    private final SourceDocumentRepository sourceRepo;
    private final IngestionRunRepository runRepo;
    private final ExtractionItemRepository itemRepo;
    private final MetricRepository metricRepo;
    private final DimensionRepository dimensionRepo;
    private final ReportingPeriodRepository periodRepo;
    private final OpenAiProperties openAiProps;

    public ExtractionPersistenceService(
            SourceDocumentRepository sourceRepo,
            IngestionRunRepository runRepo,
            ExtractionItemRepository itemRepo,
            MetricRepository metricRepo,
            DimensionRepository dimensionRepo,
            ReportingPeriodRepository periodRepo,
            OpenAiProperties openAiProps
    ) {
        this.sourceRepo = sourceRepo;
        this.runRepo = runRepo;
        this.itemRepo = itemRepo;
        this.metricRepo = metricRepo;
        this.dimensionRepo = dimensionRepo;
        this.periodRepo = periodRepo;
        this.openAiProps = openAiProps;
    }

    /** Creates or reuses source metadata and starts a fresh run for the downloaded file. */
    @Transactional(rollbackFor = Exception.class)
    public Long start(DownloadedDocument doc) throws IOException {
        Path file = doc.file().toAbsolutePath().normalize();
        String hash = hash(file);
        SourceDocumentEntity source = sourceRepo.findByFileHash(hash)
                .orElseGet(SourceDocumentEntity::new);
        source.setName(file.getFileName().toString());
        source.setDocumentType(doc.documentType());
        source.setSourceUrl(doc.sourceUrl().toString());
        source.setLocalPath(file.toString());
        source.setFileHash(hash);
        source = sourceRepo.save(source);

        IngestionRunEntity run = new IngestionRunEntity();
        run.setSourceDocument(source);
        run.setModelName(openAiProps.getModel());
        run.setStatus(IngestionStatus.RUNNING);
        run.setStartedAt(Instant.now());
        return runRepo.save(run).getId();
    }

    /** Validates and stores all extracted items, then completes the run atomically. */
    @Transactional
    public void complete(Long runId, AiExtractionResult result) {
        IngestionRunEntity run = getRun(runId);
        requireState(run, IngestionStatus.RUNNING);

        LocalDate publicationDate = parseDate(result.publicationDate());
        run.getSourceDocument().setPublicationDate(publicationDate);

        Set<String> periods = savePeriods(result.reportingPeriods());
        Map<String, MetricEntity> metrics = metricMap();
        Map<DimensionKey, DimensionEntity> dimensions = dimensionMap();
        List<ExtractionItemEntity> items = requireItems(result.extractionItems()).stream()
                .map(item -> toEntity(run, item, periods, metrics, dimensions))
                .toList();

        itemRepo.saveAll(items);
        run.setStatus(IngestionStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(null);
        runRepo.save(run);
    }

    /** Marks a started run as failed after extraction or persistence rejects the result. */
    @Transactional
    public void fail(Long runId, Throwable error) {
        IngestionRunEntity run = getRun(runId);
        run.setStatus(IngestionStatus.FAILED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(errorMessage(error));
        runRepo.save(run);
    }

    private ExtractionItemEntity toEntity(
            IngestionRunEntity run,
            AiExtractionResult.ExtractionItem item,
            Set<String> periods,
            Map<String, MetricEntity> metrics,
            Map<DimensionKey, DimensionEntity> dimensions
    ) {
        String periodCode = requireText(item.periodCode(), "periodCode");
        if (!periods.contains(periodCode)) {
            throw new IllegalArgumentException("Unknown reporting period: " + periodCode);
        }

        String metricCode = requireText(item.metricCode(), "metricCode");
        MetricEntity metric = metrics.get(metricCode);
        if (metric == null) {
            throw new IllegalArgumentException("Unknown metric: " + metricCode);
        }

        MetricUnit unit = enumValue(MetricUnit.class, item.unit(), "unit");
        if (unit != metric.getUnit()) {
            throw new IllegalArgumentException("Metric unit does not match catalogue: " + metricCode);
        }

        DimensionEntity dimension = dimension(item, dimensions);
        requireRange(item.confidence(), BigDecimal.ZERO, BigDecimal.ONE, "confidence");
        if (item.numericValue() == null) {
            throw new IllegalArgumentException("numericValue is required");
        }
        if (item.sourcePage() < 1) {
            throw new IllegalArgumentException("sourcePage must be positive");
        }

        ExtractionItemEntity entity = new ExtractionItemEntity();
        entity.setIngestionRun(run);
        entity.setPeriodCode(periodCode);
        entity.setMetricCode(metricCode);
        entity.setRawValue(requireText(item.rawValue(), "rawValue"));
        entity.setNumericValue(item.numericValue());
        entity.setUnit(unit);
        entity.setDimension(dimension);
        entity.setSourcePage(item.sourcePage());
        entity.setSourceText(requireText(item.sourceText(), "sourceText"));
        entity.setConfidence(item.confidence());
        entity.setValidationStatus(ValidationStatus.PENDING);
        return entity;
    }

    private DimensionEntity dimension(
            AiExtractionResult.ExtractionItem item,
            Map<DimensionKey, DimensionEntity> dimensions
    ) {
        boolean hasType = item.dimensionType() != null && !item.dimensionType().isBlank();
        boolean hasCode = item.dimensionCode() != null && !item.dimensionCode().isBlank();
        if (!hasType && !hasCode) {
            return null;
        }
        if (!hasType || !hasCode) {
            throw new IllegalArgumentException("dimensionType and dimensionCode must be supplied together");
        }

        DimensionType type = enumValue(DimensionType.class, item.dimensionType(), "dimensionType");
        DimensionKey key = new DimensionKey(type, item.dimensionCode());
        DimensionEntity dimension = dimensions.get(key);
        if (dimension == null) {
            throw new IllegalArgumentException("Unknown dimension: " + item.dimensionCode());
        }
        return dimension;
    }

    private Set<String> savePeriods(List<AiExtractionResult.ReportingPeriod> periods) {
        if (periods == null) {
            throw new IllegalArgumentException("reportingPeriods is required");
        }
        Set<String> codes = new HashSet<>();
        for (AiExtractionResult.ReportingPeriod period : periods) {
            String code = requireText(period.code(), "reportingPeriods.code");
            if (!codes.add(code)) {
                throw new IllegalArgumentException("Duplicate reporting period: " + code);
            }
            savePeriod(period, code);
        }
        return codes;
    }

    private void savePeriod(AiExtractionResult.ReportingPeriod item, String code) {
        String label = requireText(item.label(), "reportingPeriods.label");
        PeriodType type = enumValue(PeriodType.class, item.periodType(), "reportingPeriods.periodType");
        LocalDate start = LocalDate.parse(requireText(item.startDate(), "reportingPeriods.startDate"));
        LocalDate end = LocalDate.parse(requireText(item.endDate(), "reportingPeriods.endDate"));

        var period = periodRepo.findByCode(code)
                .orElseGet(com.hazely.senusboard.entities.ReportingPeriodEntity::new);
        period.setCode(code);
        period.setLabel(label);
        period.setPeriodType(type);
        period.setStartDate(start);
        period.setEndDate(end);
        periodRepo.save(period);
    }

    private List<AiExtractionResult.ExtractionItem> requireItems(
            List<AiExtractionResult.ExtractionItem> items
    ) {
        if (items == null) {
            throw new IllegalArgumentException("extractionItems is required");
        }
        return items;
    }

    private Map<String, MetricEntity> metricMap() {
        Map<String, MetricEntity> metrics = new HashMap<>();
        metricRepo.findAll().forEach(metric -> metrics.put(metric.getCode(), metric));
        return metrics;
    }

    private Map<DimensionKey, DimensionEntity> dimensionMap() {
        Map<DimensionKey, DimensionEntity> dimensions = new HashMap<>();
        dimensionRepo.findAll().forEach(dimension -> dimensions.put(
                new DimensionKey(dimension.getDimensionType(), dimension.getCode()),
                dimension
        ));
        return dimensions;
    }

    private IngestionRunEntity getRun(Long runId) {
        return runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ingestion run: " + runId));
    }

    private void requireState(IngestionRunEntity run, IngestionStatus status) {
        if (run.getStatus() != status) {
            throw new IllegalStateException("Ingestion run is not " + status);
        }
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private void requireRange(BigDecimal value, BigDecimal min, BigDecimal max, String field) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(field + " is outside the allowed range");
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, requireText(value, field));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported " + field + ": " + value, ex);
        }
    }

    private String hash(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private String errorMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }

    private record DimensionKey(DimensionType type, String code) {
    }
}
