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

/** Stores stable capital values for one reporting period. */
@Entity
@Table(
        name = "capital",
        uniqueConstraints = @UniqueConstraint(name = "uq_capital_period", columnNames = "reporting_period_id")
)
@Getter
@Setter
@NoArgsConstructor
public class CapitalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporting_period_id", nullable = false)
    private ReportingPeriodEntity reportingPeriod;

    @Column(name = "bank_debt", precision = 20, scale = 4)
    private BigDecimal bankDebt;

    @Column(name = "loan_movement", precision = 20, scale = 4)
    private BigDecimal loanMovement;

    @Column(name = "interest_expense", precision = 20, scale = 4)
    private BigDecimal interestExpense;

    @Column(name = "net_asset_position", precision = 20, scale = 4)
    private BigDecimal netAssetPosition;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
