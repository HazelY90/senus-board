package com.hazely.senusboard.entities;

import com.hazely.senusboard.entities.enums.ValueStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** Stores a validated metric value for one period and dimension. */
@Entity
@Table(
        name = "metric_values",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_metric_values_period_metric_dimension",
                columnNames = {"period_id", "metric_id", "dimension_id"}
        ),
        indexes = {
                @Index(name = "idx_metric_values_source_document", columnList = "source_document_id"),
                @Index(name = "idx_metric_values_extraction_item", columnList = "extraction_item_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MetricValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "period_id", nullable = false)
    private ReportingPeriodEntity period;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "metric_id", nullable = false)
    private MetricEntity metric;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "dimension_id", nullable = false)
    private DimensionEntity dimension;

    @Column(name = "`value`", nullable = false, precision = 20, scale = 4)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_status", nullable = false, length = 20)
    private ValueStatus valueStatus;

    @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "source_document_id")
    private SourceDocumentEntity sourceDocument;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "extraction_item_id")
    private ExtractionItemEntity extractionItem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
