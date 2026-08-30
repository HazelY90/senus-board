-- Create stored AI analytics for ordered reporting-period comparisons.

CREATE TABLE comparison_analytics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    base_period_id BIGINT NOT NULL,
    target_period_id BIGINT NOT NULL,
    growth_analytics TEXT NULL,
    profitability_analytics TEXT NULL,
    liquidity_analytics TEXT NULL,
    capital_analytics TEXT NULL,
    total_analytics TEXT NULL,
    input_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_comparison_analytics PRIMARY KEY (id),
    CONSTRAINT uq_comparison_analytics_periods UNIQUE (base_period_id, target_period_id),
    CONSTRAINT fk_comparison_analytics_base_period
        FOREIGN KEY (base_period_id) REFERENCES reporting_periods (id),
    CONSTRAINT fk_comparison_analytics_target_period
        FOREIGN KEY (target_period_id) REFERENCES reporting_periods (id),
    CONSTRAINT chk_comparison_analytics_periods
        CHECK (base_period_id <> target_period_id),
    INDEX idx_comparison_analytics_target_period (target_period_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
