package com.hazely.senusboard.entities;

import com.hazely.senusboard.entities.enums.MetricUnit;
import com.hazely.senusboard.entities.enums.ValidationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** Stores an extracted value while it awaits validation. */
@Entity
@Table(
        name = "extraction_items",
        indexes = {
                @Index(
                        name = "idx_extraction_items_run_validation",
                        columnList = "ingestion_run_id, validation_status"
                ),
                @Index(name = "idx_extraction_items_dimension", columnList = "dimension_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExtractionItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "ingestion_run_id", nullable = false)
    private IngestionRunEntity ingestionRun;

    @Column(name = "period_code", nullable = false, length = 20)
    private String periodCode;

    @Column(name = "metric_code", nullable = false, length = 60)
    private String metricCode;

    @Column(name = "raw_value", nullable = false, length = 255)
    private String rawValue;

    @Column(name = "numeric_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal numericValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetricUnit unit;

    @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "dimension_id")
    private DimensionEntity dimension;

    @Column(name = "source_page", nullable = false)
    private Integer sourcePage;

    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    private ValidationStatus validationStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
