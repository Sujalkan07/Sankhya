# SANKHYA — Business Recommendation Rules Engine
## Deterministic Heuristic Specifications & Lifecycle Documentation

This document describes the business rules, threshold parameters, evaluation logic, severity tiers, and status lifecycle implemented in the **Sankhya Rule-Based Recommendation Engine** (`BusinessRecommendationService`).

---

## 1. Engine Architecture & Design Principles

The Sankhya Recommendation Engine evaluates operational retail health deterministically without black-box approximations:
1. **Multi-Domain Coverage**: Rules monitor Sales, Inventory, Product Economics, Customer Churn, and Store Concentration.
2. **Idempotence & Anti-Spam Guardrails**:
   - The engine checks whether an active recommendation (`status IN ('NEW', 'REVIEWED')`) for the same `rule_identifier` and target SKU/customer/category already exists in the active workspace.
   - If an active recommendation exists, re-creation is suppressed to prevent cluttering the manager's dashboard.
3. **Workspace Isolation**:
   - Every rule evaluation query is strictly filtered by `workspace_id`.
   - Recommendations are persisted to `business_recommendations` table with foreign key linkage to the tenant workspace.
4. **Lifecycle State Machine**:
   $$\text{NEW} \xrightarrow{\text{Mark Reviewed}} \text{REVIEWED} \xrightarrow{\text{Action Taken}} \text{RESOLVED}$$
   $$\text{NEW / REVIEWED} \xrightarrow{\text{User Dismissal}} \text{DISMISSED}$$

---

## 2. Recommendation Rules Specification

### Rule 1: Stockout Risk Replenishment
- **Rule Identifier**: `RULE_STOCKOUT_RISK`
- **Domain Category**: `INVENTORY`
- **Severity**: `CRITICAL` (if `closingStock <= 0`), `HIGH` (if `0 < closingStock <= reorderLevel`)
- **Priority Tier**: Priority 1 (Stockouts) / Priority 2 (Low stock)
- **Trigger Condition**:
  $$\text{latestClosingStock} \le \text{reorderLevel}$$
- **Recommended Action**:
  $$\text{Suggested Order} = \max\left( (2 \times \text{reorderLevel}) - \text{closingStock}, \max(\text{reorderLevel}, 10) \right)$$
  "Place an immediate purchase order for $N$ units to prevent stockout and lost sales."
- **Supporting Metric**: Product SKU ID

---

### Rule 2: Excess Inventory Holding Cost
- **Rule Identifier**: `RULE_OVERSTOCK_HOLDING_COST`
- **Domain Category**: `INVENTORY`
- **Severity**: `MEDIUM`
- **Priority Tier**: Priority 4
- **Trigger Condition**:
  $$\text{reorderLevel} > 0 \land \text{latestClosingStock} > 3 \times \text{reorderLevel}$$
- **Recommended Action**:
  "Consider a promotional discount, feature end-cap display, or product bundle to improve inventory turnover and release working capital."
- **Supporting Metric**: Product SKU ID

---

### Rule 3: High-Volume Low-Margin Optimization
- **Rule Identifier**: `RULE_TOP_PRODUCT_MARGIN_OPTIMIZATION`
- **Domain Category**: `PRODUCT`
- **Severity**: `MEDIUM`
- **Priority Tier**: Priority 3
- **Trigger Condition**:
  $$\text{quantitySold} \ge 20 \land \text{grossMarginPercentage} < 20.0\%$$
- **Recommended Action**:
  "Review supplier wholesale cost or consider a 3-5% retail price adjustment to improve category profitability on high-velocity items."
- **Supporting Metric**: Product SKU ID

---

### Rule 4: VIP Customer Churn Prevention
- **Rule Identifier**: `RULE_CUSTOMER_CHURN_RISK`
- **Domain Category**: `CUSTOMER`
- **Severity**: `HIGH`
- **Priority Tier**: Priority 2
- **Trigger Condition**:
  $$\text{recencyDays} > 30 \land \text{frequency} \ge 3 \land \text{monetarySpend} \ge \$150.00$$
- **Recommended Action**:
  "Trigger a personalized SMS or email win-back campaign with a 10% re-engagement voucher to recover this high-value customer."
- **Supporting Metric**: Customer ID

---

### Rule 5: Period Revenue Decline Warning
- **Rule Identifier**: `RULE_REVENUE_DECLINE_ALERT`
- **Domain Category**: `SALES`
- **Severity**: `HIGH`
- **Priority Tier**: Priority 1
- **Trigger Condition**:
  $$\text{previousRevenue} \ge \$100.00 \land \frac{\text{currentRevenue} - \text{previousRevenue}}{\text{previousRevenue}} \times 100 \le -10.0\%$$
- **Recommended Action**:
  "Examine sales drop across key categories and review basket sizes to diagnose footfall vs pricing issues."
- **Supporting Metric**: `WORKSPACE_REVENUE`

---

### Rule 6: Departmental Category Concentration
- **Rule Identifier**: `RULE_CATEGORY_CONCENTRATION`
- **Domain Category**: `SALES`
- **Severity**: `LOW`
- **Priority Tier**: Priority 5
- **Trigger Condition**:
  $$\frac{\text{categoryRevenue}}{\text{totalRevenue}} \times 100 \ge 50.0\% \land \text{totalCategories} > 1$$
- **Recommended Action**:
  "Diversify sales mix by launching promotional campaigns for complementary departments to reduce single-category revenue vulnerability."
- **Supporting Metric**: Category Name

---

### Rule 7: Low Repeat Purchase Rate Alert
- **Rule Identifier**: `RULE_REPEAT_PURCHASE_LOW`
- **Domain Category**: `CUSTOMER`
- **Severity**: `MEDIUM`
- **Priority Tier**: Priority 3
- **Trigger Condition**:
  $$\text{activeCustomers} \ge 10 \land \text{repeatPurchaseRate} < 20.0\%$$
- **Recommended Action**:
  "Introduce a customer loyalty card or send bounce-back coupons valid within 7 days of initial purchase to convert one-time shoppers into repeat buyers."
- **Supporting Metric**: `REPEAT_PURCHASE`

---

## 3. Database Persistence & Indices

Recommendations are persisted in the `business_recommendations` table created in `V3__phase3_analytics.sql`:
- Composite indices on `(workspace_id, status)`, `(workspace_id, category)`, and `(workspace_id, severity)` ensure sub-millisecond querying during dashboard loading.
- Unique check query prevents duplicate insertions while keeping an audit trail of historical resolved recommendations.
