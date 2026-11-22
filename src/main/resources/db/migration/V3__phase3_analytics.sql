-- Sankhya Phase 3: Analytics Engine & Business Recommendations Migration

CREATE TABLE IF NOT EXISTS business_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    priority INT NOT NULL DEFAULT 3,
    supporting_metric VARCHAR(100),
    observed_value VARCHAR(100),
    comparison_value VARCHAR(100),
    recommended_action TEXT NOT NULL,
    rule_identifier VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    generated_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_recommendations_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    INDEX idx_rec_workspace (workspace_id),
    INDEX idx_rec_category (category),
    INDEX idx_rec_status (status),
    INDEX idx_rec_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optimization indexes for analytical aggregations on sales and inventory
CREATE INDEX IF NOT EXISTS idx_sales_composite_analytics ON sales_transactions (workspace_id, transaction_date, product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_composite_analytics ON inventory_snapshots (workspace_id, snapshot_date, product_id);
