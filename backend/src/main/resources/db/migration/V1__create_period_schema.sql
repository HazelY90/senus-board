-- Create the complete period-based reporting schema from an empty database.

CREATE TABLE reporting_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    label VARCHAR(100) NOT NULL,
    period_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reporting_periods PRIMARY KEY (id),
    CONSTRAINT uq_reporting_periods_code UNIQUE (code),
    CONSTRAINT chk_reporting_periods_type
        CHECK (period_type IN ('FULL_YEAR', 'HALF_YEAR')),
    CONSTRAINT chk_reporting_periods_dates
        CHECK (start_date <= end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE source_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    publication_date DATE NULL,
    source_url VARCHAR(1000) NULL,
    local_path VARCHAR(1000) NULL,
    file_hash VARCHAR(128) NOT NULL,
    ai_summary TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_source_documents PRIMARY KEY (id),
    CONSTRAINT uq_source_documents_file_hash UNIQUE (file_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ingestion_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_document_id BIGINT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    error_message TEXT NULL,
    CONSTRAINT pk_ingestion_runs PRIMARY KEY (id),
    CONSTRAINT fk_ingestion_runs_source_document
        FOREIGN KEY (source_document_id) REFERENCES source_documents (id),
    CONSTRAINT chk_ingestion_runs_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_ingestion_runs_dates
        CHECK (completed_at IS NULL OR completed_at >= started_at),
    INDEX idx_ingestion_runs_source_document (source_document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE growth (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    revenue DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_growth PRIMARY KEY (id),
    CONSTRAINT uq_growth_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_growth_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE profitability (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    gross_profit DECIMAL(20, 4) NULL,
    gross_margin DECIMAL(20, 4) NULL,
    operating_loss DECIMAL(20, 4) NULL,
    cost_of_sales DECIMAL(20, 4) NULL,
    administrative_expenses DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_profitability PRIMARY KEY (id),
    CONSTRAINT uq_profitability_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_profitability_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE liquidity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    cash_balance DECIMAL(20, 4) NULL,
    operating_cash_flow DECIMAL(20, 4) NULL,
    working_capital_movement DECIMAL(20, 4) NULL,
    current_assets DECIMAL(20, 4) NULL,
    current_liabilities DECIMAL(20, 4) NULL,
    net_current_position DECIMAL(20, 4) NULL,
    capital_expenditure DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_liquidity PRIMARY KEY (id),
    CONSTRAINT uq_liquidity_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_liquidity_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE capital (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    bank_debt DECIMAL(20, 4) NULL,
    loan_movement DECIMAL(20, 4) NULL,
    interest_expense DECIMAL(20, 4) NULL,
    net_asset_position DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_capital PRIMARY KEY (id),
    CONSTRAINT uq_capital_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_capital_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE calculated_growth (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    revenue_growth DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_calculated_growth PRIMARY KEY (id),
    CONSTRAINT uq_calculated_growth_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_calculated_growth_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE calculated_profitability (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    calculated_gross_margin DECIMAL(20, 4) NULL,
    operating_margin DECIMAL(20, 4) NULL,
    cost_of_sales_ratio DECIMAL(20, 4) NULL,
    administrative_expense_ratio DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_calculated_profitability PRIMARY KEY (id),
    CONSTRAINT uq_calculated_profitability_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_calculated_profitability_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE calculated_liquidity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    operating_cash_flow_margin DECIMAL(20, 4) NULL,
    free_cash_flow DECIMAL(20, 4) NULL,
    free_cash_flow_margin DECIMAL(20, 4) NULL,
    current_ratio DECIMAL(20, 4) NULL,
    cash_ratio DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_calculated_liquidity PRIMARY KEY (id),
    CONSTRAINT uq_calculated_liquidity_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_calculated_liquidity_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE calculated_capital (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    net_cash DECIMAL(20, 4) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_calculated_capital PRIMARY KEY (id),
    CONSTRAINT uq_calculated_capital_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_calculated_capital_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE analytics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporting_period_id BIGINT NOT NULL,
    growth_analytics TEXT NULL,
    profitability_analytics TEXT NULL,
    liquidity_analytics TEXT NULL,
    capital_analytics TEXT NULL,
    total_analytics TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_analytics PRIMARY KEY (id),
    CONSTRAINT uq_analytics_period UNIQUE (reporting_period_id),
    CONSTRAINT fk_analytics_period
        FOREIGN KEY (reporting_period_id) REFERENCES reporting_periods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
