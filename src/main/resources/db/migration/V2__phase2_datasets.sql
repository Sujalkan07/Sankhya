-- Sankhya Phase 2: Datasets, Ingestion & Supermarket Domain Schema Migration

CREATE TABLE IF NOT EXISTS datasets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    dataset_type VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    record_count BIGINT NOT NULL DEFAULT 0,
    last_imported_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_workspace_dataset_type UNIQUE (workspace_id, dataset_type),
    CONSTRAINT fk_datasets_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dataset_imports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT,
    workspace_id BIGINT NOT NULL,
    dataset_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    uploaded_by_id BIGINT,
    status VARCHAR(40) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    valid_rows INT NOT NULL DEFAULT 0,
    invalid_rows INT NOT NULL DEFAULT 0,
    duplicate_rows INT NOT NULL DEFAULT 0,
    error_summary TEXT,
    column_mapping_json TEXT,
    is_synthetic BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    CONSTRAINT fk_imports_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_imports_dataset FOREIGN KEY (dataset_id) REFERENCES datasets(id) ON DELETE SET NULL,
    CONSTRAINT fk_imports_user FOREIGN KEY (uploaded_by_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS import_errors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_import_id BIGINT NOT NULL,
    row_number INT NOT NULL,
    column_name VARCHAR(100),
    original_value VARCHAR(500),
    error_code VARCHAR(50) NOT NULL,
    error_message VARCHAR(500) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    suggested_correction VARCHAR(500),
    CONSTRAINT fk_errors_import FOREIGN KEY (dataset_import_id) REFERENCES dataset_imports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    subcategory VARCHAR(100),
    brand VARCHAR(100),
    unit_cost DECIMAL(12, 2) NOT NULL,
    selling_price DECIMAL(12, 2) NOT NULL,
    supplier VARCHAR(150),
    expiry_days INT,
    dataset_import_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_workspace_product_id UNIQUE (workspace_id, product_id),
    CONSTRAINT fk_products_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_products_import FOREIGN KEY (dataset_import_id) REFERENCES dataset_imports(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    customer_name VARCHAR(150),
    gender VARCHAR(20),
    age INT,
    city VARCHAR(100),
    registration_date DATE,
    customer_segment VARCHAR(50),
    dataset_import_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_workspace_customer_id UNIQUE (workspace_id, customer_id),
    CONSTRAINT fk_customers_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_customers_import FOREIGN KEY (dataset_import_id) REFERENCES dataset_imports(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    transaction_id VARCHAR(100) NOT NULL,
    transaction_date DATETIME NOT NULL,
    customer_id VARCHAR(50),
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    discount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    revenue DECIMAL(12, 2) NOT NULL,
    payment_method VARCHAR(50),
    store_id VARCHAR(50),
    dataset_import_id BIGINT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_sales_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_sales_import FOREIGN KEY (dataset_import_id) REFERENCES dataset_imports(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    inventory_id VARCHAR(100) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    snapshot_date DATE NOT NULL,
    opening_stock INT NOT NULL,
    received_stock INT NOT NULL DEFAULT 0,
    sold_quantity INT NOT NULL DEFAULT 0,
    closing_stock INT NOT NULL,
    reorder_level INT DEFAULT 0,
    warehouse_or_store VARCHAR(100),
    dataset_import_id BIGINT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_inventory_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_import FOREIGN KEY (dataset_import_id) REFERENCES dataset_imports(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
