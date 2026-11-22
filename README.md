# Sankhya — Naveen Store AI Supermarket Analytics Platform

**Sankhya** is a single-store retail Business Intelligence, Machine Learning, and Conversational Data Analyst platform built with **Java 21** and **Spring Boot 3.3.5**, pre-configured for **Naveen Store (`STORE-001`)**, owned by **Sujal Kansal** in **Lucknow, Uttar Pradesh, India** (`INR ₹`).

---

## Single-Store Scope & Canonical Analytical Data Mart

Sankhya loads one pre-integrated unified analytical data mart directly on startup via `DevDataInitializer` from [`data/naveen_store_unified_mart.csv`](data/naveen_store_unified_mart.csv) (`125,880,703` bytes, 25 columns):

- **Store Identity:** Naveen Store (`STORE-001`) • Owner: Sujal Kansal • Location: Lucknow, Uttar Pradesh, India
- **Currency:** Indian Rupees (`INR`, `₹`) formatted across all KPIs, tables, charts, CSV exports, and AI answers
- **Dataset Scale:**
  - **500,000** sales line items (`unified_sales_mart`)
  - **83,439** distinct receipts (`BILL-...`)
  - **9,978** purchasing customers (fictional Indian names, standardized `city = Lucknow`)
  - **1,000** distinct SKUs across **19** source-derived categories
  - **Date Window:** `2025-11-22 08:00:00` to `2026-11-22 23:00:00`
- **Reconciled Full-Year Financials (`INR`):**
  - Gross revenue before discount: `₹18,46,85,091.30`
  - Promotional discounts: `₹21,42,473.85`
  - Net revenue: `₹18,25,42,617.45`
  - COGS: `₹13,85,29,200.70`
  - Gross profit: `₹4,40,13,416.75` (`24.11%` gross margin)
- **Inventory Position (`2026-11-22`, 1,000 Deduplicated SKUs):**
  - `994` `HEALTHY` • `3` `LOW_STOCK` • `3` `OVERSTOCK` • `0` `OUT_OF_STOCK` (`169` SKUs with `closing_stock <= reorder_level`)

### Dataset Provenance & Explicit Limitations
- **Source & Synthetic Enrichment:** Product names, categories/subcategories, and basket co-occurrence structure derive from the Instacart Market Basket dataset filtered to the top 1,000 products (some baskets represent filtered partial views). Unit prices, unit costs, quantities, discounts, payment methods, cashier names, fictional customer names, Lucknow city assignment, and closing stock levels are synthetic demonstration data; transaction dates are reconstructed (`2025-11-22` to `2026-11-22`).
- **Single-Snapshot Inventory Honesty:** The mart stores one latest product-level inventory snapshot dated `2026-11-22`. Historical daily stock movement logs, opening/received stock drifts, and supplier lead times are not present and are explicitly disclosed in `/analytics/inventory` and `Ask Naveen Store AI`.

---

## Core Modules

1. **Executive Overview (`/dashboard`)**: Store-level KPI cards (Net Revenue, Receipts, Average Transaction Value, Active Customers, Gross Margin %, Reorder Alerts), daily revenue trajectory, and category revenue distribution.
2. **Sales Intelligence (`/analytics/sales`)**: Net Revenue, Receipts, Average Ticket, Units Sold, Items per Basket (`sales lines / distinct receipts`), Discounts, hourly footfall curve, tender mix, receipt line-item inspection modal, and 14-day Linear Regression revenue forecast.
3. **Customer Intelligence (`/analytics/customers`)**: RFM quintile scoring across 9 lifecycle segments (`Lucknow` buyers), repeat purchase rate, top buyers leaderboard, and K-Means++ behavioral cluster assignments.
4. **Product Intelligence (`/analytics/products`)**: 19-category profitability breakdown, BCG quadrant classification (`Star`, `Cash Cow`, `Opportunity`, `Underperformer`), searchable 1,000-SKU matrix, and Apriori Market Basket pairs.
5. **Inventory Intelligence (`/analytics/inventory`)**: Cost/Retail warehouse valuations in `₹`, Stock Health Classification (`994 / 3 / 3`), Single-Snapshot Inventory Notice & searchable 1,000 deduplicated SKU snapshot position (`2026-11-22`), replenishment alerts, and Random Forest demand risk predictions.
6. **Ask Naveen Store AI (`/analytics/ai-insights`)**: Full-page conversational data analyst backed by `NaveenStoreDataQueryService` (allowlisted parameterized store queries, SQL/prompt-injection guard, unavailable-history refusal, inline KPI cards, data tables, Chart.js visualizations, and Gemini narrative synthesis with verified deterministic fallback).
7. **Business Recommendations & CSV Export Hub (`/analytics/recommendations`, `/analytics/reports`)**: Rule-based alerts and UTF-8 CSV exports with `(INR)` monetary headers.

---

## Quick Start & Running Locally

### Prerequisites
- **JDK 21** (Eclipse Temurin 21 recommended)
- **Maven 3.9+** (or included `mvnw` wrapper)
- Optional `.env` in project root for live Gemini narrative synthesis:
  ```properties
  GEMINI_API_KEY=your_google_gemini_api_key
  GEMINI_MODEL=gemini-3.5-flash-lite
  ```

### Test, Package & Run
```bash
mvn clean package
java -Xmx2g -jar target/sankhya-platform-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### Local Access (`dev` profile)
- **URL:** [http://localhost:8080/](http://localhost:8080/)
- **Credentials:** `admin@sankhya.com` (or `sujal@naveenstore.com`) / `Password@123`
- Direct authentication redirects straight into `/dashboard` for **Naveen Store (`STORE-001`)**. On subsequent startups, `DevDataInitializer` detects all `500,000` rows in `./data/sankhya_dev_db.mv.db` and skips re-seeding automatically.

---

## Project Structure

```text
Sankhya/
├── data/                              # Canonical naveen_store_unified_mart.csv & H2 file database (gitignored)
├── docs/                              # Technical documentation (Schema, Formulas, ML, Features, Gemini)
├── src/main/java/com/sankhya/
│   ├── ai/chat/                       # Safe allowlisted NaveenStoreDataQueryService
│   ├── ai/gemini/                     # GeminiClient, GeminiChatService, GeminiInsightService
│   ├── analytics/                     # Overview, Sales, Customers, Products, Inventory, Recommendations, Reports & AI
│   ├── auth/ & security/              # AuthController, AuthService, AuthenticationSuccessHandler, SecurityConfig
│   ├── common/                        # AnalyticsMathUtils (Indian Rupee formatting), GlobalModelAttributes, exceptions
│   ├── config/                        # Idempotent DevDataInitializer (500,000-row batched mart seed + ML bootstrap)
│   ├── dataset/                       # UnifiedSalesMart entity, repository, validator & parsers
│   └── ml/                            # Pure Java 21 K-Means++, Apriori, Random Forest & Linear Regression
└── src/main/resources/templates/      # Single-store Thymeleaf views & Ask Naveen Store AI full-page UI
```
