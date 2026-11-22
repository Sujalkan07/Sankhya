# SANKHYA — MACHINE LEARNING MODEL ARCHITECTURE & SPECIFICATIONS

**Project:** SANKHYA — AI-Powered Supermarket Analytics and Business Intelligence Platform  
**Document:** ML Model Architecture & Algorithmic Specifications  
**Phase:** Phase 5  
**Version:** 1.0.0  
**Language:** Java 21 / Spring Boot 3.3.5 (Zero External Microservices, Zero Native Python Binaries)  

---

## 1. Overview & Guarantees

Phase 5 establishes a pure Java 21 machine learning engine embedded natively within the Sankhya application runtime. This guarantees:
- **Zero Process Boundaries**: No IPC latency, no external Flask/FastAPI network hops, no dependency on host Python versions or pip packaging.
- **Tenant Isolation**: Every training dataset, centroid, association rule, prediction, and forecast is partitioned strictly by `workspace_id`.
- **Deterministic Math & Reproducibility**: Seeded pseudorandom generators (`Random(42)`) ensure reproducible training splits, initializations, and bootstrap sampling.
- **Fail-Safe Robustness**: Matrix operations incorporate ridge regularization ($\lambda = 10^{-4}$) to prevent matrix singularity; scaling safeguards against zero-variance features.

---

## 2. Model 1: K-Means Customer Clustering (Unsupervised Segmentation)

### 2.1 Problem Formulation & Business Value
Supermarkets need to distinguish between customer shopping behaviors—differentiating high-spending loyal champions from price-conscious occasional buyers and at-risk dormant shoppers. Heuristic thresholds (e.g. arbitrary spend buckets) fail to capture multi-dimensional patterns. K-Means mathematically partitions customers into compact, distinct cohorts in normalized feature space.

### 2.2 Feature Space & Preprocessing
Input vector $\mathbf{x} = [r, f, m, v]^T \in \mathbb{R}^4$:
1. **$r$ (Recency):** Days elapsed between the customer's last purchase and the snapshot date.
2. **$f$ (Frequency):** Count of distinct transactional checkout events within the observation window.
3. **$m$ (Monetary):** Cumulative gross spending (INR) across the observation window.
4. **$v$ (Average Transaction Value):** $m / f$, representing average ticket size.

**Standard Z-Score Normalization:**
Features vary by orders of magnitude (monetary in thousands, frequency in single digits). Each feature is standardized:
$$z_{ij} = \frac{x_{ij} - \mu_j}{\sigma_j}$$
Where $\sigma_j = \sqrt{\frac{1}{n-1} \sum_{i=1}^n (x_{ij} - \mu_j)^2}$. If $\sigma_j < 10^{-6}$, $\sigma_j$ is set to $1.0$ to prevent division by zero.

### 2.3 Algorithmic Execution: K-Means++
1. **Initialization:**
   - First centroid $\mathbf{c}_1$ chosen uniformly at random from dataset $\mathcal{X}$.
   - Subsequent centroids $\mathbf{c}_k$ ($k=2 \dots K$) chosen with probability proportional to the squared Euclidean distance to the closest existing centroid:
     $$P(\mathbf{x}) = \frac{D(\mathbf{x})^2}{\sum_{\mathbf{x}' \in \mathcal{X}} D(\mathbf{x}')^2}$$
2. **Assignment Step:**
   Assign each point $\mathbf{x}_i$ to nearest cluster centroid:
   $$\arg\min_{k} \|\mathbf{x}_i - \mathbf{c}_k\|_2$$
3. **Update Step:**
   Update centroid $\mathbf{c}_k = \frac{1}{|S_k|} \sum_{\mathbf{x} \in S_k} \mathbf{x}$.
4. **Convergence:**
   Iterate until assignment changes cease or 100 iterations complete.

### 2.4 Model Evaluation & Interpretability
- **Inertia (SSE):** $\sum_{k=1}^K \sum_{\mathbf{x} \in S_k} \|\mathbf{x} - \mathbf{c}_k\|^2$.
- **Silhouette Coefficient ($s$):**
  $$s(i) = \frac{b(i) - a(i)}{\max(a(i), b(i))}$$
  Where $a(i)$ is mean intra-cluster distance, $b(i)$ is smallest mean distance to another cluster.
- **Automatic Business Labeling:** Centroids are inverted back to original currency and day dimensions. Clusters are sorted by monetary rank into:
  - *High-Value Champions*
  - *Loyal Frequent Shoppers*
  - *Emerging Regulars*
  - *At-Risk Occasionals*
  - *Lapsed Budget Buyers*

---

## 3. Model 2: Apriori Market Basket Association Mining

### 3.1 Problem Formulation & Business Value
Uncovering hidden product cross-affinities allows supermarket managers to optimize aisle end-caps, design co-located shelf displays, and create bundling promotions (e.g. Diwali sweet kits, breakfast combos).

### 3.2 Candidate Generation & Pruning
Given transaction database $\mathcal{D} = \{T_1, T_2, \dots, T_N\}$ where each transaction $T_i \subseteq \mathcal{I}$ represents items purchased together:
1. **Frequent 1-Itemsets ($L_1$):**
   Items with support $\ge \text{minSupport}$.
2. **Frequent 2-Itemsets ($L_2$):**
   Candidate pairs $\{i_a, i_b\}$ evaluated across all multi-item baskets.
3. **Metric Formulas:**
   - **Support:**
     $$\text{Supp}(A \Rightarrow B) = \frac{\text{count}(A \cup B)}{N}$$
   - **Confidence:**
     $$\text{Conf}(A \Rightarrow B) = \frac{\text{count}(A \cup B)}{\text{count}(A)} = P(B \mid A)$$
   - **Lift:**
     $$\text{Lift}(A \Rightarrow B) = \frac{\text{Conf}(A \Rightarrow B)}{\text{Supp}(B)} = \frac{P(A \cap B)}{P(A) \cdot P(B)}$$
     - $\text{Lift} > 1.0$: Positive affinity (items co-occur more frequently than expected under independence).
     - $\text{Lift} = 1.0$: Statistical independence.
     - $\text{Lift} < 1.0$: Negative association (items co-occur less often than expected under statistical independence; Lift alone does not prove product substitution without category/price elasticity verification).
   - **Conviction:**
     $$\text{Conv}(A \Rightarrow B) = \frac{1 - \text{Supp}(B)}{1 - \text{Conf}(A \Rightarrow B)}$$

---

## 4. Model 3: Random Forest SKU Demand Classification

### 4.1 Problem Formulation & Business Value
Categorizing SKU velocity dynamics and stockout risk enables automated replenishment triggers, warehouse capital optimization, and prioritized supplier purchase orders.

### 4.2 Feature Representation
For each product SKU:
- $x_1$: `totalUnitsSold` (90-day volume)
- $x_2$: `dailyVelocity` (units/day)
- $x_3$: `unitPrice` (selling price)
- $x_4$: `currentStock` (on-hand inventory)
- Target label: `HIGH`, `MEDIUM`, `LOW`

### 4.3 Ensemble Architecture
- **Number of Trees:** `30` decision trees configured in standard workflow orchestration (`MLJobService.executeRandomForest` / `AiInsightsController`), parameterized in `ProductDemandClassificationService.trainAndClassify(workspaceId, numTrees)`.
- **Tree Depth & Split Bounds:** `maxDepth = 5`, `minSamplesSplit = 2`.
- **Train/Holdout Split:** 80% train / 20% test split evaluated in `ProductDemandClassificationService` (falls back to training set when `n == 1` / `testSize == 0`).
- **Feature Subsampling:** $\lfloor \sqrt{p} \rfloor = 2$ features randomly selected at each split.
- **Split Criterion (Gini Impurity):**
  $$I_G(S) = 1 - \sum_{c} p_c^2$$
  $$\Delta I_G = I_G(S) - \left( \frac{|S_L|}{|S|} I_G(S_L) + \frac{|S_R|}{|S|} I_G(S_R) \right)$$
- **Bootstrap Aggregation (Bagging):** Each tree trained on $N$ samples drawn with replacement.
- **Ensemble Voting:** Majority vote with confidence probability $P(c) = \frac{\text{votes}(c)}{T}$.
- **Stockout Risk Trigger:** When predicted class is `HIGH` and on-hand stock is less than 7-day predicted velocity ($7 \cdot \text{velocity}$), risk level escalates to `HIGH`.

---

## 5. Model 4: Linear Regression Forward Revenue Forecasting

### 5.1 Problem Formulation & Business Value
Predicting forward daily revenue across 7, 14, and 30-day horizons gives supermarket executives visibility into cash flow, working capital requirements, and seasonal surge management.

### 5.2 Autoregressive Feature Vector
For each day $t$:
$$\mathbf{x}_t = [1, \, t, \, \text{dow}_t, \, \text{isWeekend}_t, \, y_{t-1}, \, y_{t-7}, \, \bar{y}_{t-7..t-1}]^T$$
Where:
- $t$: Linear trend index ($0, 1, 2, \dots$)
- $\text{dow}_t$: Day of week ($1=\text{Mon} \dots 7=\text{Sun}$)
- $\text{isWeekend}_t$: Binary indicator ($1$ if Sat/Sun, else $0$)
- $y_{t-1}$: Autoregressive lag 1 (yesterday's revenue)
- $y_{t-7}$: Seasonal lag 7 (same day last week)
- $\bar{y}_{t-7..t-1}$: 7-day rolling moving average

### 5.3 OLS Solution with Ridge Regularization
To guarantee matrix invertibility in collinear sales data:
$$\boldsymbol{\beta} = (\mathbf{X}^T \mathbf{X} + \lambda \mathbf{I})^{-1} \mathbf{X}^T \mathbf{y}$$
Where $\lambda = 10^{-4}$. Solved natively via Gaussian elimination with partial pivoting.

### 5.4 Recursive Multi-Step Forecasting & Confidence Bounds
For horizon day $h = 1 \dots H$:
$$\hat{y}_{t+h} = \mathbf{x}_{t+h}^T \boldsymbol{\beta}$$
Autoregressive features ($y_{t-1}, y_{t-7}$, moving average) update recursively with newly predicted values, and `SalesForecastingService` applies a `1.12` seasonal multiplier when `isWeekend > 0.5`. Model fit metrics (`RMSE`, `MAE`, `MAPE`, `R^2`) are computed in-sample (`lr.evaluate(X, y)`).
**Empirical $\pm 1.96 \cdot \text{RMSE}$ Bounds:**
$$\text{Lower} = \max\left(0, \, \hat{y} - 1.96 \cdot \text{RMSE}\right), \quad \text{Upper} = \hat{y} + 1.96 \cdot \text{RMSE}$$
