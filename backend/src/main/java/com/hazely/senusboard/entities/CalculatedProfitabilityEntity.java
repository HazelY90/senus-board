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

/** Stores calculated profitability values for one reporting period. */
@Entity
@Table(
        name = "calculated_profitability",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_calculated_profitability_period",
                columnNames = "reporting_period_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CalculatedProfitabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporting_period_id", nullable = false)
    private ReportingPeriodEntity reportingPeriod;

    @Column(name = "calculated_gross_margin", precision = 20, scale = 4)
    private BigDecimal calculatedGrossMargin;

    @Column(name = "operating_margin", precision = 20, scale = 4)
    private BigDecimal operatingMargin;

    @Column(name = "cost_of_sales_ratio", precision = 20, scale = 4)
    private BigDecimal costOfSalesRatio;

    @Column(name = "administrative_expense_ratio", precision = 20, scale = 4)
    private BigDecimal administrativeExpenseRatio;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
