ALTER TABLE approval_requests
    ADD COLUMN executed_by VARCHAR(100) NULL,
    ADD COLUMN executed_at DATETIME NULL,
    ADD COLUMN execution_error VARCHAR(1000) NULL;
