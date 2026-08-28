package com.hazely.senusboard.entities;

import com.hazely.senusboard.entities.enums.MetricCategory;
import com.hazely.senusboard.entities.enums.MetricUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** Stores stable metric identity, category, and unit metadata. */
@Entity
@Table(
        name = "metrics",
        uniqueConstraints = @UniqueConstraint(name = "uq_metrics_code", columnNames = "code"),
        indexes = @Index(name = "idx_metrics_category", columnList = "category")
)
@Getter
@Setter
@NoArgsConstructor
public class MetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetricCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetricUnit unit;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
