package com.sankhya.dataset.enums;

public enum DatasetType {
    UNIFIED_MART("Unified Sales & Analytics Mart", "Complete single-file retail fact mart containing receipt line items, customer context, product pricing, and latest inventory", "sankhya_unified_mart_template.csv"),
    SALES_TRANSACTIONS("Sales Transactions", "POS transaction logs, line items, and tender types", "sales_transactions_template.csv"),
    CUSTOMER_REGISTRY("Customer Registry", "Customer demographic profiles, loyalty tiers, and registration data", "customer_registry_template.csv"),
    PRODUCT_CATALOG("Product Master Catalog", "SKU master catalog, unit costs, selling prices, and categories", "product_catalog_template.csv"),
    INVENTORY_STOCK("Inventory Stock Levels", "Warehouse and store stock snapshots, opening, and closing quantities", "inventory_stock_template.csv");

    private final String displayName;
    private final String description;
    private final String templateFilename;

    DatasetType(String displayName, String description, String templateFilename) {
        this.displayName = displayName;
        this.description = description;
        this.templateFilename = templateFilename;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getTemplateFilename() {
        return templateFilename;
    }

    public static DatasetType fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (DatasetType type : DatasetType.values()) {
            if (type.name().equalsIgnoreCase(text) || type.displayName.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }
}
