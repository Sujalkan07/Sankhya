# SANKHYA — PHASE 2 IMPLEMENTATION REPORT
## Dataset Management, Data Ingestion, Synthetic Data Generation & Data Preparation

**Project Name:** SANKHYA — AI-Powered Supermarket Analytics Platform  
**Phase:** Phase 2 of 6  
**Status:** Completed  
**Technology Stack:** Java 21 LTS, Spring Boot 3.3.5, Spring Data JPA, Hibernate, MySQL 8+, Thymeleaf, Apache Commons CSV 1.11.0, Apache POI 5.3.0, Maven  
**Architecture:** Multi-Tenant Modular Monolith with Workspace Isolation  

---

## 1. Executive Summary

Phase 2 builds the core ingestion, schema mapping, validation, cleaning, and synthetic data preparation engine for Sankhya. In Phase 1, the foundational user authentication, tenant workspace scoping, navigation shell, and database architecture were implemented. Phase 2 introduces full support for ingesting multi-table supermarket datasets via CSV and Excel, a rule-based validation engine with 5-dimension quality scoring, error logging with downloadable CSV reports, and a pure Java synthetic Indian supermarket dataset generator spanning 1 to 3 years of relationally consistent retail telemetry.

All 23 automated unit and integration tests execute with zero errors, and all 9 UI screens have been designed and implemented in Thymeleaf matching the Stitch UI design reference (`https://stitch.google.com/projects/9936945891794056152`).

---

## 2. Ingestion Engine Architecture

The ingestion pipeline is designed as an asynchronous, staged lifecycle:

```mermaid
flowchart LR
    A["File Upload (.csv, .xlsx)"] --> B["Header & Preview Parsing"]
    B --> C["Automatic Column Mapping"]
    C --> D["Deep Validation Engine"]
    D --> E["Data Quality Scoring"]
    E --> F["Confirmation & Batch Commit"]
    F --> G["Domain Database Tables"]
```

### 2.1 File Parsers
- `CsvFileParser`: Built with Apache Commons CSV. Handles UTF-8 BOM, trims whitespace, supports RFC 4180 formatting, escaped quotes, and handles varying column lengths.
- `ExcelFileParser`: Built with Apache POI (`poi` and `poi-ooxml`). Parses modern `.xlsx` workbooks and legacy `.xls` files with formula evaluation, formatted numeric cells, and ISO date parsing.
- `FileStorageService`: Safely persists files in `./data/uploads/workspaces/{workspaceId}/` with UUID sanitization and directory traversal protection.

### 2.2 Import Job Lifecycle States
1. `PENDING`: Staged upload file saved; headers detected and mapped.
2. `VALIDATING`: Deep validation running across all records.
3. `READY_FOR_CONFIRMATION`: Quality scores calculated; awaiting user review.
4. `PROCESSING`: Batch insertion into domain database tables.
5. `COMPLETED`: All records committed successfully.
6. `COMPLETED_WITH_ERRORS`: Ingested valid records while skipping flagged error rows.
7. `FAILED`: Fatal format errors or zero valid records.
8. `CANCELLED`: Staged import aborted by user; file deleted.

---

## 3. Four Core Supermarket Dataset Schemas

| Dataset Category | Canonical Entity | Mandatory Fields | Business Logic & Rules |
| :--- | :--- | :--- | :--- |
| **Sales Transactions** | `SalesTransaction` | `transaction_id`, `transaction_date`, `product_id`, `quantity`, `unit_price` | Transaction grouping across line items, positive qty, non-negative unit price, discount $\le$ gross value, calculated revenue $= (\text{quantity} \times \text{unit\_price}) - \text{discount}$, payment tender normalization (UPI, Cash, Card, Net Banking). |
| **Product Catalog** | `Product` | `product_id`, `product_name`, `category`, `unit_cost`, `selling_price` | Unique SKU per workspace, cost $\ge 0$, selling price $\ge 0$, negative gross margin detection, perishable shelf life days. |
| **Customer Registry** | `Customer` | `customer_id` | Unique customer ID per workspace, demographic attributes, registration dates $\le \text{today}$, age bounds (10-115), RFM loyalty segment. |
| **Inventory Stock** | `InventorySnapshot` | `inventory_id`, `product_id`, `snapshot_date`, `opening_stock`, `closing_stock` | Stock conservation consistency formula: $\text{opening\_stock} + \text{received\_stock} - \text{sold\_quantity} = \text{closing\_stock}$. SKU referential validation. |

---

## 4. Validation Engine & Quality Scoring

The validation engine audits datasets against five dimensions, producing a composite quality score ($0.0 - 100.0\%$):

1. **Completeness Score:** Proportion of required canonical fields present without nulls or blank entries.
2. **Validity Score:** Conformance to required data types (integer, decimal, ISO-8601 timestamps) and numeric domain boundaries ($\ge 0$).
3. **Uniqueness Score:** Detection of duplicate primary keys within the uploaded batch.
4. **Consistency Score:** Logical formula validation (e.g. stock balance formula, discount $\le$ gross amount).
5. **Referential Integrity Score:** Verification that foreign keys (e.g. `product_id` in sales and inventory, `customer_id` in sales) exist in the active workspace's catalogs.

### Error Tracking & Export
- `ImportError` entities record `row_number`, `column_name`, `original_value`, `error_code`, `error_message`, `severity` (`CRITICAL`, `ERROR`, `WARNING`), and actionable `suggested_correction`.
- Users can download an error report CSV directly from `/datasets/imports/{id}/errors/download`.

---

## 5. Pure Java Synthetic Indian Supermarket Generator

The synthetic data generator (`SyntheticDataGeneratorService`) is implemented entirely in pure Java 21 without external scripts, Python dependencies, or third-party generation services:

- **Configurable Timeline:** 12, 24, or 36 months (1 to 3 years) of historical transaction history.
- **Scales:** SMALL, MEDIUM (Recommended), LARGE.
- **Indian Retail Context:**
  - 30 fast-moving products across 8 departments (Amul, Aashirvaad, Tata, Maggi, Fortune, Britannia, Parle, Haldiram's, Everest, Dettol, Surf Excel).
  - Indian demographics across Tier 1 & Tier 2 cities (Mumbai, Bengaluru, Delhi NCR, Pune, Ahmedabad, Hyderabad).
  - Realistic tender mix: 55% UPI, 25% Cash, 18% Cards, 2% Net Banking.
  - Festive surges: Diwali surge (+80% to +100% volume and larger basket size in Oct/Nov), Holi surge (March), summer beverage spikes, and weekend shopping lifts (+40% on Sat/Sun).
  - Physical stock conservation formula strictly maintained across all generated weekly snapshots.

---

## 6. Phase 3 Analytical Query Boundaries

Five dedicated query services provide clean interfaces for future Phase 3 analytics:
- `DatasetQueryService`: Multi-dataset readiness checks and data health metrics.
- `SalesDataService`: Aggregated sales revenue, distinct transaction counts, and date-range queries.
- `CustomerDataService`: Customer profiles, segment distributions, and demographic queries.
- `ProductDataService`: SKU catalog pagination, category lists, and pricing queries.
- `InventoryDataService`: Stock snapshot history, low-stock alerts, and stock movement logs.

---

## 7. Thymeleaf UI Implementation (Stitch Inspired)

9 responsive Thymeleaf screens were designed and implemented under `src/main/resources/templates/dataset/`:
1. `overview.html`: Core dataset management dashboard with metric cards, 4 stream cards, recent jobs, and action hub.
2. `upload.html`: Drag-and-drop file upload with category selection and template download links.
3. `mapping.html`: Schema mapping table pairing file headers to canonical fields with live top-5 preview rows.
4. `preview.html`: 15-row preview table with mapped headers and deep validation trigger.
5. `quality_report.html`: Quality score hero banner, 5-dimension score breakdown, line-level error table, and error CSV download.
6. `import_status.html`: Ingestion completion screen with record counts and navigation CTAs.
7. `history.html`: Full audit history table of all ingestion jobs.
8. `details.html`: Dataset category view with persisted database records and schema specifications.
9. `generator.html`: Synthetic Indian Supermarket data generator control center.

---

## 8. Automated Test Suite Results

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.sankhya.dataset.parser.CsvFileParserTest (2 tests) - SUCCESS
[INFO] Running com.sankhya.dataset.parser.ExcelFileParserTest (2 tests) - SUCCESS
[INFO] Running com.sankhya.dataset.service.DatasetImportServiceIntegrationTest (1 test) - SUCCESS
[INFO] Running com.sankhya.dataset.validator.InventoryDatasetValidatorTest (2 tests) - SUCCESS
[INFO] Running com.sankhya.dataset.validator.ProductDatasetValidatorTest (2 tests) - SUCCESS
[INFO] Running com.sankhya.dataset.validator.SalesDatasetValidatorTest (2 tests) - SUCCESS
[INFO] Running com.sankhya.generator.service.SyntheticDataGeneratorServiceTest (1 test) - SUCCESS
[INFO] Running com.sankhya.repository.UserRepositoryTest (3 tests) - SUCCESS
[INFO] Running com.sankhya.repository.WorkspaceMembershipRepositoryTest (2 tests) - SUCCESS
[INFO] Running com.sankhya.repository.WorkspaceRepositoryTest (1 test) - SUCCESS
[INFO] Running com.sankhya.SankhyaApplicationTests (1 test) - SUCCESS
[INFO] Running com.sankhya.service.AuthServiceTest (2 tests) - SUCCESS
[INFO] Running com.sankhya.service.WorkspaceServiceTest (2 tests) - SUCCESS
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

---

## 9. Verification & Conclusion

Phase 2 is complete, rigorously tested, and fully aligned with the technology stack and architectural guidelines. All data ingestion streams, schema detection routines, validation rules, error reports, synthetic generators, and UI screens are active, isolated per workspace, and ready for Phase 3 Analytical Operations.
