# SANKHYA — PHASE 5 INITIAL ARCHITECTURE & CODEBASE AUDIT

**Project:** SANKHYA — AI-Powered Supermarket Analytics and Business Intelligence Platform  
**Phase:** Phase 5 — Advanced Machine Learning Engine, Predictive Analytics, Model Evaluation & Gemini AI Integration  
**Date:** November 22, 2026  
**Status:** COMPLETE & VERIFIED  

---

## 1. Executive Summary & Context

Sankhya is an enterprise-grade supermarket analytics and business intelligence platform built on **Java 21**, **Spring Boot 3.3.5**, **Spring Data JPA / Hibernate**, **Spring Security 6**, and **Thymeleaf with Tailwind CSS & Chart.js**. 

Phases 1 through 4 have established:
1. **Phase 1 (Foundation):** Multi-tenant workspace architecture, role-based access control (ADMIN, MANAGER, ANALYST, VIEWER), user authentication, audit logging, and responsive Stitch UI shell.
2. **Phase 2 (Data Ingestion & Preparation):** Comprehensive CSV/Excel ingestion pipeline, automated validation, cleaning, deduplication, synthetic dataset generation (5,000+ realistic supermarket records), and staging.
3. **Phase 3 (Core Analytics Engine):** High-performance transactional aggregation, Sales KPIs, Customer RFM scoring, Product ABC/Pareto categorization, Inventory stockout and turnover analysis, and rule-based business recommendation engine.
4. **Phase 4 (Frontend Integration & Dashboards):** Full-featured responsive UI across all 8 modules (Overview, Sales, Customers, Products, Recommendations, Inventory, Datasets, AI Insights) conforming to Google Stitch design specifications, What-If simulator, SKU economics modal, and transaction drill-downs.

Phase 5 transitions Sankhya from descriptive and diagnostic analytics into **predictive and prescriptive intelligence**. This audit establishes the architectural baseline, data readiness, and implementation roadmap for the Machine Learning Engine and Google Gemini AI integration.

---

## 2. Existing Workspace & Entity Inventory

The database schema and entity model already persist rich, normalized supermarket business telemetry isolated by `workspace_id`:

| Entity | Primary Attributes | Business Telemetry for ML |
| :--- | :--- | :--- |
| `Workspace` | `id`, `name`, `slug`, `currency`, `timezone` | Tenancy boundary for all model training and inference. |
| `Customer` | `id`, `workspace_id`, `customerCode`, `name`, `email`, `phone`, `segment`, `totalSpend`, `totalVisits`, `firstVisit`, `lastVisit` | Feature source for **K-Means Clustering** (Recency, Frequency, Monetary, Average Transaction Value, Unit counts). |
| `Product` | `id`, `workspace_id`, `productCode`, `name`, `category`, `subcategory`, `costPrice`, `unitPrice`, `stockQuantity`, `reorderLevel`, `status` | Feature source for **Random Forest Demand Classification** and **Apriori Basket Analysis**. |
| `SalesTransaction` & `TransactionItem` | `id`, `workspace_id`, `invoiceNumber`, `transactionDate`, `customerCode`, `productCode`, `productName`, `category`, `quantity`, `unitPrice`, `totalAmount`, `paymentMethod`, `storeBranch` | Training data for **Apriori Association Rules**, **Linear Regression Revenue Forecasting**, and **Demand Classification**. |
| `InventorySnapshot` | `id`, `workspace_id`, `snapshotDate`, `productCode`, `currentStock`, `minStock`, `maxStock`, `dailyConsumption`, `stockStatus` | Validation and input for **Demand Forecasting & Stockout Risk Detection**. |
| `BusinessRecommendation` | `id`, `workspace_id`, `category`, `title`, `description`, `impactScore`, `urgency`, `confidence`, `status` | Persistence target for **ML-derived prescriptive actions**. |

---

## 3. Analysis of Existing Analytics Services

The application currently has production-ready analytical services in `com.sankhya.analytics.*`:
- `SalesAnalyticsService`: Computes time-series sales aggregation, date-preset filtering (7D, 30D, 90D, 12M), category breakdowns, and payment method distributions.
- `CustomerIntelligenceService`: Computes recency (days since last purchase), purchase frequency, monetary total, and rule-based segment tags (Champions, Loyal, At Risk, Hibernating, New).
- `ProductIntelligenceService`: Performs ABC Pareto revenue classification, margin analysis, and sales velocity tracking.
- `InventoryAnalyticsService`: Computes reorder points, safety stock, turnover ratio, and estimated days-of-supply.
- `BusinessRecommendationService`: Evaluates rules against current metrics to generate tactical recommendations.

**Key Opportunity for Phase 5:**  
Phase 5 will not replace these services; rather, it introduces dedicated machine learning models that generate deeper predictive patterns (e.g., true mathematical clustering via K-Means rather than heuristic RFM thresholds; item co-occurrence associations via Apriori; rolling demand classification via Random Forest; multi-step recursive revenue forecasting via Linear Regression).

---

## 4. Phase 5 Machine Learning Architecture Blueprint

### 4.1 Strict Technological Guarantees
- **Pure Java 21 Implementation**: Zero Python, Flask, FastAPI, or microservice dependencies. Zero native binaries that fail on cross-platform hosts. All math, matrix manipulations, and tree ensembles run natively in the JVM.
- **Tenant Isolation**: Every training run, hyperparameter set, evaluation metric, cluster assignment, association rule, and prediction is strictly foreign-keyed and scoped to `workspace_id`.
- **Zero Hallucination Grounding**: Google Gemini is utilized exclusively as an intelligent synthesizer, narrative explainer, and business consultant grounded strictly in verified backend metrics.

---

## 5. Security & Secret Management

- `GEMINI_API_KEY` is retrieved dynamically from environment variables / `.env` (`${GEMINI_API_KEY:}`).
- `.env` is verified in `.gitignore` and must never be committed.
- `.env.example` provides the documented development template.
- Application gracefully degrades when `GEMINI_API_KEY` is not present, displaying clear administrative alerts in the UI rather than throwing unhandled exceptions.
- Zero Customer PII (names, phone numbers, email addresses) is transmitted to the Gemini API. Only aggregated cohort metrics, anonymized IDs, and telemetry summaries are provided in prompts.

---

## 6. Baseline Verification

- **Current Automated Tests:** 49 test cases passing with 0 failures and 0 errors across controllers, services, repositories, and synthetic generators.
- **Phase 5 Target:** Extend test coverage to include comprehensive mathematical and integration tests for K-Means, Apriori, Random Forest, Linear Regression, Model Registry, and Gemini API client.
