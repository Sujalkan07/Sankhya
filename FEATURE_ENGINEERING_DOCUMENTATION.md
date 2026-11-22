# SANKHYA — FEATURE ENGINEERING & DATA PREPARATION PIPELINES

**Project:** SANKHYA — AI-Powered Supermarket Analytics and Business Intelligence Platform  
**Document:** Feature Engineering & Data Preparation Pipelines  
**Phase:** Phase 5  
**Version:** 1.0.0  

---

## 1. Overview & Pipeline Philosophy

The machine learning models in Sankhya operate directly upon cleaned, deduplicated supermarket records in MySQL/H2 (populated during Phase 2 ingestion and Phase 3 analytics).

Feature engineering is managed by `com.sankhya.ml.features.FeatureEngineeringService`. The design adheres to three core engineering principles:
1. **Zero Data Leakage:** Time-series and demand models adhere to strict temporal causal ordering. Future transaction dates are strictly excluded from historical rolling windows.
2. **Defensive Imputation:** Missing or zero values (e.g., customers with zero transactions in period, newly launched products with zero velocity) are imputed with domain-appropriate defaults rather than dropping records.
3. **Statistical Scaling:** Features with disparate dimensionalities are standardized using robust $Z$-score scaling (`FeatureScaler`) with safeguards against zero variance.

---

## 2. Customer Behavioral Feature Pipeline (`CustomerFeatureVector`)

### 2.1 Domain Context & Aggregation
To model customer purchasing dynamics for K-Means clustering, transactional history is extracted across a 365-day observation window:

```
+---------------+     +-------------------------+     +-----------------------+
| Customer Table| --> | SalesTransaction Table  | --> | CustomerFeatureVector |
+---------------+     +-------------------------+     +-----------------------+
                              |
                     GROUP BY customerId
                              |
            [recency, frequency, monetary, ATV]
```

### 2.2 Feature Definitions & Math
| Feature Name | Type | Range | Mathematical Definition | Default Imputation |
| :--- | :--- | :--- | :--- | :--- |
| `recency` | Continuous | $[0, 365]$ | $\text{snapshotDate} - \max(t_{\text{tx}})$ in days | $180.0$ days (dormant baseline) |
| `frequency` | Discrete | $[0, \infty)$ | $\text{count}(\text{distinct } \text{transactionId})$ | $0.0$ |
| `monetary` | Continuous | $[0, \infty)$ | $\sum \text{revenue}$ (INR) | $0.0$ |
| `averageTransactionValue` (ATV) | Continuous | $[0, \infty)$ | $\text{monetary} / \text{frequency}$ | $0.0$ |

### 2.3 Feature Scaling
K-Means calculates distances using Euclidean metrics:
$$d(\mathbf{u}, \mathbf{v}) = \sqrt{\sum_{j=1}^p (u_j - v_j)^2}$$
Without normalization, `monetary` (e.g. ₹50,000) would dominate `frequency` (e.g. 5 orders) by a factor of 10,000. `FeatureScaler` standardizes each feature:
$$z = \frac{x - \mu}{\sigma}$$
Where if $\sigma < 10^{-6}$, $\sigma$ defaults to $1.0$.

---

## 3. Daily Sales Time Series Feature Pipeline (`TimeSeriesFeatureVector`)

### 3.1 Domain Context & Aggregation
To forecast store revenue forward 7, 14, and 30 days, daily transactions are aggregated into a contiguous chronological series:

```sql
SELECT DATE(transaction_date), SUM(revenue), COUNT(DISTINCT transaction_id), SUM(quantity)
FROM sales_transactions
WHERE workspace_id = :workspaceId
GROUP BY DATE(transaction_date)
ORDER BY DATE(transaction_date) ASC
```

### 3.2 Time Series Lag & Calendar Features
| Feature Name | Type | Representation | Purpose in Model |
| :--- | :--- | :--- | :--- |
| `trendIndex` | Continuous | $0, 1, 2, \dots, T$ | Captures long-term store revenue growth or contraction. |
| `dayOfWeek` | Discrete | $1=\text{Mon} \dots 7=\text{Sun}$ | Captures intra-week shopping patterns. |
| `isWeekend` | Binary | $1.0$ if Sat/Sun, $0.0$ else | Captures dramatic supermarket weekend footfall surges. |
| `lag1` | Continuous | $y_{t-1}$ | Immediate autoregressive inertia (yesterday's revenue). |
| `lag7` | Continuous | $y_{t-7}$ | Weekly seasonal cycle (same day last week). |
| `rolling7Avg` | Continuous | $\frac{1}{7} \sum_{k=1}^7 y_{t-k}$ | Smoothed weekly moving average. |

### 3.3 Leakage Prevention in Multi-Step Forecasting
During recursive multi-step forecasting ($h = 1 \dots H$):
- At day $h=1$, true historical $y_{t-1}$ and $y_{t-7}$ are used.
- At day $h > 1$, newly predicted $\hat{y}_{t+h-1}$ feeds recursively into `lag1` and `rolling7Avg`.
- Future actuals are never accessed, guaranteeing zero lookahead leakage.

---

## 4. Product SKU Demand Classification Pipeline (`ProductFeatureVector`)

### 4.1 Domain Context & Aggregation
Extracted across a 90-day observation window from `sales_transactions` joined with `products` and `inventory_snapshots`:
- `totalUnitsSold`: 90-day volume.
- `dailyVelocity`: $\text{totalUnitsSold} / 90.0$.
- `unitPrice`: Product retail selling price (INR).
- `currentStock`: Real on-hand warehouse inventory.

### 4.2 Supervised Label Generation
Ground-truth demand classes are assigned using calibrated supermarket velocity benchmarks:
- **`HIGH` Demand:** $\text{dailyVelocity} \ge 4.0\text{ units/day}$.
- **`MEDIUM` Demand:** $1.0 \le \text{dailyVelocity} < 4.0\text{ units/day}$.
- **`LOW` Demand:** $\text{dailyVelocity} < 1.0\text{ units/day}$.
- **Stockout Risk Rule:** When class is `HIGH` and on-hand stock $< (7 \cdot \text{dailyVelocity})$, the SKU triggers a critical stockout risk alert.

---

## 5. Market Basket Multi-Item Transaction Matrix

### 5.1 Extraction Strategy
```sql
SELECT s.transaction_id, s.product_id, COALESCE(p.product_name, s.product_id)
FROM sales_transactions s
LEFT JOIN products p ON s.product_id = p.product_id AND p.workspace_id = :workspaceId
WHERE s.workspace.id = :workspaceId
ORDER BY s.transaction_id
```
- Line items are aggregated into unique `Set<String>` sets by `transaction_id`.
- Single-item baskets are pruned prior to candidate pair generation, maximizing efficiency for Apriori co-occurrence evaluation.
