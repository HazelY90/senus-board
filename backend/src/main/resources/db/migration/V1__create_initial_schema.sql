-- Create the initial schema for reporting data, strategic targets, and extraction staging.

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
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_source_documents PRIMARY KEY (id),
    CONSTRAINT uq_source_documents_file_hash UNIQUE (file_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE metrics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(30) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_metrics PRIMARY KEY (id),
    CONSTRAINT uq_metrics_code UNIQUE (code),
    CONSTRAINT chk_metrics_category
        CHECK (category IN ('GROWTH', 'PROFITABILITY', 'LIQUIDITY', 'CAPITAL')),
    CONSTRAINT chk_metrics_unit
        CHECK (unit IN ('EUR', 'PERCENT', 'PERCENTAGE_POINT', 'COUNT', 'RATIO')),
    INDEX idx_metrics_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE dimensions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dimension_type VARCHAR(30) NOT NULL,
    code VARCHAR(80) NOT NULL,
    label VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_dimensions PRIMARY KEY (id),
    CONSTRAINT uq_dimensions_type_code UNIQUE (dimension_type, code),
    CONSTRAINT chk_dimensions_type
        CHECK (dimension_type IN ('TOTAL', 'CUSTOMER_SEGMENT', 'SOLUTION', 'GEOGRAPHY', 'PIPELINE_STAGE'))
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

CREATE TABLE extraction_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ingestion_run_id BIGINT NOT NULL,
    period_code VARCHAR(20) NOT NULL,
    metric_code VARCHAR(60) NOT NULL,
    raw_value VARCHAR(255) NOT NULL,
    numeric_value DECIMAL(20, 4) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    dimension_id BIGINT NULL,
    source_page INT NOT NULL,
    source_text TEXT NOT NULL,
    confidence DECIMAL(5, 4) NOT NULL,
    validation_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_extraction_items PRIMARY KEY (id),
    CONSTRAINT fk_extraction_items_ingestion_run
        FOREIGN KEY (ingestion_run_id) REFERENCES ingestion_runs (id),
    CONSTRAINT fk_extraction_items_dimension
        FOREIGN KEY (dimension_id) REFERENCES dimensions (id) ON DELETE SET NULL,
    CONSTRAINT chk_extraction_items_unit
        CHECK (unit IN ('EUR', 'PERCENT', 'PERCENTAGE_POINT', 'COUNT', 'RATIO')),
    CONSTRAINT chk_extraction_items_page
        CHECK (source_page > 0),
    CONSTRAINT chk_extraction_items_confidence
        CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT chk_extraction_items_validation
        CHECK (validation_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    INDEX idx_extraction_items_run_validation (ingestion_run_id, validation_status),
    INDEX idx_extraction_items_dimension (dimension_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE metric_values (
    id BIGINT NOT NULL AUTO_INCREMENT,
    period_id BIGINT NOT NULL,
    metric_id BIGINT NOT NULL,
    dimension_id BIGINT NOT NULL,
    `value` DECIMAL(20, 4) NOT NULL,
    value_status VARCHAR(20) NOT NULL,
    source_document_id BIGINT NULL,
    source_page INT NULL,
    comments TEXT NULL,
    extraction_item_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_metric_values PRIMARY KEY (id),
    CONSTRAINT uq_metric_values_period_metric_dimension
        UNIQUE (period_id, metric_id, dimension_id),
    CONSTRAINT fk_metric_values_period
        FOREIGN KEY (period_id) REFERENCES reporting_periods (id),
    CONSTRAINT fk_metric_values_metric
        FOREIGN KEY (metric_id) REFERENCES metrics (id),
    CONSTRAINT fk_metric_values_dimension
        FOREIGN KEY (dimension_id) REFERENCES dimensions (id),
    CONSTRAINT fk_metric_values_source_document
        FOREIGN KEY (source_document_id) REFERENCES source_documents (id),
    CONSTRAINT fk_metric_values_extraction_item
        FOREIGN KEY (extraction_item_id) REFERENCES extraction_items (id) ON DELETE SET NULL,
    CONSTRAINT chk_metric_values_status
        CHECK (value_status IN ('REPORTED', 'CALCULATED', 'ESTIMATED')),
    CONSTRAINT chk_metric_values_page
        CHECK (source_page IS NULL OR source_page > 0),
    CONSTRAINT chk_metric_values_context
        CHECK (
            (value_status = 'REPORTED' AND source_document_id IS NOT NULL)
            OR (value_status = 'CALCULATED' AND comments IS NOT NULL AND CHAR_LENGTH(TRIM(comments)) > 0)
            OR (value_status = 'ESTIMATED' AND comments IS NOT NULL AND CHAR_LENGTH(TRIM(comments)) > 0)
        ),
    INDEX idx_metric_values_source_document (source_document_id),
    INDEX idx_metric_values_extraction_item (extraction_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE strategic_targets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metric_id BIGINT NOT NULL,
    dimension_id BIGINT NOT NULL,
    label VARCHAR(150) NOT NULL,
    `operator` VARCHAR(30) NOT NULL,
    target_value DECIMAL(20, 4) NOT NULL,
    target_period_id BIGINT NOT NULL,
    source_document_id BIGINT NULL,
    source_page INT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_strategic_targets PRIMARY KEY (id),
    CONSTRAINT uq_strategic_targets_metric_dimension_period
        UNIQUE (metric_id, dimension_id, target_period_id),
    CONSTRAINT fk_strategic_targets_metric
        FOREIGN KEY (metric_id) REFERENCES metrics (id),
    CONSTRAINT fk_strategic_targets_dimension
        FOREIGN KEY (dimension_id) REFERENCES dimensions (id),
    CONSTRAINT fk_strategic_targets_period
        FOREIGN KEY (target_period_id) REFERENCES reporting_periods (id),
    CONSTRAINT fk_strategic_targets_source_document
        FOREIGN KEY (source_document_id) REFERENCES source_documents (id),
    CONSTRAINT chk_strategic_targets_operator
        CHECK (`operator` IN ('GREATER_THAN_OR_EQUAL', 'LESS_THAN')),
    CONSTRAINT chk_strategic_targets_page
        CHECK (source_page IS NULL OR source_page > 0),
    INDEX idx_strategic_targets_source_document (source_document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
