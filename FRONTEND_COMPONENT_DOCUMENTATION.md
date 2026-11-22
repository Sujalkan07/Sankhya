# SANKHYA — FRONTEND COMPONENT & DESIGN SYSTEM DOCUMENTATION

**Platform:** Sankhya — Supermarket Analytics & Business Intelligence  
**Phase:** 4 — Frontend Integration, UI/UX Refinement & Complete Module Connectivity  
**Design Reference:** Stitch Project `https://stitch.google.com/projects/9936945891794056152` (SmartMart Supermarket Platform)  
**Date:** November 22, 2026  

---

## 1. Design System & Design Tokens

Sankhya's frontend implements the **SmartMart Stitch Design System**, creating a unified, high-density, accessible retail analytics interface.

### 1.1 Color Palette

| Token Name | Hex Code | Tailwind / Custom Role | Application |
|---|---|---|---|
| **Primary Green** | `#1C6B00` / `#186E00` | `bg-primary`, `text-primary` | Main CTAs, active navigation items, high-performing metrics, positive trends |
| **Primary Container** | `#56DA33` | `bg-primary-container` | Accent pills, live telemetry indicators, secondary action buttons |
| **Primary Hover** | `#125B00` | `hover:bg-primary-hover` | Interactive button hover states |
| **Surface Lowest** | `#FFFFFF` | `bg-surface-container-lowest` | Metric cards, tables, modal dialogs, data visualization containers |
| **Surface Low** | `#F9F9FB` / `#F0F2EB` | `bg-surface-container-low` | Table alternating rows, input fields, badge backgrounds |
| **Surface High** | `#E6F1E1` / `#EFF9E9` | `bg-surface-container` | Active tabs, highlighted rows, status cards |
| **Border / Divider** | `#E7E9E3` | `border-[#E7E9E3]` | Subtle cards borders, horizontal rules, table dividers |
| **Text On-Surface** | `#141E14` | `text-on-surface` | Primary headlines, numerical values, emphasized table data |
| **Text Variant** | `#3E4A38` | `text-on-surface-variant` | Body copy, secondary titles, descriptions |
| **Text Tertiary** | `#6E7B66` | `text-tertiary` | Labels, timestamps, subtle annotations, table headers |
| **Error / Stockout** | `#BA1A1A` / `#EF4444` | `text-error`, `bg-error` | Critical stockout alerts, negative trend deltas, unread notification dots |
| **Warning / Alert** | `#F59E0B` / `#B45309` | `text-amber-700`, `bg-amber-100` | Low stock warnings, moderate urgency directives, markdown risks |
| **Info / Cash Cow** | `#3B82F6` / `#1D4ED8` | `text-blue-700`, `bg-blue-100` | High volume staples, returning customers, audit information |
| **Accent / RFM** | `#8B5CF6` / `#6D28D9` | `text-purple-700`, `bg-purple-100` | RFM composite scores, customer cohort dynamics |

### 1.2 Typography & Iconography

- **Headlines & Metric Displays:** Plus Jakarta Sans (`font-headline`, `font-metric-display`)
- **Body & Tabular Analytics:** Inter (`font-body`, `font-tabular-data`, `font-mono`)
- **Iconography:** Google Material Symbols Outlined (`material-symbols-outlined`), loaded via Google Fonts CDN

---

## 2. Shared Layout Architecture

```
layout/
  └── base.html                 # Master layout shell: Topbar + Sidebar + Main Canvas + Backdrop
fragments/
  ├── head.html                 # Meta, typography fonts, Tailwind CDN, Chart.js CDN, Material Symbols
  ├── topbar.html               # Dual-tier fixed navigation: Brand, Switcher, Global Date Filter, Tabs
  ├── sidebar.html              # Fixed sub-tools navigation: 8 modules, department pivot, ML badge
  └── alerts.html               # Flash alerts (success, error, warning) with auto-dismiss
```

### 2.1 Fixed Dual-Tier Topbar (`fragments/topbar.html`)
- **Tier 1 (Height: `h-16`):**
  - Brand identity with supermarket icon and platform title.
  - Interactive multi-tenant workspace switcher dropdown with direct submission to `/workspaces/{id}/select`.
  - Live analytics engine latency telemetry pill (`<14ms`).
  - Global Date Range dropdown with presets (`TODAY`, `YESTERDAY`, `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_90_DAYS`, `THIS_MONTH`, `LAST_MONTH`, `THIS_QUARTER`, `THIS_YEAR`).
  - Dataset sync shortcut with pulsating status indicator.
  - Interactive notification bell dropdown displaying real-time stockout, margin, and recommendation alerts.
  - Authenticated user profile pill with quick-access dropdown (Settings, Workspaces, Sign Out).
- **Tier 2 (Height: `h-12`):**
  - Horizontally scrollable module navigation tabs:
    1. Overview (`/dashboard`)
    2. Sales Intelligence (`/analytics/sales`)
    3. Customer Intelligence (`/analytics/customers`)
    4. Recommendation Engine (`/analytics/recommendations`)
    5. Inventory Intelligence (`/analytics/inventory`)
    6. Product Intelligence (`/analytics/products`)
    7. Business Insights (`/analytics/ai-insights`)
    8. Dataset Management (`/datasets`)
    9. Reports (`/analytics/reports`)
    10. Settings (`/settings`)

### 2.2 Collapsible & Mobile-Responsive Sidebar (`fragments/sidebar.html`)
- Positioned fixed from `top-28` to `bottom-0` (`w-64`).
- Mobile sliding drawer behavior via `transition-transform duration-300 -translate-x-full lg:translate-x-0` paired with `mobile-menu-btn` and `mobile-sidebar-backdrop` in `layout/base.html`.
- Displays 8 sub-tools navigation with active state highlights (`bg-surface-container text-primary font-bold shadow-xs`).
- Department Pivot summary widget (Produce 98.2%, Dairy 94.1%, FMCG 99.4%, Frozen 91.8%).
- ML Pipeline status card and quick access to Store Workspace Manager, Settings, and Sign Out.

---

## 3. Module Component Specifications

### 3.1 Module 1: Executive Overview (`/dashboard`)
- **Top Banner:** Workspace title, live analytics status badge, date preset selector, quick export shortcut.
- **6 KPI Cards:** Gross Revenue, Invoices, Average Ticket Value (ATV), Active Customers, Gross Margin %, Stock Alerts. All include period-over-period percentage change and directional indicators (`arrow_upward` / `arrow_downward`).
- **Revenue Trend Chart:** Chart.js responsive line chart showing daily revenue telemetry.
- **Category Donut:** Chart.js donut chart illustrating category sales distribution.
- **Descriptive Insights:** Algorithmic takeaways computed live from store telemetry.
- **Urgent Action Items:** Top critical and high priority rule-based recommendations.

### 3.2 Module 2: Sales Analytics (`/analytics/sales`)
- **6 Sales KPIs:** Gross Revenue, Total Invoices, ATV, Units Sold, Items Per Basket (IPB), Promotional Markdowns.
- **Revenue Time Series:** Chart.js line chart with tension smoothing.
- **Hourly Velocity Curve:** 24-hour trading pattern bar chart identifying peak transaction hours.
- **Payment Tender Mix:** Donut chart of payment methods (Cash, Card, UPI, Mobile, Digital).
- **Departmental Margin Breakdown Table:** Category unit sales, revenue, profit, and gross margin %.
- **Top Selling Products Table:** Filterable by client-side search input.
- **Basket Drill-Down Stream & Inspection Modal:** Recent transaction stream with "Inspect Basket" button opening a detailed modal showing cashier lane, tender details, and individual line-item items with quantities, unit prices, discounts, and line totals.

### 3.3 Module 3: Customer Intelligence (`/analytics/customers`)
- **6 Customer KPIs:** Catalog Customers, Active Customers, New Registrations, Returning Shoppers, Repeat Purchase Rate %, ARPU.
- **9 Canonical RFM Segments Summary:** Champions, Loyal Customers, Potential Loyalists, Recent Customers, Promising, Need Attention, About to Sleep, At Risk, Cannot Lose Them.
- **RFM Distribution Bar Chart:** Visual customer count distribution across segments.
- **VIP Shoppers Card:** Top shoppers ranked by gross monetary contribution.
- **Monthly Cohort Retention Table:** Month 0 to Month 4 retention decay matrix tracking customer stickiness.
- **RFM Quintile Matrix Table:** Full customer table with individual Recency (days), Frequency, Monetary (₹), R, F, M quintile scores, composite RFM code, and assigned segment. Features interactive client-side search by Customer ID / Name and segment dropdown filtering.

### 3.4 Module 4: Business Recommendations (`/analytics/recommendations`)
- **5 Summary Metric Cards:** Active Directives, Critical Alerts, High Priority, In Review, Resolved Directives.
- **Multi-Attribute Filters:** Filter by category (Sales, Inventory, Customers, Pricing), status (New, Reviewed, Resolved, Dismissed), and client-side keyword search.
- **Recommendation Cards:** Displays severity badge, category, priority level, rule identifier, title, detailed rationale, and manager action prompt.
- **Lifecycle Action Buttons:** One-click status transitions (`Mark Reviewed`, `Mark Resolved`, `Dismiss`) with flash message confirmations.

### 3.5 Module 5: Inventory Management (`/analytics/inventory`)
- **6 Inventory KPIs:** Tracked SKUs, Cost Valuation, Retail Valuation, Low Stock SKUs, Out of Stock SKUs, Inventory Turnover Ratio.
- **Stock Health Donut Chart:** Classification into Healthy, Low Stock, Out of Stock, and Overstocked.
- **Daily Movement Trend:** Multi-day opening, received, sold, and closing stock trends.
- **Actionable Replenishment Table:** SKUs below reorder point with calculated suggested order quantity, estimated restock cost, and urgency level. Features search filtering.
- **Slow-Moving Inventory Audit:** Identifies SKUs tying up warehouse capital with low turnover and recommends corrective action (bundle promotions, checkout placement, clearance).

### 3.6 Module 6: Product Intelligence (`/analytics/products`)
- **5 Product KPIs:** Catalog SKUs, Active Selling SKUs, Units Sold, Overall Gross Margin %, Lead Category.
- **4 BCG Performance Quadrant Tiles:** Stars, Cash Cows, Opportunities, Underperformers.
- **Category Profitability Table:** Departmental COGS, revenue, profit, and gross margin %.
- **SKU Profitability Matrix:** Table with client-side search.
- **Interactive SKU Economics Modal:** Clicking any SKU row opens an inspection dialog showing units sold, revenue, COGS, gross profit, margin %, and strategic BCG retail advice.

### 3.7 Module 7: AI Insights & What-If Simulator (`/analytics/ai-insights`)
- **Diagnostic AI Insight Cards:** Structured anomaly cards (Margin Expansion Opportunity, Unusual Sales Surge / Stockout Risk) with What Happened, Supporting Telemetry, Why It Matters, and Prescribed Action.
- **Interactive "What-If" Simulator Console:**
  - Parameters: Department selector, Promotional Discount Slider (0-30%), Expected Footfall Uplift Slider (0-50%), Price Elasticity Buttons (0.8, 1.2, 1.6).
  - Real-Time Outcomes: Projected Monthly Revenue, Projected Gross Profit, Blended Gross Margin, Net Working Capital Required.
  - Dynamic SVG Chart: Interactive trajectory curve that recalculates shape and area fill live on slider input.
  - Save Scenario Action: CTA with visual confirmation toast ("Scenario Saved to Executive Vault!").
- **Phase 5 ML Architecture Cards:** 5 enterprise model cards (K-Means, Apriori, Random Forest, Linear Regression, Gemini LLM) detailing algorithms, inputs, and business purposes.

### 3.8 Module 8: Dataset Management (`/datasets`)
- **6-Step Ingestion Wizard:** File Upload -> Column Mapping -> Data Preview -> Schema Validation -> Quality Report -> Persisted Import.
- **Synthetic Data Generator:** Generates 5 realistic Indian supermarket CSV datasets with festive spikes, UPI/Cash splits, and perishable decay patterns.
- **Import History & Analytics Refresh:** Automatic readiness check updating analytical dashboards upon new dataset ingestion.

---

## 4. Accessibility & Responsive Verification

- **Color Contrast:** All text tokens meet WCAG 2.1 AA standards (minimum 4.5:1 for body copy and 3:1 for large text).
- **Keyboard Navigation:** Native interactive elements (`<button>`, `<a>`, `<select>`, `<input>`) support full keyboard tab navigation and focus rings (`focus:ring-2 focus:ring-primary`).
- **Responsive Viewports:** Verified across Desktop (1440px+), Tablet (768px - 1024px), and Mobile (375px - 640px) with responsive grid wraps and collapsible drawer menus.
- **Empty States:** Every view contains dedicated empty state panels with guidance and direct actions to import or generate data.
