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

/** Stores stable liquidity values for one reporting period. */
@Entity
@Table(
        name = "liquidity",
        uniqueConstraints = @UniqueConstraint(name = "uq_liquidity_period", columnNames = "reporting_period_id")
)
@Getter
@Setter
@NoArgsConstructor
public class LiquidityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporting_period_id", nullable = false)
    private ReportingPeriodEntity reportingPeriod;

    @Column(name = "cash_balance", precision = 20, scale = 4)
    private BigDecimal cashBalance;

    @Column(name = "operating_cash_flow", precision = 20, scale = 4)
    private BigDecimal operatingCashFlow;

    @Column(name = "working_capital_movement", precision = 20, scale = 4)
    private BigDecimal workingCapitalMovement;

    @Column(name = "current_assets", precision = 20, scale = 4)
    private BigDecimal currentAssets;

    @Column(name = "current_liabilities", precision = 20, scale = 4)
    private BigDecimal currentLiabilities;

    @Column(name = "net_current_position", precision = 20, scale = 4)
    private BigDecimal netCurrentPosition;

    @Column(name = "capital_expenditure", precision = 20, scale = 4)
    private BigDecimal capitalExpenditure;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
