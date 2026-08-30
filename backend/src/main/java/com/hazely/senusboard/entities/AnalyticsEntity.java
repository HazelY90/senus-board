package com.hazely.senusboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** Stores AI analytics for one reporting period. */
@Entity
@Table(
        name = "analytics",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_analytics_period",
                columnNames = "reporting_period_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporting_period_id", nullable = false)
    private ReportingPeriodEntity reportingPeriod;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
