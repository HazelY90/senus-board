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

import java.math.BigDecimal;
import java.time.Instant;

/** Stores stable profitability values for one reporting period. */
@Entity
@Table(
        name = "profitability",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_profitability_period",
                columnNames = "reporting_period_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ProfitabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporting_period_id", nullable = false)
    private ReportingPeriodEntity reportingPeriod;

    @Column(name = "gross_profit", precision = 20, scale = 4)
    private BigDecimal grossProfit;

    @Column(name = "gross_margin", precision = 20, scale = 4)
    private BigDecimal grossMargin;

    @Column(name = "operating_loss", precision = 20, scale = 4)
    private BigDecimal operatingLoss;

    @Column(name = "cost_of_sales", precision = 20, scale = 4)
    private BigDecimal costOfSales;

    @Column(name = "administrative_expenses", precision = 20, scale = 4)
    private BigDecimal administrativeExpenses;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
