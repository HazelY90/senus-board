-- Create the users table for authentication and administrative account management.

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role
        CHECK (role IN ('MANAGEMENT', 'BOARD', 'EQUITY_INVESTOR', 'CREDIT_PROVIDER', 'ADMIN')),
    CONSTRAINT chk_users_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'DISABLED')),
    INDEX idx_users_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
