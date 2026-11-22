-- Sankhya Phase 5: Advanced Machine Learning Engine & Gemini AI Integration Migration

CREATE TABLE IF NOT EXISTS ml_models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    algorithm VARCHAR(50) NOT NULL,
    model_type VARCHAR(50) NOT NULL,
    version VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'READY',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_ml_model_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    INDEX idx_ml_model_workspace (workspace_id),
    INDEX idx_ml_model_algo (algorithm),
    INDEX idx_ml_model_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_model_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    duration_ms BIGINT,
    train_record_count INT DEFAULT 0,
    test_record_count INT DEFAULT 0,
    hyperparameters_json TEXT,
    metrics_json TEXT,
    error_message TEXT,
    artifact_path VARCHAR(255),
    executed_by VARCHAR(100),
    CONSTRAINT fk_execution_model FOREIGN KEY (model_id) REFERENCES ml_models(id) ON DELETE CASCADE,
    CONSTRAINT fk_execution_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    INDEX idx_exec_workspace (workspace_id),
    INDEX idx_exec_model (model_id),
    INDEX idx_exec_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_customer_clusters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    execution_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_code VARCHAR(100),
    cluster_id INT NOT NULL,
    cluster_label VARCHAR(100) NOT NULL,
    recency_days DOUBLE NOT NULL,
    frequency DOUBLE NOT NULL,
    monetary DOUBLE NOT NULL,
    average_transaction_value DOUBLE NOT NULL,
    distance_to_centroid DOUBLE,
    assigned_at DATETIME NOT NULL,
    CONSTRAINT fk_cluster_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_cluster_execution FOREIGN KEY (execution_id) REFERENCES ml_model_executions(id) ON DELETE CASCADE,
    INDEX idx_cluster_workspace (workspace_id),
    INDEX idx_cluster_exec (execution_id),
    INDEX idx_cluster_num (cluster_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_association_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    execution_id BIGINT NOT NULL,
    antecedent VARCHAR(500) NOT NULL,
    consequent VARCHAR(500) NOT NULL,
    antecedent_product_id BIGINT,
    consequent_product_id BIGINT,
    support DOUBLE NOT NULL,
    confidence DOUBLE NOT NULL,
    lift DOUBLE NOT NULL,
    conviction DOUBLE,
    rule_type VARCHAR(50) DEFAULT 'CROSS_SELL',
    actionable_recommendation TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_rule_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_rule_execution FOREIGN KEY (execution_id) REFERENCES ml_model_executions(id) ON DELETE CASCADE,
    INDEX idx_rule_workspace (workspace_id),
    INDEX idx_rule_exec (execution_id),
    INDEX idx_rule_lift (lift)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_forecast_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    execution_id BIGINT NOT NULL,
    forecast_date DATE NOT NULL,
    predicted_revenue DOUBLE NOT NULL,
    baseline_revenue DOUBLE,
    lower_bound DOUBLE,
    upper_bound DOUBLE,
    horizon_day INT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_forecast_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_forecast_execution FOREIGN KEY (execution_id) REFERENCES ml_model_executions(id) ON DELETE CASCADE,
    INDEX idx_forecast_workspace (workspace_id),
    INDEX idx_forecast_exec (execution_id),
    INDEX idx_forecast_date (forecast_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_demand_predictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    execution_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(100),
    product_name VARCHAR(255),
    category VARCHAR(100),
    predicted_demand_class VARCHAR(50) NOT NULL,
    confidence DOUBLE NOT NULL,
    recent_daily_velocity DOUBLE,
    predicted_units_next_7d DOUBLE,
    stockout_risk_level VARCHAR(30),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_demand_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_demand_execution FOREIGN KEY (execution_id) REFERENCES ml_model_executions(id) ON DELETE CASCADE,
    INDEX idx_demand_workspace (workspace_id),
    INDEX idx_demand_exec (execution_id),
    INDEX idx_demand_class (predicted_demand_class)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_gemini_insights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    full_narrative MEDIUMTEXT,
    key_findings_json TEXT,
    action_items_json TEXT,
    grounded_metrics_json TEXT,
    gemini_model_version VARCHAR(50),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_ai_insight_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    INDEX idx_ai_insight_workspace (workspace_id),
    INDEX idx_ai_insight_cat (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
