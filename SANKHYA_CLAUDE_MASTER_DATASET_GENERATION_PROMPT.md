# CLAUDE MASTER INSTRUCTION PROMPT: SYNTHETIC SUPERMARKET DATASET GENERATION FOR SANKHYA

> **INSTRUCTIONS FOR THE USER:**  
> Copy and paste the entire block below into **Claude** (Claude 3.5 Sonnet / Claude 3 Opus / Claude 3.7 Sonnet). This prompt is fully grounded in the actual codebase, Flyway database migrations, ingestion parsers, analytics math, and Java 21 ML algorithms of **Sankhya**.

---

```markdown
# MASTER PROMPT: GENERATE ENTERPRISE-GRADE SYNTHETIC INDIAN SUPERMARKET DATASET FOR SANKHYA

## 1. ROLE & MISSION OBJECTIVE

Act as an elite **Principal Retail Data Architect**, **Indian Supermarket Domain Specialist**, **Data Engineering Lead**, and **Senior AI/ML Dataset Designer**.

Your mission is to generate a comprehensive, 100% referentially consistent, chronologically realistic, multi-category synthetic supermarket dataset specifically designed for **Sankhya** — a Java 21 / Spring Boot 3.3.5 / MySQL 8+ Supermarket Analytics & Business Intelligence platform.

The dataset must reflect real-world operational patterns of an Indian medium-to-large supermarket (such as a modern format store like DMart, Reliance Smart, or Nature's Basket). It will power executive KPIs, inventory reorder automations, customer RFM quintile segmentation, market-basket association mining (Apriori), SKU velocity classification (Random Forest), forward sales forecasting (Multiple Linear Regression), and Gemini AI executive summaries.

---

## 2. STRICT ARCHITECTURAL & INGESTION CONSTRAINTS (NO SCHEMA INVENTIONS)

Sankhya uses a strict 4-step progressive disclosure ingestion engine:
`Upload (.csv/.xlsx) -> Auto-Detect Header Mapping -> Tabular Preview -> 5D Rule Validation -> Database Ingestion`.

To guarantee automated 100% mapping accuracy and zero import errors, you **MUST** strictly adhere to the following four canonical schemas, table structures, and validation rules extracted directly from Sankhya's production codebase:

### 2.1 File 1: Product Master Catalog (`products.csv`)
- **Dataset Category Type:** `PRODUCT_CATALOG`
- **Database Table:** `products`
- **Primary / Unique Constraint:** `(workspace_id, product_id)` — `product_id` must be globally unique within the file.
- **Canonical CSV Header Line (Exact Case & Names):**
  ```csv
  product_id,product_name,category,subcategory,brand,unit_cost,selling_price,unit_of_measure,is_active,supplier,expiry_days
  ```

| Column Name | SQL Type | Required | Accepted Aliases | Validation Rules & Bounds |
| :--- | :--- | :---: | :--- | :--- |
| `product_id` | `VARCHAR(50)` | **YES** | `sku`, `product_code`, `item_id`, `barcode`, `code`, `prod_id` | Unique SKU string (e.g. `PROD_001`). No nulls or duplicates. |
| `product_name` | `VARCHAR(200)` | **YES** | `name`, `item_name`, `title`, `description` | Full descriptive display name with package size (e.g. `Amul Taaza Toned Milk 1L`). |
| `category` | `VARCHAR(100)` | **YES** | `dept`, `department`, `main_category`, `prod_category` | Primary retail department. |
| `subcategory` | `VARCHAR(100)` | NO | `sub_cat`, `sub_category`, `minor_category` | Granular aisle grouping (e.g. `Milk & Cream`, `Edible Oils`). |
| `brand` | `VARCHAR(100)` | NO | `company`, `manufacturer`, `make` | Established manufacturer (e.g. `Amul`, `ITC`, `Tata Consumer`). |
| `unit_cost` | `DECIMAL(12,2)` | **YES** | `cost`, `cost_price`, `purchase_cost`, `landing_cost`, `cp` | Acquisition wholesale cost in INR. Must be $\ge 0.00$. |
| `selling_price`| `DECIMAL(12,2)` | **YES** | `price`, `mrp`, `sale_price`, `sp`, `retail_price` | Shelf MRP in INR. Must be $\ge 0.00$. Standard retail margin: $\text{selling\_price} > \text{unit\_cost}$. |
| `unit_of_measure`| `VARCHAR(50)` | NO | `uom`, `unit`, `measure` | Standard unit: `Units`, `Litres`, `Kilograms`, `Grams`, `Packs`. |
| `is_active` | `BOOLEAN` | NO | `active`, `status`, `enabled` | `true` or `false` (Default `true`). |
| `supplier` | `VARCHAR(150)` | NO | `vendor`, `distributor`, `supplier_name` | Wholesale distributor name. |
| `expiry_days` | `INTEGER` | NO | `shelf_life`, `shelf_life_days`, `expiry`, `best_before_days` | Perishable shelf life days ($\ge 0$). E.g., `10` for milk, `720` for salt. |

---

### 2.2 File 2: Customer Registry (`customers.csv`)
- **Dataset Category Type:** `CUSTOMER_REGISTRY`
- **Database Table:** `customers`
- **Primary / Unique Constraint:** `(workspace_id, customer_id)` — `customer_id` must be unique.
- **Canonical CSV Header Line (Exact Case & Names):**
  ```csv
  customer_id,customer_name,email,phone,gender,age,city,registration_date,customer_segment
  ```

| Column Name | SQL Type | Required | Accepted Aliases | Validation Rules & Bounds |
| :--- | :--- | :---: | :--- | :--- |
| `customer_id` | `VARCHAR(50)` | **YES** | `cust_id`, `client_id`, `customer_code`, `account_no`, `id` | Unique ID string (e.g. `CUST_0001`). No nulls or duplicates. |
| `customer_name`| `VARCHAR(150)` | NO | `name`, `full_name`, `customer`, `client_name` | Realistic Indian full name (e.g. `Rajesh Sharma`). |
| `email` | `VARCHAR(150)` | NO | `email_address`, `mail` | Synthetic email (e.g. `rajesh.sharma@example.in`). |
| `phone` | `VARCHAR(50)` | NO | `mobile`, `contact`, `phone_number` | Synthetic Indian mobile (e.g. `+91 98200 12345`). |
| `gender` | `VARCHAR(20)` | NO | `sex` | `Male`, `Female`, or `Other`. |
| `age` | `INTEGER` | NO | `customer_age`, `dob_age` | Integer bounded strictly between $10$ and $115$ (realistic: $18 - 72$). |
| `city` | `VARCHAR(100)` | NO | `location`, `town`, `address_city`, `region` | Major Indian commercial hubs (e.g. `Mumbai`, `Bengaluru`, `Delhi NCR`, `Pune`, `Hyderabad`). |
| `registration_date`| `DATE` | NO | `reg_date`, `joined_date`, `created_date`, `signup_date` | `YYYY-MM-DD`. Must NOT be in the future ($\le \text{today}$). Must be $\le$ customer's first purchase date. |
| `customer_segment`| `VARCHAR(50)` | NO | `segment`, `loyalty_tier`, `tier`, `category`, `group` | Initial loyalty tier: `REGULAR`, `VIP`, `OCCASIONAL`, `NEW`. |

---

### 2.3 File 3: Sales Transactions (`sales_transactions.csv`)
- **Dataset Category Type:** `SALES_TRANSACTIONS`
- **Database Table:** `sales_transactions`
- **CRITICAL DATA MODEL DISTINCTION:**  
  Sankhya's `sales_transactions` table operates at **Line-Item Granularity**. There is **NO separate transaction items file**. Multiple product sales line items belong to the same checkout receipt by sharing the **identical `transaction_id`**, `transaction_date`, `customer_id`, `payment_method`, and `store_id`.
- **Canonical CSV Header Line (Exact Case & Names):**
  ```csv
  transaction_id,transaction_date,customer_id,product_id,quantity,unit_price,discount,payment_method,store_id,cashier_name,status
  ```

| Column Name | SQL Type | Required | Accepted Aliases | Validation Rules & Bounds |
| :--- | :--- | :---: | :--- | :--- |
| `transaction_id` | `VARCHAR(100)` | **YES** | `tx_id`, `bill_no`, `invoice_no`, `receipt_no`, `order_id` | Receipt/Bill ID (e.g. `TXN_10001`). Multiple rows can share one ID to represent multi-item baskets! |
| `transaction_date`| `DATETIME` | **YES** | `tx_date`, `bill_date`, `purchase_date`, `timestamp`, `datetime` | Format: `YYYY-MM-DD HH:MM:SS`. Chronologically valid, between 09:00 and 22:00. No future dates. |
| `customer_id` | `VARCHAR(50)` | NO | `cust_id`, `client_id`, `customer_code`, `buyer_id` | Foreign Key referencing `customers.customer_id`. Can be blank/null for anonymous cash shoppers (~10-15%). |
| `product_id` | `VARCHAR(50)` | **YES** | `sku`, `product_code`, `item_id`, `barcode`, `prod_id` | Foreign Key referencing `products.product_id`. Every single SKU must exist in `products.csv`! |
| `quantity` | `INTEGER` | **YES** | `qty`, `units`, `items_count`, `volume`, `count` | Units purchased. Must be an integer strictly greater than zero ($\ge 1$). |
| `unit_price` | `DECIMAL(12,2)` | **YES** | `price`, `selling_price`, `mrp`, `unit_rate`, `rate`, `sp` | Shelf selling price in INR ($\ge 0.00$). Should match the product's catalog `selling_price`. |
| `discount` | `DECIMAL(12,2)` | NO | `disc`, `discount_amount`, `rebate`, `offer_disc` | Promotional discount in INR ($\ge 0.00$). **CRITICAL CHECK:** $\text{discount} \le (\text{quantity} \times \text{unit\_price})$. Defaults to `0.00`. |
| `payment_method` | `VARCHAR(50)` | NO | `payment_mode`, `tender_type`, `pay_type`, `mode` | Normalized values: `UPI`, `Cash`, `Card`, `Net Banking`. |
| `store_id` | `VARCHAR(100)` | NO | `store`, `branch_id`, `outlet_id`, `store_location` | Supermarket outlet identifier (e.g. `SmartMart Supermarket - Main Store`). |
| `cashier_name` | `VARCHAR(100)` | NO | `cashier`, `operator`, `biller` | Staff cashier name (e.g. `Rohan K.`, `Sneha P.`). |
| `status` | `VARCHAR(50)` | NO | `order_status`, `tx_status` | Transaction status: `COMPLETED`. |

> **Application Formula Note:** Revenue is calculated internally by Sankhya as:  
> $\text{revenue} = \max(0, (\text{quantity} \times \text{unit\_price}) - \text{discount})$. Do not add a separate imported `revenue` column.

---

### 2.4 File 4: Inventory Stock Snapshots (`inventory_stock.csv`)
- **Dataset Category Type:** `INVENTORY_STOCK`
- **Database Table:** `inventory_snapshots`
- **Primary / Unique Constraint:** `(workspace_id, snapshot_date, product_id)` — Exactly **one** record per SKU per snapshot date!
- **NON-SUMMATIVE RULE:** Sankhya queries the latest snapshot by `MAX(snapshot_date)` for current stock. It never sums across dates.
- **Canonical CSV Header Line (Exact Case & Names):**
  ```csv
  inventory_id,snapshot_date,product_id,opening_stock,received_stock,sold_quantity,closing_stock,unit_cost,reorder_level,warehouse_or_store
  ```

| Column Name | SQL Type | Required | Accepted Aliases | Validation Rules & Bounds |
| :--- | :--- | :---: | :--- | :--- |
| `inventory_id` | `VARCHAR(100)` | **YES** | `inv_id`, `record_id`, `batch_id`, `snapshot_id`, `id` | Snapshot ID (e.g. `INV_2024-01-15_PROD_001`). Unique per row. |
| `snapshot_date`| `DATE` | **YES** | `date`, `obs_date`, `as_of_date`, `stock_date` | Date of snapshot observation: `YYYY-MM-DD`. Regular periodic intervals (e.g. weekly every Monday). |
| `product_id` | `VARCHAR(50)` | **YES** | `sku`, `product_code`, `item_id`, `barcode` | Foreign Key referencing `products.product_id`. Must exist in catalog. |
| `opening_stock`| `INTEGER` | **YES** | `opening_qty`, `opening_balance`, `start_stock` | On-hand quantity at period start ($\ge 0$). |
| `received_stock`| `INTEGER` | NO | `received_qty`, `inward_stock`, `stock_in`, `stock_received` | Inward batch units received during period ($\ge 0$, default 0). |
| `sold_quantity` | `INTEGER` | NO | `sold_qty`, `sales_quantity`, `stock_out`, `stock_sold` | Outward units sold during period ($\ge 0$, default 0). |
| `closing_stock`| `INTEGER` | **YES** | `closing_qty`, `end_stock`, `balance_stock`, `current_stock` | On-hand quantity at period end ($\ge 0$). |
| `unit_cost` | `DECIMAL(12,2)` | NO | `cost`, `unit_price`, `valuation_cost` | Valuation cost per unit ($\ge 0.00$). Should match product unit cost. |
| `reorder_level`| `INTEGER` | NO | `reorder_point`, `min_stock`, `threshold`, `buffer_level` | Safety replenishment threshold ($\ge 0$). E.g. `15` to `50`. |
| `warehouse_or_store`| `VARCHAR(100)`| NO | `location`, `warehouse`, `store`, `bay_location` | Storage section (e.g. `Section-DAI`, `Section-STA`, `Section-BEV`). |

> **MATHEMATICAL STOCK BALANCE AUDIT (CRITICAL):**  
> Sankhya's validator performs this exact mathematical consistency check:  
> $$\text{opening\_stock} + \text{received\_stock} - \text{sold\_quantity} = \text{closing\_stock}$$  
> Any discrepancy triggers an `INCONSISTENT_CALCULATION` error. Ensure this formula holds **100% true** across every single inventory row! Furthermore, `sold_quantity` in inventory should reflect the actual aggregate sales volume of that product in that week.

---

## 3. DATASET SCALE & TEMPORAL HORIZON REQUIREMENTS

The generated dataset must provide substantial, production-ready depth:

1. **Catalog Scale:**
   - **Products:** Exactly **150 to 200 distinct SKUs** (`PROD_001` through `PROD_0180+`).
   - **Categories:** 10 core supermarket departments (Dairy, Staples & Grains, Packaged Foods, Beverages, Snacks & Namkeen, Spices & Masalas, Household Care, Personal Care, Bakery & Fresh, Fruits & Vegetables).
   - **Customers:** Exactly **600 to 1,000 synthetic customers** (`CUST_0001` through `CUST_0800`).
2. **Temporal Window:**
   - **Coverage:** Minimum **24 Months** to **36 Months** of continuous historical sales (e.g., from `2023-01-01` to `2025-12-31` or `2024-01-01` to `2026-03-31`).
3. **Transaction Volume:**
   - **Sales Transactions (Line Items):** **20,000 to 35,000 line-item rows**.
   - **Distinct Basket Checkouts:** **8,000 to 15,000 distinct `transaction_id` receipts**.
   - Average basket size: **2.2 to 3.5 items per basket** (varying from single-item express trips to 8-item weekly family grocery hauls).
4. **Inventory Volume:**
   - Weekly snapshots across all active SKUs for the entire period (~104 weeks $\times$ 180 SKUs $\approx$ **18,000 snapshot records**).

---

## 4. MATHEMATICAL & ALGORITHMIC GROUNDING (SANKHYA COMPLIANCE)

The synthetic data must NOT be pure white noise or uniform random distributions. It must be engineered to stimulate Sankhya's analytical and machine learning engines:

### 4.1 Module 1: Core Sales & Executive Analytics
- **ATV (Average Transaction Value):** Must range between **₹450.00 and ₹850.00** at the basket level.
- **Payment Method Distribution:** Calibrated strictly to modern urban Indian supermarkets:
  - `UPI`: **55%** (Google Pay, PhonePe, Paytm QR).
  - `Cash`: **25%** (Cash payments, higher on small single-item tickets).
  - `Card`: **18%** (Debit / Credit cards, concentrated on high-value weekend baskets).
  - `Net Banking`: **2%**.
- **Diurnal Hourly Distribution:** POS open between 09:00 and 22:00. Peak surges at:
  - Morning Grocery Peak: 10:00 to 12:30.
  - Evening Rush: 17:30 to 20:30 (representing ~55% of daily transactions).
- **Weekly & Seasonal Cycles:**
  - **Weekend Surge:** Saturday and Sunday experience a **+35% to +45% footfall lift**.
  - **Diwali Festive Surge (October / November):** **+70% to +100% volume surge** with heavy spending on Sweets, Dry Fruits, Ghee, Edible Oils, and Household Cleaning.
  - **Holi Surge (March):** Snacks, Thandai, Beverage spikes.
  - **Summer Beverage Surge (April to June):** Soft drinks, Juices, and Bottled Water double in volume.

### 4.2 Module 2: RFM Customer Segmentation & K-Means Clustering
Sankhya runs a pure Java **K-Means++ ($K=4$)** algorithm using standard $Z$-score normalized vectors $\mathbf{x} = [r, f, m, v]^T$:
- $r$ (Recency in days): $0$ to $365$.
- $f$ (Frequency): count of distinct `transaction_id` checkouts in 365 days.
- $m$ (Monetary): cumulative revenue in INR.
- $v$ (Average Transaction Value): $m / f$.

The customer base must be segmented into distinct behavioral personas that naturally emerge from transaction history:
1. **High-Value Champions (~15%):** Shop weekly ($f \ge 35$), high basket spend ($v \ge \text{₹1,200}$), recent purchase ($r \le 7$ days). Cumulative spend $> \text{₹40,000/yr}$.
2. **Loyal Regulars (~35%):** Bi-weekly shoppers ($f \in [15, 30]$), moderate spend ($v \in [\text{₹500}, \text{₹900}]$), $r \le 18$ days.
3. **Emerging Regulars / New Shoppers (~25%):** Registered recently ($< 90$ days), $f \in [2, 6]$, expanding basket sizes.
4. **At-Risk / Lapsed Occasionals (~25%):** Infrequent buyers ($f \le 3$), high recency ($r > 60$ to $180$ days).
5. **Anonymous Walk-ins:** Ensure **10% to 15%** of transactions have an empty/blank `customer_id` (cashier quick checkout).

### 4.3 Module 3: Apriori Market Basket Association Mining
Sankhya's Apriori engine filters candidate pairs with `minSupport = 0.01` (1%), `minConfidence = 0.20` (20%), and `minLift = 1.05`.  
You must engineer realistic product co-occurrences so that high-affinity pairs repeatedly appear in multi-item baskets:
- **Breakfast / Dairy Combo:** `Amul Taaza Milk` $\Leftrightarrow$ `Britannia Bread` / `Amul Butter` ($\text{Confidence} \approx 45\%, \text{Lift} > 2.5$).
- **Indian Kitchen Staples:** `Aashirvaad Atta` $\Leftrightarrow$ `Fortune Sunflower Oil` $\Leftrightarrow$ `Tata Salt` ($\text{Confidence} \approx 35\%, \text{Lift} > 2.0$).
- **Snacks & Refreshment:** `Lay's Chips` $\Leftrightarrow$ `Thums Up Cold Drink` / `Kurkure` ($\text{Confidence} \approx 40\%, \text{Lift} > 3.0$).
- **Tea Time:** `Tata Tea Gold` $\Leftrightarrow$ `Parle-G Biscuits` / `Good Day Cookies`.
- **Household Cleaning:** `Surf Excel Detergent` $\Leftrightarrow$ `Vim Dishwash Gel` $\Leftrightarrow$ `Harpic Cleaner`.
- **Negative / Independent Controls:** Ensure items like `Harpic Toilet Cleaner` and `Fresh Paneer` or `Toor Dal` and `Toothpaste` have random, near-zero cross-affinity ($\text{Lift} \approx 1.0$ or $\text{Support} < 0.005$).

### 4.4 Module 4: Random Forest Product Demand Classification
Sankhya classifies SKU 90-day velocity into three classes:
- `HIGH` Demand: $\text{dailyVelocity} \ge 4.0\text{ units/day}$.
- `MEDIUM` Demand: $1.0 \le \text{dailyVelocity} < 4.0\text{ units/day}$.
- `LOW` Demand: $\text{dailyVelocity} < 1.0\text{ units/day}$.

Your catalog must distribute SKUs across all three classes:
- **High Velocity (~20% of SKUs):** Daily staples (Milk, Bread, Eggs, Atta, Salt, Maggi).
- **Medium Velocity (~50% of SKUs):** Snacks, Beverages, Cooking Spices, Detergents.
- **Low Velocity (~30% of SKUs):** Premium items, Specialty Sauces, Exotic Spices, Large packaging units.

### 4.5 Module 5: Linear Regression Revenue Forecasting
Sankhya forecasts future store revenue using autoregressive features:
$\mathbf{x}_t = [1, t, \text{dow}_t, \text{isWeekend}_t, y_{t-1}, y_{t-7}, \bar{y}_{t-7..t-1}]^T$.  
The daily revenue time series must demonstrate:
- A gradual baseline year-over-year organic growth rate (~6% to 10% annual trend).
- Consistent 7-day cyclicality ($y_{t}$ strongly correlated with $y_{t-7}$).
- No unexplainable negative revenue spikes or zero-sales days (supermarket stays open 365 days).

---

## 5. EMBEDDED BUSINESS SCENARIOS FOR SANKHYA'S RECOMMENDATION ENGINE

Sankhya contains a deterministic heuristic rule engine (`BusinessRecommendationService`). To showcase the platform in executive demonstrations, your synthetic dataset **MUST intentionally seed the following operational scenarios**:

1. **Stockout Risk Replenishment Alert (`RULE_STOCKOUT_RISK`):**
   - Seed **3 to 5 fast-moving SKUs** (e.g. `Amul Butter 500g`, `Maggi 280g`) where the latest inventory closing stock drops to or below the reorder point ($\text{closing\_stock} \le \text{reorder\_level}$), with at least one item reaching $\text{closing\_stock} = 0$.
2. **Excess Inventory Holding Cost Alert (`RULE_OVERSTOCK_HOLDING_COST`):**
   - Seed **3 to 4 slow-moving SKUs** (e.g. premium specialty olive oil or slow-moving cosmetics) where the latest closing stock exceeds three times the reorder level ($\text{closing\_stock} > 3 \times \text{reorder\_level}$), tieing up working capital.
3. **High-Volume Low-Margin Optimization (`RULE_TOP_PRODUCT_MARGIN_OPTIMIZATION`):**
   - Seed **2 to 3 high-volume staples** (e.g. Sugar, Specific Brand Rice) where sales volume $\ge 20$ units, but gross margin percentage is razor-thin ($< 15.0\%$), triggering a margin optimization recommendation.
4. **VIP Customer Churn Risk Alert (`RULE_CUSTOMER_CHURN_RISK`):**
   - Seed **4 to 6 high-value customers** who previously spent $> \text{₹15,000}$ with frequency $\ge 5$, but whose last purchase occurred $> 35$ days ago ($r > 30$ days), triggering a win-back campaign alert.
5. **BCG Matrix Diversification:**
   - Products must populate all 4 quadrants: **Stars** (High Vol, High Margin), **Cash Cows** (High Vol, Low Margin), **Opportunities** (Low Vol, High Margin), and **Underperformers** (Low Vol, Low Margin).

---

## 6. REQUIRED OUTPUT DELIVERABLES & GENERATION SCRIPT

Because generating 25,000+ relational rows exceeds standard LLM single-response chat tokens, **you must deliver**:

### Deliverable 1: Complete, Self-Contained Python Dataset Generator (`generate_sankhya_dataset.py`)
Provide a production-ready, bulletproof, highly optimized Python script that I can execute locally with:
`python generate_sankhya_dataset.py`

**Technical Script Requirements:**
1. **Zero External Third-Party Dependencies:** Use **ONLY** standard library modules (`csv`, `datetime`, `random`, `math`, `os`, `sys`, `collections`). No `pandas`, no `numpy`, no `faker` required.
2. **Deterministic Reproducibility:** Hardcode `random.seed(42)` at the start so identical data is produced on every run.
3. **Target File Outputs:** Generates the exact 4 CSV files in a `./sankhya_dataset/` folder:
   - `products.csv` (~180 SKUs)
   - `customers.csv` (~750 customers)
   - `sales_transactions.csv` (~25,000+ line items)
   - `inventory_stock.csv` (~18,000 weekly snapshot records)
4. **Referential Integrity Enforcement:**
   - All `product_id` in sales and inventory are drawn strictly from `products.csv`.
   - All `customer_id` in sales are drawn strictly from `customers.csv` (or left blank for anonymous).
   - All inventory snapshots follow: $\text{opening} + \text{received} - \text{sold} = \text{closing}$.
   - All discounts $\le \text{quantity} \times \text{unit\_price}$.
   - All customer registration dates $\le$ their earliest transaction date.

### Deliverable 2: Comprehensive Technical Documentation Artifacts
Directly in your response, provide the complete markdown documentation:
1. `DATASET_README.md`: Overview of the dataset, retail context, file dependency graph, recommended ingestion order in Sankhya (`products -> customers -> sales -> inventory`).
2. `DATA_DICTIONARY.md`: Full catalog specification, column definitions, data types, constraints, and valid ranges.
3. `DATA_VALIDATION_REPORT.md`: Pre-computed audit metrics verifying row counts, null rates, uniqueness, financial reconciliation, and ML feature readiness.
4. `DATASET_GENERATION_SUMMARY.md`: Technical breakdown of the seasonal curves, festival multipliers, and embedded business scenario triggers.

### Deliverable 3: Representative CSV Samples
Provide the first 10 rows of each of the 4 CSV files formatted inside markdown codeblocks so I can visually inspect the headers, data types, and formatting.

---

## 7. FINAL QUALITY STANDARD

Do not provide pseudocode, incomplete placeholders (e.g. `# TODO: add more products`), or fragmented snippets. Deliver the complete, production-grade Python script and full technical specifications. The generated CSV files must import cleanly into Sankhya on the very first attempt with **0 critical errors, 0 duplicate keys, and a 100% Data Quality Score**.
```
