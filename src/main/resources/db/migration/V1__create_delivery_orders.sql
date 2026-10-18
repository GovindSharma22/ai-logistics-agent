CREATE TABLE delivery_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(50) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    estimated_delivery_at DATETIME(6) NOT NULL,
    actual_delivery_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_orders_order_number UNIQUE (order_number),
    INDEX idx_delivery_status (status),
    INDEX idx_delivery_eta (estimated_delivery_at)
);
