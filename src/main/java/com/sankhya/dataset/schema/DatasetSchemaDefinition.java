package com.sankhya.dataset.schema;

import com.sankhya.dataset.enums.DatasetType;

import java.util.*;

public class DatasetSchemaDefinition {

    private static final Map<DatasetType, List<CanonicalField>> SCHEMAS = new EnumMap<>(DatasetType.class);

    static {
        // 0. Unified Sales & Analytics Mart (Lean Upload Edition - 22 Columns)
        SCHEMAS.put(DatasetType.UNIFIED_MART, List.of(
                new CanonicalField("transaction_id", "Transaction ID", "STRING", true, "Receipt / bill number",
                        List.of("tx_id", "bill_no", "invoice_no", "order_id", "bill_id", "receipt_id")),
                new CanonicalField("transaction_date", "Transaction Date", "DATETIME", true, "Date and timestamp of purchase",
                        List.of("tx_date", "bill_date", "purchase_date", "date", "timestamp", "datetime")),
                new CanonicalField("customer_id", "Customer ID", "STRING", false, "Customer identification code",
                        List.of("cust_id", "client_id", "customer_code", "buyer_id")),
                new CanonicalField("customer_name", "Customer Name", "STRING", false, "Customer full name",
                        List.of("cust_name", "name", "client_name")),
                new CanonicalField("city", "Customer City", "STRING", false, "Customer residential city",
                        List.of("location", "town", "region")),
                new CanonicalField("customer_segment", "Customer Segment", "STRING", false, "Loyalty tier or segment",
                        List.of("segment", "loyalty_tier", "tier")),
                new CanonicalField("product_id", "Product ID / SKU", "STRING", true, "Product code or SKU",
                        List.of("sku", "item_id", "product_code", "barcode")),
                new CanonicalField("product_name", "Product Name", "STRING", true, "Display name of product",
                        List.of("item_name", "product_title", "title", "description")),
                new CanonicalField("category", "Product Category", "STRING", true, "Main retail department",
                        List.of("dept", "department", "main_category")),
                new CanonicalField("subcategory", "Subcategory", "STRING", false, "Secondary category classification",
                        List.of("sub_category", "sub_cat", "minor_category")),
                new CanonicalField("brand", "Brand", "STRING", false, "Brand or manufacturer name",
                        List.of("company", "manufacturer", "make")),
                new CanonicalField("unit_cost", "Unit Cost", "DECIMAL", true, "Wholesale acquisition cost in INR",
                        List.of("cost", "purchase_cost", "cost_price", "cp")),
                new CanonicalField("selling_price", "Selling Price", "DECIMAL", true, "Catalog retail price in INR",
                        List.of("mrp", "list_price", "retail_price", "sp")),
                new CanonicalField("quantity", "Quantity", "INTEGER", true, "Units purchased (>0)",
                        List.of("qty", "units", "items_count", "volume")),
                new CanonicalField("unit_price", "Unit Price", "DECIMAL", true, "Actual selling price per unit in INR",
                        List.of("price", "rate", "unit_rate")),
                new CanonicalField("discount", "Discount", "DECIMAL", false, "Line item discount in INR",
                        List.of("disc", "discount_amount", "rebate")),
                new CanonicalField("payment_method", "Payment Method", "STRING", false, "Tender type (Cash, Card, UPI)",
                        List.of("payment_mode", "tender_type", "mode")),
                new CanonicalField("cashier_name", "Cashier Name", "STRING", false, "Cashier or terminal operator name",
                        List.of("cashier", "operator", "biller")),
                new CanonicalField("latest_inventory_snapshot_date", "Inventory Date", "DATE", false, "Latest stock snapshot date",
                        List.of("snapshot_date", "inv_date", "stock_date")),
                new CanonicalField("current_closing_stock", "Current Stock", "INTEGER", false, "Latest closing stock quantity",
                        List.of("closing_stock", "stock", "on_hand", "quantity_on_hand")),
                new CanonicalField("current_reorder_level", "Reorder Level", "INTEGER", false, "Minimum threshold for restocking",
                        List.of("reorder_level", "safety_stock", "min_stock")),
                new CanonicalField("current_inventory_status", "Inventory Status", "STRING", false, "Current stock status label",
                        List.of("inventory_status", "stock_status", "health_status"))
        ));

        // 1. Sales Transactions
        SCHEMAS.put(DatasetType.SALES_TRANSACTIONS, List.of(
                new CanonicalField("transaction_id", "Transaction ID", "STRING", true, "Unique transaction/receipt number",
                        List.of("tx_id", "bill_no", "invoice_no", "receipt_no", "order_id", "bill_id", "invoice")),
                new CanonicalField("transaction_date", "Transaction Date", "DATETIME", true, "Date and time of purchase",
                        List.of("tx_date", "bill_date", "purchase_date", "date", "timestamp", "datetime", "invoice_date", "transaction_timestamp")),
                new CanonicalField("product_id", "Product ID / SKU", "STRING", true, "Product code or barcode",
                        List.of("sku", "product_code", "item_id", "barcode", "item_code", "prod_id")),
                new CanonicalField("quantity", "Quantity", "INTEGER", true, "Units purchased (>0)",
                        List.of("qty", "units", "items_count", "volume", "count")),
                new CanonicalField("unit_price", "Unit Selling Price", "DECIMAL", true, "Selling price per unit in INR",
                        List.of("price", "selling_price", "mrp", "unit_rate", "rate", "sp")),
                new CanonicalField("customer_id", "Customer ID", "STRING", false, "Customer identification or blank for anonymous",
                        List.of("cust_id", "client_id", "customer_code", "buyer_id", "customer")),
                new CanonicalField("discount", "Discount", "DECIMAL", false, "Line item discount in INR",
                        List.of("disc", "discount_amount", "rebate", "offer_disc")),
                new CanonicalField("payment_method", "Payment Method", "STRING", false, "Tender type (UPI, Cash, Card)",
                        List.of("payment_mode", "tender_type", "pay_type", "mode", "payment")),
                new CanonicalField("store_id", "Store ID", "STRING", false, "Store location or register ID",
                        List.of("store", "branch_id", "outlet_id", "pos_store", "store_code", "store_location")),
                new CanonicalField("cashier_name", "Cashier Name", "STRING", false, "Cashier / staff name",
                        List.of("cashier", "operator", "biller")),
                new CanonicalField("status", "Status", "STRING", false, "Transaction status",
                        List.of("order_status", "tx_status"))
        ));

        // 2. Customer Registry
        SCHEMAS.put(DatasetType.CUSTOMER_REGISTRY, List.of(
                new CanonicalField("customer_id", "Customer ID", "STRING", true, "Unique customer identifier",
                        List.of("cust_id", "client_id", "customer_code", "account_no", "id")),
                new CanonicalField("customer_name", "Customer Name", "STRING", false, "Full customer name",
                        List.of("name", "full_name", "customer", "client_name")),
                new CanonicalField("email", "Email", "STRING", false, "Email address",
                        List.of("email_address", "mail")),
                new CanonicalField("phone", "Phone", "STRING", false, "Phone number",
                        List.of("mobile", "contact", "phone_number")),
                new CanonicalField("gender", "Gender", "STRING", false, "Customer gender",
                        List.of("sex")),
                new CanonicalField("age", "Age", "INTEGER", false, "Customer age in years",
                        List.of("customer_age", "dob_age")),
                new CanonicalField("city", "City", "STRING", false, "Customer city of residence",
                        List.of("location", "town", "address_city", "region")),
                new CanonicalField("registration_date", "Registration Date", "DATE", false, "Account registration date",
                        List.of("reg_date", "joined_date", "created_date", "signup_date", "member_since")),
                new CanonicalField("customer_segment", "Customer Segment", "STRING", false, "Customer RFM tier",
                        List.of("segment", "loyalty_tier", "tier", "category", "group"))
        ));

        // 3. Product Catalog
        SCHEMAS.put(DatasetType.PRODUCT_CATALOG, List.of(
                new CanonicalField("product_id", "Product ID / SKU", "STRING", true, "Unique product SKU / barcode",
                        List.of("sku", "product_code", "item_id", "barcode", "code", "prod_id")),
                new CanonicalField("product_name", "Product Name", "STRING", true, "Display name of product",
                        List.of("name", "item_name", "title", "description")),
                new CanonicalField("category", "Main Category", "STRING", true, "Primary retail department",
                        List.of("dept", "department", "main_category", "prod_category")),
                new CanonicalField("unit_cost", "Unit Cost", "DECIMAL", true, "Wholesale acquisition cost in INR",
                        List.of("cost", "cost_price", "purchase_cost", "landing_cost", "cp")),
                new CanonicalField("selling_price", "Selling Price", "DECIMAL", true, "Shelf retail price / MRP in INR",
                        List.of("price", "mrp", "sale_price", "sp", "retail_price")),
                new CanonicalField("subcategory", "Subcategory", "STRING", false, "Secondary category",
                        List.of("sub_cat", "sub_category", "minor_category")),
                new CanonicalField("brand", "Brand", "STRING", false, "Manufacturer / brand name",
                        List.of("company", "manufacturer", "make")),
                new CanonicalField("unit_of_measure", "Unit of Measure", "STRING", false, "UOM (Units, Litres, Kg)",
                        List.of("uom", "unit", "measure")),
                new CanonicalField("is_active", "Is Active", "BOOLEAN", false, "Active catalog status",
                        List.of("active", "status", "enabled")),
                new CanonicalField("supplier", "Supplier", "STRING", false, "Vendor / supplier name",
                        List.of("vendor", "distributor", "supplier_name")),
                new CanonicalField("expiry_days", "Shelf Life (Days)", "INTEGER", false, "Perishable shelf life days",
                        List.of("shelf_life", "shelf_life_days", "expiry", "best_before_days"))
        ));

        // 4. Inventory Stock Levels
        SCHEMAS.put(DatasetType.INVENTORY_STOCK, List.of(
                new CanonicalField("inventory_id", "Inventory Record ID", "STRING", true, "Unique snapshot entry ID",
                        List.of("inv_id", "record_id", "batch_id", "snapshot_id", "id")),
                new CanonicalField("product_id", "Product ID / SKU", "STRING", true, "Product SKU / code",
                        List.of("sku", "product_code", "item_id", "barcode")),
                new CanonicalField("snapshot_date", "Observation Date", "DATE", true, "Date of stock observation",
                        List.of("date", "obs_date", "as_of_date", "stock_date")),
                new CanonicalField("opening_stock", "Opening Stock", "INTEGER", true, "Units on hand at start of period",
                        List.of("opening_qty", "opening_balance", "start_stock", "open_stock")),
                new CanonicalField("closing_stock", "Closing Stock", "INTEGER", true, "Units on hand at end of period",
                        List.of("closing_qty", "end_stock", "balance_stock", "current_stock")),
                new CanonicalField("received_stock", "Received Stock", "INTEGER", false, "Inward units received",
                        List.of("received_qty", "inward_stock", "stock_in", "received", "stock_received")),
                new CanonicalField("sold_quantity", "Sold Quantity", "INTEGER", false, "Outward units sold",
                        List.of("sold_qty", "sales_quantity", "stock_out", "outward_stock", "stock_sold")),
                new CanonicalField("unit_cost", "Unit Cost", "DECIMAL", false, "Valuation cost per unit",
                        List.of("cost", "unit_price", "valuation_cost")),
                new CanonicalField("reorder_level", "Reorder Level", "INTEGER", false, "Safety replenishment threshold",
                        List.of("reorder_point", "min_stock", "threshold", "buffer_level")),
                new CanonicalField("warehouse_or_store", "Location", "STRING", false, "Warehouse bay or store code",
                        List.of("location", "warehouse", "store", "bay_location", "warehouse_location"))
        ));
    }

    public static List<CanonicalField> getFields(DatasetType datasetType) {
        return SCHEMAS.getOrDefault(datasetType, Collections.emptyList());
    }

    public static List<CanonicalField> getRequiredFields(DatasetType datasetType) {
        return getFields(datasetType).stream()
                .filter(CanonicalField::isRequired)
                .toList();
    }

    public static Map<String, String> autoDetectMappings(DatasetType datasetType, List<String> fileHeaders) {
        Map<String, String> result = new LinkedHashMap<>();
        List<CanonicalField> canonicalFields = getFields(datasetType);

        for (String header : fileHeaders) {
            String bestMatch = null;
            for (CanonicalField field : canonicalFields) {
                if (field.matches(header)) {
                    bestMatch = field.getName();
                    break;
                }
            }
            if (bestMatch != null) {
                result.put(header, bestMatch);
            }
        }
        return result;
    }

    public static Map<String, String> detectMapping(DatasetType datasetType, List<String> fileHeaders) {
        return autoDetectMappings(datasetType, fileHeaders);
    }
}
