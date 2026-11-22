# SANKHYA — Core Analytics Formula & Mathematical Methodology
## Phase 3 Mathematical Specification Document

This document defines the mathematical foundations, statistical algorithms, aggregation rules, and formulas implemented across the **Sankhya** Core Analytics Engine.

---

## 1. Common Aggregation Principles & Integrity Guardrails

### 1.1 Line-Item vs. Transaction Distinction
A point-of-sale dataset contains line-item level granularity where a single basket checkout may consist of multiple line items sharing an identical `transaction_id`.
- **Row Count**: Represents the count of distinct product sales rows.
- **Transaction Count**: Represents the count of distinct customer basket checkouts:
  $$\text{Total Transactions} = \text{COUNT}(\text{DISTINCT } \text{transaction\_id})$$
- All basket metrics (Average Transaction Value, Items Per Basket) strictly divide by distinct transactions, avoiding invoice row inflation.

### 1.2 Multi-Tenant Workspace Scoping
Every metric, sum, count, and statistical quantile is strictly constrained to the active workspace:
$$\forall \text{ queries}: \text{workspace\_id} = \text{authenticated\_user\_active\_workspace\_id}$$

### 1.3 Safe Division & Zero-Baseline Guardrails
To prevent `ArithmeticException` (division by zero) and invalid null states:
$$\text{safeDivide}(N, D) = \begin{cases} \frac{N}{D} & \text{if } D \neq 0 \text{ and } N, D \neq \text{null} \\ 0.00 & \text{otherwise} \end{cases}$$

For percentage change comparisons where the previous baseline is zero:
$$\Delta\% = \begin{cases} 
0.00\% & \text{if } \text{current} = 0 \text{ and } \text{previous} = 0 \\
+100.00\% & \text{if } \text{current} > 0 \text{ and } \text{previous} = 0 \\
-100.00\% & \text{if } \text{current} = 0 \text{ and } \text{previous} > 0 \\
\frac{\text{current} - \text{previous}}{|\text{previous}|} \times 100 & \text{otherwise}
\end{cases}$$

---

## 2. Sales Analytics Formulas (Module 2)

### 2.1 Gross Revenue
$$\text{Gross Revenue} = \sum_{i=1}^{N} \left( \text{quantity}_i \times \text{unit\_price}_i - \text{discount}_i \right)$$

### 2.2 Average Transaction Value (ATV) / Basket Value
$$\text{ATV} = \frac{\text{Gross Revenue}}{\text{COUNT}(\text{DISTINCT } \text{transaction\_id})}$$

### 2.3 Average Basket Size (Items per Basket)
$$\text{Average Basket Size} = \frac{\sum_{i=1}^{N} \text{quantity}_i}{\text{COUNT}(\text{DISTINCT } \text{transaction\_id})}$$

### 2.4 Hourly Velocity Distribution
Transactions and revenues are binned into 24 hour buckets ($h \in [0, 23]$):
$$\text{Hourly Revenue}_h = \sum_{t \in \text{Txn}_h} \text{revenue}_t$$
The engine guarantees a continuous 24-point series, populating $0$ for hours with zero traffic.

### 2.5 Payment Tender Mix Share
$$\text{Share}_{\text{method}} = \frac{\text{Revenue}_{\text{method}}}{\text{Total Gross Revenue}} \times 100$$

---

## 3. Product Intelligence & SKU Economics Formulas (Module 4)

### 3.1 Estimated Cost of Goods Sold (COGS)
Calculated by joining sales line items with the product catalog:
$$\text{COGS}_{\text{product}} = \sum_{j \in \text{Sales}_{\text{product}}} \left( \text{quantity}_j \times \text{product.unit\_cost} \right)$$
$$\text{Total COGS} = \sum_{\text{all products}} \text{COGS}_{\text{product}}$$

### 3.2 Gross Profit & Gross Margin Percentage
$$\text{Gross Profit} = \text{Revenue} - \text{COGS}$$
$$\text{Gross Margin } \% = \frac{\text{Gross Profit}}{\text{Revenue}} \times 100$$

### 3.3 BCG-Style Product Performance Quadrant Matrix
Products active in the date interval are classified into 4 quadrants based on sample median volume ($\tilde{V}$) and median margin ($\tilde{M}$):
1. **Star**: $\text{Volume} \ge \tilde{V}$ and $\text{Margin } \% \ge \tilde{M}$ (High Volume, High Margin)
2. **Cash Cow**: $\text{Volume} \ge \tilde{V}$ and $\text{Margin } \% < \tilde{M}$ (High Volume, Low Margin)
3. **Opportunity**: $\text{Volume} < \tilde{V}$ and $\text{Margin } \% \ge \tilde{M}$ (Low Volume, High Margin)
4. **Underperformer**: $\text{Volume} < \tilde{V}$ and $\text{Margin } \% < \tilde{M}$ (Low Volume, Low Margin)

---

## 4. Customer Intelligence & RFM Segmentation Formulas (Module 3)

### 4.1 Exclusion of Anonymous Shoppers
Cash transactions without customer loyalty identification (`customer_id IS NULL OR TRIM(customer_id) = ''`) are excluded from customer-level behavioral aggregations to avoid skewing loyalty and churn statistics.

### 4.2 Repeat Purchase Rate
$$\text{Repeat Purchase Rate } \% = \frac{\text{COUNT}(\text{Customers with } \ge 2 \text{ distinct transactions})}{\text{COUNT}(\text{Total Active Customers in interval})} \times 100$$

### 4.3 Average Revenue Per User (ARPU)
$$\text{ARPU} = \frac{\text{Gross Revenue from Identified Customers}}{\text{Total Active Identified Customers}}$$

### 4.4 RFM Quintile Scoring (Scores 1 to 5)
For each identified customer active in the period:
- **Recency ($R_{\text{days}}$)**: Days elapsed between the customer's latest transaction and the filter period boundary:
  $$R_{\text{days}} = \max\left(0, \text{ChronoUnit.DAYS.between}(\text{latestTxnDate}, \text{filterEndDateTime})\right)$$
  - Lower days indicates higher recency: Top 20% most recent receive Score 5, descending to Score 1 for least recent.
- **Frequency ($F$)**: Count of distinct transactions in the period:
  - Top 20% most frequent receive Score 5, descending to Score 1.
- **Monetary ($M$)**: Total currency spent in the period:
  - Top 20% highest spenders receive Score 5, descending to Score 1.

### 4.5 The 9 Canonical Retail Segments
| Segment Name | Scoring Condition | Business Meaning |
| :--- | :--- | :--- |
| **Champions** | $R \ge 4 \land F \ge 4 \land M \ge 4$ | Highest spenders, frequent shoppers, visited recently |
| **Loyal Customers** | $R \ge 3 \land F \ge 3 \land M \ge 3$ | Consistent repeat buyers, responsive to promotions |
| **Potential Loyalists** | $R \ge 4 \land F \le 2 \land M \ge 2$ | Recent shoppers with above-average spend |
| **New Customers** | $R \ge 4 \land F = 1$ | First purchase within the recent window |
| **Promising** | $R = 3 \land F \le 2 \land M \le 3$ | Average recency and spend, moderate upside |
| **Cannot Lose Them** | $R \le 2 \land F \ge 4 \land M \ge 4$ | High historic spenders who haven't returned recently |
| **At Risk** | $R \le 2 \land F \ge 3 \land M \ge 3$ | Above-average frequency and spend, slipping away |
| **Hibernating** | $R \le 2 \land F \le 2 \land M \le 2$ | Low spenders, infrequent, long elapsed time |
| **Lost** | $R = 1 \land F = 1 \land M = 1$ | Lowest possible engagement across all 3 dimensions |

---

## 5. Inventory Intelligence Formulas (Module 5)

### 5.1 Latest Snapshot Resolution (Non-Summative Rule)
Closing stock must **never** be summed across multiple snapshot dates. Current inventory metrics query strictly the latest snapshot per SKU:
$$\text{Latest Snapshot}(p) = \text{snapshot where } \text{snapshot\_date} = \max_{s \in \text{ProductSnapshots}(p)}(\text{date}_s)$$

### 5.2 Inventory Valuation
$$\text{Cost Valuation} = \sum_{p \in \text{SKUs}} \left( \text{closing\_stock}_p \times \text{unit\_cost}_p \right)$$
$$\text{Retail Valuation} = \sum_{p \in \text{SKUs}} \left( \text{closing\_stock}_p \times \text{selling\_price}_p \right)$$

### 5.3 Stock Health Classification
For each SKU based on its latest closing stock $C$ and reorder threshold $R$:
- **Out of Stock**: $C \le 0$
- **Low Stock**: $0 < C \le R$
- **Overstocked**: $R > 0 \land C > 3 \times R$
- **Healthy**: $R < C \le 3 \times R$ (or $C > 0$ when $R=0$)

### 5.4 Suggested Replenishment Order Quantity
When an item is low stock or out of stock ($C \le R$):
$$\text{Suggested Order Quantity} = \max\left( (2 \times R) - C, \max(R, 10) \right)$$
$$\text{Estimated Restock Cost} = \text{Suggested Order Quantity} \times \text{unit\_cost}$$

### 5.5 Inventory Turnover Ratio
$$\text{Turnover Ratio} = \frac{\text{COGS in Period}}{\text{Total Cost Valuation}}$$
