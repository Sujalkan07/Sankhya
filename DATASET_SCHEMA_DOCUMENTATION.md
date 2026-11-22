# SANKHYA — DATASET SCHEMA DOCUMENTATION
## Canonical Supermarket Retail Schemas, Types, Constraints & Business Rules

This document specifies the canonical schemas, constraints, field mapping aliases, and validation business rules for the four core dataset categories supported by **Sankhya**.

---

## 1. Overview & General Constraints

- **Multi-Tenant Isolation:** Every record in all four categories is scoped by `workspace_id`. Cross-workspace queries are strictly isolated.
- **Character Encoding:** Files must be encoded in UTF-8 (BOM or without BOM).
- **Date Formats Supported:**
  - Date & Time: `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd HH:mm`, `dd/MM/yyyy HH:mm:ss`, `dd-MM-yyyy HH:mm:ss`, `ISO-8601`.
  - Date Only: `yyyy-MM-dd`, `dd/MM/yyyy`, `dd-MM-yyyy`, `yyyy/MM/dd`.
- **Numeric Normalization:** Currency symbols (`₹`, `$`) and commas (e.g. `1,250.00`) are automatically stripped and parsed.

---

## 2. Sales Transactions (`SALES_TRANSACTIONS`)

Records individual basket line-items from store POS systems. Multiple product line items can share the same `transaction_id`.

### 2.1 Schema Definition

| Field Name | Type | Required | Description | Recognized Header Aliases |
| :--- | :--- | :--- | :--- | :--- |
| `transaction_id` | String (100) | **Yes** | Unique transaction receipt identifier | `tx_id`, `bill_no`, `invoice_no`, `receipt_no`, `order_id`, `bill_id` |
| `transaction_date`| DateTime | **Yes** | Timestamp of purchase | `tx_date`, `bill_date`, `purchase_date`, `date`, `timestamp`, `datetime`, `transaction_timestamp` |
| `product_id` | String (50) | **Yes** | Product SKU or barcode | `sku`, `product_code`, `item_id`, `barcode`, `item_code`, `prod_id` |
| `quantity` | Integer | **Yes** | Units purchased ($> 0$) | `qty`, `units`, `items_count`, `volume`, `count` |
| `unit_price` | Decimal (12,2)| **Yes** | Selling price per unit in INR ($\ge 0$) | `price`, `selling_price`, `mrp`, `unit_rate`, `rate`, `sp` |
| `customer_id` | String (50) | No | Customer loyalty ID or blank | `cust_id`, `client_id`, `customer_code`, `buyer_id`, `customer` |
| `discount` | Decimal (12,2)| No | Line item discount in INR ($\ge 0$) | `disc`, `discount_amount`, `rebate`, `offer_disc` |
| `revenue` | Decimal (12,2)| Calculated | Net line revenue after discount | `total_amount`, `net_amount`, `line_total` |
| `payment_method` | String (50) | No | Tender type (UPI, Cash, Card, Net Banking)| `payment_mode`, `tender_type`, `pay_type`, `mode`, `payment` |
| `store_id` | String (100) | No | Store branch or register identifier | `store`, `branch_id`, `outlet_id`, `pos_store`, `store_code`, `store_location` |
| `cashier_name` | String (100) | No | Cashier or operator name | `cashier`, `operator`, `biller` |
| `status` | String (50) | No | Transaction status (`COMPLETED`, etc.) | `order_status`, `tx_status` |

### 2.2 Business Rules & Validations
1. **Quantity:** Must be an integer strictly greater than zero ($> 0$).
2. **Unit Price:** Must be numeric and greater than or equal to zero ($\ge 0$).
3. **Discount:** Must not exceed gross value ($\text{discount} \le \text{quantity} \times \text{unit\_price}$). If omitted, defaults to $0.00$.
4. **Revenue Formula:** Net revenue is calculated as $\text{revenue} = (\text{quantity} \times \text{unit\_price}) - \text{discount}$.
5. **Referential Integrity:** If the workspace catalog contains products, `product_id` is validated against known SKUs. Missing SKUs trigger a warning.
6. **Payment Tender Normalization:** Automatically mapped to canonical values (`UPI`, `Cash`, `Card`, `Net Banking`).

### 2.3 Sample CSV
```csv
transaction_id,transaction_date,customer_id,product_id,quantity,unit_price,discount,payment_method,store_id
TXN_10001,2024-01-15 10:30:00,CUST_0001,PROD_001,2,74.00,0.00,UPI,Store_Mumbai_01
TXN_10001,2024-01-15 10:30:00,CUST_0001,PROD_011,1,56.00,0.00,UPI,Store_Mumbai_01
TXN_10002,2024-01-15 11:15:00,CUST_0002,PROD_005,1,250.00,10.00,Card,Store_Mumbai_01
```

---

## 3. Product Catalog (`PRODUCT_CATALOG`)

Master retail item definitions including categorization, acquisition cost, and retail pricing.

### 3.1 Schema Definition

| Field Name | Type | Required | Description | Recognized Header Aliases |
| :--- | :--- | :--- | :--- | :--- |
| `product_id` | String (50) | **Yes** | Unique SKU code or barcode | `sku`, `product_code`, `item_id`, `barcode`, `code`, `prod_id` |
| `product_name` | String (200)| **Yes** | Product display name | `name`, `item_name`, `title`, `description` |
| `category` | String (100)| **Yes** | Primary supermarket aisle/department | `dept`, `department`, `main_category`, `prod_category` |
| `unit_cost` | Decimal (12,2)| **Yes** | Wholesale acquisition cost in INR ($\ge 0$)| `cost`, `cost_price`, `purchase_cost`, `landing_cost`, `cp` |
| `selling_price`| Decimal (12,2)| **Yes** | Retail shelf price / MRP in INR ($\ge 0$) | `price`, `mrp`, `sale_price`, `sp`, `retail_price` |
| `subcategory` | String (100)| No | Secondary category | `sub_cat`, `sub_category`, `minor_category` |
| `brand` | String (100)| No | Manufacturer brand name | `company`, `manufacturer`, `make` |
| `unit_of_measure`| String (50) | No | Units, Litres, Kilograms | `uom`, `unit`, `measure` |
| `is_active` | Boolean | No | Catalog availability flag (Default `true`) | `active`, `status`, `enabled` |
| `supplier` | String (150)| No | Vendor / distributor name | `vendor`, `distributor`, `supplier_name` |
| `expiry_days` | Integer | No | Perishable shelf life days | `shelf_life`, `shelf_life_days`, `expiry`, `best_before_days` |

### 3.2 Business Rules & Validations
1. **Unique SKU:** `(workspace_id, product_id)` must be unique. Duplicate SKUs within a single file trigger critical errors. Existing database SKUs are updated with latest attributes.
2. **Cost & Price Bounds:** Both `unit_cost` and `selling_price` must be $\ge 0$.
3. **Margin Warning:** If $\text{selling\_price} < \text{unit\_cost}$, a warning is flagged for negative gross margin.
4. **Shelf Life Days:** If supplied, must be an integer $\ge 0$.

### 3.3 Sample CSV
```csv
product_id,product_name,category,subcategory,brand,unit_cost,selling_price,unit_of_measure,expiry_days
PROD_001,Amul Taaza Toned Milk 1L,Dairy,Milk & Cream,Amul,66.00,74.00,Litres,10
PROD_005,Aashirvaad Shudh Chakki Atta 5kg,Staples & Grains,Flours,ITC,215.00,250.00,Kilograms,180
PROD_006,Tata Salt Vacuum Evaporated 1kg,Staples & Grains,Salt & Sugar,Tata Consumer,22.00,28.00,Kilograms,720
```

---

## 4. Customer Registry (`CUSTOMER_REGISTRY`)

Customer demographic profiles and loyalty program memberships.

### 4.1 Schema Definition

| Field Name | Type | Required | Description | Recognized Header Aliases |
| :--- | :--- | :--- | :--- | :--- |
| `customer_id` | String (50) | **Yes** | Unique customer identifier | `cust_id`, `client_id`, `customer_code`, `account_no`, `id` |
| `customer_name`| String (150)| No | Full customer name | `name`, `full_name`, `customer`, `client_name` |
| `email` | String (150)| No | Customer email address | `email_address`, `mail` |
| `phone` | String (50) | No | Contact phone number | `mobile`, `contact`, `phone_number` |
| `gender` | String (20) | No | Gender (Male, Female, Other) | `sex` |
| `age` | Integer | No | Age in years (10 to 115) | `customer_age`, `dob_age` |
| `city` | String (100)| No | City of residence | `location`, `town`, `address_city`, `region` |
| `registration_date`| Date | No | Signup date ($\le \text{today}$) | `reg_date`, `joined_date`, `created_date`, `signup_date`, `member_since` |
| `customer_segment` | String (50) | No | Loyalty segment (REGULAR, VIP, OCCASIONAL, NEW)| `segment`, `loyalty_tier`, `tier`, `category`, `group` |

### 4.2 Business Rules & Validations
1. **Uniqueness:** `(workspace_id, customer_id)` must be unique. Duplicate customer IDs in file trigger critical validation failure.
2. **Age Range:** If supplied, age must be between 10 and 115.
3. **Registration Date:** Must not be in the future ($\le \text{today}$).

### 4.3 Sample CSV
```csv
customer_id,customer_name,email,phone,gender,age,city,registration_date,customer_segment
CUST_0001,Rajesh Sharma,rajesh.sharma@example.in,+91 98200 12345,Male,38,Mumbai,2023-01-15,VIP
CUST_0002,Priya Patel,priya.patel@example.in,+91 98200 23456,Female,29,Ahmedabad,2023-03-22,REGULAR
CUST_0003,Amit Verma,amit.verma@example.in,+91 98200 34567,Male,45,Delhi NCR,2023-06-10,OCCASIONAL
```

---

## 5. Inventory Stock Levels (`INVENTORY_STOCK`)

Periodic snapshots of warehouse and shelf stock positions verifying physical inventory conservation.

### 5.1 Schema Definition

| Field Name | Type | Required | Description | Recognized Header Aliases |
| :--- | :--- | :--- | :--- | :--- |
| `inventory_id` | String (100)| **Yes** | Unique snapshot entry code | `inv_id`, `record_id`, `batch_id`, `snapshot_id`, `id` |
| `product_id` | String (50) | **Yes** | Product SKU / code | `sku`, `product_code`, `item_id`, `barcode` |
| `snapshot_date`| Date | **Yes** | Stock observation date | `date`, `obs_date`, `as_of_date`, `stock_date` |
| `opening_stock`| Integer | **Yes** | Quantity on hand at start of period ($\ge 0$)| `opening_qty`, `opening_balance`, `start_stock`, `open_stock` |
| `closing_stock`| Integer | **Yes** | Quantity on hand at end of period ($\ge 0$)| `closing_qty`, `end_stock`, `balance_stock`, `current_stock` |
| `received_stock`| Integer | No | Inward units received during period ($\ge 0$)| `received_qty`, `inward_stock`, `stock_in`, `received`, `stock_received` |
| `sold_quantity` | Integer | No | Outward units sold during period ($\ge 0$) | `sold_qty`, `sales_quantity`, `stock_out`, `outward_stock`, `stock_sold` |
| `unit_cost` | Decimal (12,2)| No | Valuation cost per unit ($\ge 0$) | `cost`, `unit_price`, `valuation_cost` |
| `reorder_level`| Integer | No | Safety stock threshold ($\ge 0$) | `reorder_point`, `min_stock`, `threshold`, `buffer_level` |
| `warehouse_or_store`| String (100)| No | Storage location or warehouse bay | `location`, `warehouse`, `store`, `bay_location`, `warehouse_location` |

### 5.2 Business Rules & Validations
1. **Mandatory Quantities:** `opening_stock` and `closing_stock` must be non-negative integers ($\ge 0$).
2. **Stock Movement Formula Consistency:**
   $$\text{opening\_stock} + \text{received\_stock} - \text{sold\_quantity} = \text{closing\_stock}$$
   Discrepancies trigger a consistency warning identifying the exact arithmetic difference.
3. **SKU Referential Integrity:** Verified against workspace product catalog.
4. **Snapshot Uniqueness:** Combination of `(snapshot_date, product_id)` in the same upload is checked for duplicates.

### 5.3 Sample CSV
```csv
inventory_id,snapshot_date,product_id,opening_stock,received_stock,sold_quantity,closing_stock,reorder_level,warehouse_or_store
INV_2024-01-15_PROD_001,2024-01-15,PROD_001,50,30,25,55,20,Section-DAI
INV_2024-01-15_PROD_005,2024-01-15,PROD_005,40,20,15,45,15,Section-STA
INV_2024-01-15_PROD_006,2024-01-15,PROD_006,80,50,30,100,30,Section-STA
```
