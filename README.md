# Sankhya — AI-Powered Supermarket Analytics & Business Intelligence Platform

**Sankhya** is a modular-monolith retail Business Intelligence and Predictive Analytics platform built in **Java 21** and **Spring Boot 3.3.5**. Designed for supermarket chains and multi-store retail operations, Sankhya ingests a single unified supermarket dataset, maintains tenant-isolated store workspaces, computes deterministic financial/operational KPIs, executes four native Java 21 machine learning pipelines, and integrates **Google Gemini AI** for grounded strategic intelligence and interactive Retail Copilot Q&A.

---

## Key Capabilities

### 1. Single Unified Analytical Data Mart & Dataset Pipeline
- **Unified Fact Mart (`unified_sales_mart`)**: Ingests a canonical 22-column CSV/Excel supermarket dataset (`data/sankhya_unified_mart.csv`) capturing POS transactions, basket line items, customer profiles, SKU pricing/margins, and inventory positions.
- **Automatic Dimension Synchronization**: Idempotently synchronizes normalized `products`, `customers`, and `inventory_snapshots` tables alongside the mart during import and development seeding.
- **Validation & Data Quality**: Validates schemas (`UnifiedMartValidator`), checks nulls/negatives/formatting (`DataCleaningUtils`), persists import audit history (`dataset_imports`, `import_errors`), and includes a synthetic Indian supermarket dataset generator (`SyntheticDataGeneratorService`) for multi-branch testing.

### 2. Six Business Intelligence Modules
- **Executive Overview Dashboard (`/dashboard`)**: Period-over-period tracking for Total Revenue, Distinct Transactions, Average Transaction Value (ATV), Active Customers, Gross Margin %, and Stock Alerts with daily revenue trajectory and category share visualizations.
- **Sales Intelligence & Basket Dynamics (`/analytics/sales`)**: Daily revenue curve, hourly footfall distribution, payment method split, category profitability, top selling SKUs, multi-item **POS Basket Line-Item Inspection** modal (`/analytics/sales/api/transactions/{transactionId}`), and forward revenue predictions.
- **Customer Intelligence & RFM Segmentation (`/analytics/customers`)**: 1–5 quintile scoring across Recency, Frequency, and Monetary spend; 9 behavioral RFM segments; Repeat Purchase Rate & Returning/New Shopper cohort breakdown; and K-Means cluster assignments.
- **Product Intelligence & Margin Quadrants (`/analytics/products`)**: BCG matrix classification (**Star**, **Cash Cow**, **Opportunity**, **Underperformer**), category margin comparison, searchable SKU profitability matrix, and interactive SKU economics modal.
- **Inventory Health & Replenishment Engine (`/analytics/inventory`)**: Cost and Retail warehouse valuations, Stock Health Classification (**Healthy**, **Low Stock**, **Out of Stock**, **Overstocked**), actionable safety-stock reorder quantities, and Random Forest Low-Velocity & Stockout Risk watchlists.
- **Deterministic Business Recommendations & CSV Export Center (`/analytics/recommendations`, `/analytics/reports`)**: 7-rule advisory engine paired with one-click CSV exports for Sales, Products, Customers, Inventory, and Recommendations.

### 3. Pure Java 21 Machine Learning Engine
Zero external Python microservices, IPC overhead, or runtime bridges—all four pipelines execute natively inside the Spring Boot JVM with `workspace_id` isolation:
- **Customer Clustering (`KMeansService`)**: K-Means++ initialization ($k = 4$) over $Z$-score standardized `[recency, frequency, monetary, ATV]` vectors with Inertia (SSE) and Silhouette score evaluation.
- **Market Basket Analysis (`AprioriService`)**: Mines multi-item transaction baskets to surface high-lift cross-merchandising rules with Support, Confidence, Lift, and Conviction metrics.
- **SKU Demand & Stockout Risk Classification (`RandomForestClassifier`)**: Ensemble of 30 decision trees (`maxDepth = 5`, `minSamplesSplit = 2`, Gini impurity) trained on an 80/20 split over `[totalUnitsSold, dailyVelocity, unitPrice, currentStock]`.
- **Forward Revenue Forecasting (`MultipleLinearRegression`)**: Autoregressive Ridge-regularized OLS ($\lambda = 10^{-4}$) using trend index, day-of-week, weekend indicator, `lag1`, `lag7`, and 7-day rolling mean for recursive 14-day revenue projections with $\pm 1.96 \cdot \text{RMSE}$ intervals.

### 4. Google Gemini AI Strategic Intelligence & Copilot (`/analytics/ai-insights`)
- **Telemetry-Grounded Narratives**: Synthesizes Executive Summaries, Sales Forecast Briefs, and Product Affinity insights from live workspace KPIs, ML outputs, and explicit dataset provenance metadata.
- **Resilient Model Fallback & Honest Provenance**: Primary `gemini-3.5-flash-lite` with automatic single secondary fallback to `gemini-3-flash-preview` on transient HTTP `404`/`503` unavailability, plus deterministic heuristic fallback (`DETERMINISTIC_FALLBACK`) if the AI service is offline or unconfigured—always labeled transparently in the UI.
- **Interactive Retail Copilot & What-If Simulator**: Conversational chat endpoint (`POST /analytics/ai-insights/chat`) and an interactive margin/discount/COGS simulator anchored to verified live store telemetry baselines.

### 5. Multi-Store Workspaces & Security
- **Authentication & Authorization**: Spring Security 6 form login with BCrypt password hashing, HTTP session fixation protection, and CSRF tokens across state-changing forms and JSON fetch requests.
- **Tenant Isolation**: Every repository query and controller action validates access against the active user's owned or member workspaces (`WorkspaceSecurityService`).

---

## Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Language & Runtime** | Java 21 LTS |
| **Backend Framework** | Spring Boot 3.3.5 (Spring MVC, Spring Security 6, Spring Data JPA, Hibernate 6) |
| **Database** | File-backed H2 in `dev` mode (`./data/sankhya_dev_db`, MySQL compatibility mode); MySQL 8+ ready (`prod` profile) |
| **Data Ingestion & Export** | Apache Commons CSV 1.11.0, Apache POI 5.3.0 (`.xlsx`) |
| **Frontend & Visualization** | Thymeleaf 3, HTML5, Tailwind CSS, JavaScript, Chart.js |
| **AI Integration** | Google Gemini REST API (`generativelanguage.googleapis.com/v1beta`) via Spring `RestClient` |
| **Build & Testing** | Apache Maven (`3.9.9` via wrapper or local `mvn`), JUnit 5, Spring Test / MockMvc |

---

## Quick Start & Running Locally

### Prerequisites
- **JDK 21** (e.g., Eclipse Temurin 21)
- **Maven 3.9+** (or the included Maven wrapper)
- *(Optional)* A **Google Gemini API key** for live AI narrative generation and Retail Copilot chat

### 1. Configure Environment Variables (Optional for Live Gemini AI)
Create a `.env` file in the project root (automatically ignored by Git and imported in the `dev` profile):

```properties
GEMINI_API_KEY=your_google_gemini_api_key
GEMINI_MODEL=gemini-3.5-flash-lite
```

> If `GEMINI_API_KEY` is omitted, all BI dashboards, CSV exports, and Java ML pipelines still run normally, and AI Insights falls back cleanly to deterministic heuristic summaries.

### 2. Run the Automated Test Suite
```bash
mvn clean test
```
Verify all 76 tests across 34 test classes pass with `0` failures and `0` errors.

### 3. Build & Launch Locally
```bash
mvn clean package -DskipTests
java -jar target/sankhya-platform-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Or run directly with Maven:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Access the Application
- **Local URL:** [http://localhost:8080/](http://localhost:8080/)
- **Default Demo Credentials (`dev` profile):**
  - **Email:** `admin@sankhya.com`
  - **Password:** `Password@123`
- **Pre-Seeded Workspaces:**
  - **Green Basket Supermarket** *(Primary workspace auto-seeded with 50,000 rows from `data/sankhya_unified_mart.csv` into persistent `./data/sankhya_dev_db.mv.db`, syncing 1,000 products, 5,526 customers, 1,000 inventory items, and 4 baseline ML model runs)*
  - **Metro Fresh Express** *(Secondary convenience branch populated with synthetic supermarket data for multi-store switching & tenant isolation testing)*

> Note: `./data/` is persisted across restarts once seeded (`DevDataInitializer` skips re-seeding when records already exist). Do not delete `data/sankhya_unified_mart.csv` if re-seeding a fresh database.

---

## Project Structure

```text
Sankhya/
├── data/                              # Local dataset & H2 file database (gitignored)
├── docs/                              # Core technical reference documentation
├── src/
│   ├── main/
│   │   ├── java/com/sankhya/
│   │   │   ├── ai/                    # Gemini client, prompt grounding & insight service
│   │   │   ├── analytics/             # Overview, sales, customers, products, inventory, recommendations, reports & AI controllers
│   │   │   ├── auth/                  # Registration, authentication & password services
│   │   │   ├── common/                # Shared DTOs, exceptions & math utilities
│   │   │   ├── config/                # SecurityConfig, DevDataInitializer, DatabaseConfig, WebConfig
│   │   │   ├── customers/             # Customer entity & repository
│   │   │   ├── dashboard/             # Executive overview & settings controllers
│   │   │   ├── dataset/               # Unified mart ingestion, CSV/Excel parsers, validators & readiness queries
│   │   │   ├── generator/             # Synthetic Indian supermarket dataset generator
│   │   │   ├── inventory/             # Inventory snapshot entity & repository
│   │   │   ├── ml/                    # K-Means++, Apriori, Random Forest, Ridge OLS & feature engineering
│   │   │   ├── onboarding/            # Store setup & dataset onboarding wizard
│   │   │   ├── products/              # Product entity & repository
│   │   │   ├── sales/                 # Sales transaction entity & repository
│   │   │   ├── security/              # UserDetails & workspace access checks
│   │   │   ├── user/                  # User entity & repository
│   │   │   └── workspace/             # Multi-store workspace entities, services & switcher
│   │   └── resources/
│   │       ├── db/migration/          # Reference MySQL DDL scripts (V2–V6)
│   │       ├── templates/             # Thymeleaf layouts, fragments, onboarding & analytics views
│   │       ├── application.yml        # Shared Spring config
│   │       ├── application-dev.yml    # File-backed H2 dev config & .env import
│   │       └── application-prod.yml   # MySQL 8 production profile
│   └── test/                          # Unit & integration tests (76 tests across 34 test classes)
└── pom.xml
```

---

## Technical Documentation
Detailed architecture and mathematical specifications are maintained in [`docs/`](docs/):
- [Dataset Schema & Unified Mart Specification](docs/DATASET_SCHEMA_DOCUMENTATION.md)
- [Analytics Mathematical Formulas](docs/ANALYTICS_FORMULA_DOCUMENTATION.md)
- [Machine Learning Architecture & Specifications](docs/ML_MODEL_DOCUMENTATION.md)
- [Feature Engineering & Data Preparation Pipelines](docs/FEATURE_ENGINEERING_DOCUMENTATION.md)
- [Google Gemini API Integration Guide](docs/GEMINI_INTEGRATION_DOCUMENTATION.md)
