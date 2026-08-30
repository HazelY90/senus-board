package com.hazely.senusboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** Stores AI analytics for one ordered reporting-period comparison. */
@Entity
@Table(
        name = "comparison_analytics",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_comparison_analytics_periods",
                columnNames = {"base_period_id", "target_period_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ComparisonAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_period_id", nullable = false)
    private ReportingPeriodEntity basePeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_period_id", nullable = false)
    private ReportingPeriodEntity targetPeriod;

    @Column(name = "growth_analytics", columnDefinition = "TEXT")
    private String growthAnalytics;

    @Column(name = "profitability_analytics", columnDefinition = "TEXT")
    private String profitabilityAnalytics;

    @Column(name = "liquidity_analytics", columnDefinition = "TEXT")
    private String liquidityAnalytics;

    @Column(name = "capital_analytics", columnDefinition = "TEXT")
    private String capitalAnalytics;

    @Column(name = "total_analytics", columnDefinition = "TEXT")
    private String totalAnalytics;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
