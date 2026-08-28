package com.hazely.senusboard.entities;

import com.hazely.senusboard.entities.enums.TargetOperator;
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

/** Stores a strategic target and its comparison rule. */
@Entity
@Table(
        name = "strategic_targets",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_strategic_targets_metric_dimension_period",
                columnNames = {"metric_id", "dimension_id", "target_period_id"}
        ),
        indexes = @Index(
                name = "idx_strategic_targets_source_document",
                columnList = "source_document_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class StrategicTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "`operator`", nullable = false, length = 30)
    private TargetOperator operator;

    @Column(name = "target_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal targetValue;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "target_period_id", nullable = false)
    private ReportingPeriodEntity targetPeriod;

    @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "source_document_id")
    private SourceDocumentEntity sourceDocument;

    @Column(name = "source_page")
    private Integer sourcePage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
