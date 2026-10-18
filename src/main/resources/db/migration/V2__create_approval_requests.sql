
CREATE TABLE approval_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    recommended_action VARCHAR(1000) NOT NULL,
    explanation VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    decided_at DATETIME NULL,
    decided_by VARCHAR(100) NULL,

    PRIMARY KEY (id),

    INDEX idx_approval_requests_order_id (order_id),
    INDEX idx_approval_requests_status (status),
    INDEX idx_approval_requests_created_at (created_at)
);
