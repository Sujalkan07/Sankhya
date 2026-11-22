# SANKHYA — MATHEMATICAL AND ML VALIDATION REPORT
**Phase:** Phase 6 Final Integration & Local Verification
**Engine:** Pure Java 21 LTS Native Implementation (Zero Python/C++ Dependencies)
**Status:** All Formulations Mathematically Validated & Tested

---

## 1. Business Intelligence & Financial Formulations

All analytical metrics are computed using `BigDecimal` with explicit scale and `RoundingMode.HALF_UP` to prevent floating-point inaccuracies.

### 1.1 Gross Margin Percentage
$$\text{Gross Margin} = \left(\frac{\text{Net Revenue} - \text{Cost of Goods Sold (COGS)}}{\text{Net Revenue}}\right) \times 100$$
- **Implementation:** `AnalyticsMathUtils.safePercentage(revenue.subtract(cogs), revenue)`
- **Zero Divisor Guard:** Returns `BigDecimal.ZERO.setScale(2)` if revenue is zero or negative.
- **Unit Test Verification:** Tested in `AnalyticsMathUtilsTest.testGrossMarginCalculation`.

### 1.2 Average Transaction Value (ATV / Basket Size)
$$\text{ATV} = \frac{\text{Total Net Sales}}{\text{Total Completed Transactions}}$$
- **Implementation:** `AnalyticsMathUtils.safeDivide(totalNetSales, BigDecimal.valueOf(transactionCount))`
- **Zero Divisor Guard:** Evaluates to `0.00` when transaction count is zero.

### 1.3 Period-over-Period Percentage Change ($\Delta\%$)
$$\Delta\% = \left(\frac{V_{\text{current}} - V_{\text{previous}}}{|V_{\text{previous}}|}\right) \times 100$$
- **Edge Cases Handled:**
  - If $V_{\text{previous}} = 0$ and $V_{\text{current}} > 0$: Returns $+100.00\%$.
  - If $V_{\text{previous}} = 0$ and $V_{\text{current}} = 0$: Returns $0.00\%$.
  - Negative values use $|V_{\text{previous}}|$ in the denominator to preserve trend sign.

### 1.4 Days of Inventory Remaining (DIR)
$$\text{DIR} = \frac{\text{Current On-Hand Stock Units}}{\text{Average Daily Demand Rate (30-day Rolling)}}$$
- **Stockout Alert Threshold:** Triggered when $\text{DIR} < \text{Supplier Lead Time (Days)} + \text{Safety Stock Buffer}$.

### 1.5 Stockout Risk Index (SRI)
$$\text{SRI} = \min\left(100.0, \; \frac{\text{Daily Velocity} \times \text{Lead Time}}{\text{Stock On Hand} + 1} \times 100\right)$$
- Normalizes inventory vulnerability to a $0 - 100$ score.

---

## 2. Machine Learning Algorithm Formulations

### 2.1 K-Means Customer Clustering (`KMeansService`)
- **Initialization:** K-Means++ seeding. First centroid $c_1$ chosen uniformly at random. Successive centroids chosen with probability:
  $$P(x) = \frac{D(x)^2}{\sum_{x' \in X} D(x')^2}$$
  where $D(x)$ is the shortest Euclidean distance from data point $x$ to already chosen centroids.
- **Euclidean Metric:**
  $$d(x, c_k) = \sqrt{\sum_{j=1}^p (x_j - c_{k,j})^2}$$
- **Centroid Recomputation:**
  $$c_k = \frac{1}{|S_k|} \sum_{x \in S_k} x$$
- **Convergence:** Terminates when $\sum_{k=1}^K \|c_k^{(t)} - c_k^{(t-1)}\|_2 < 10^{-4}$ or at 300 iterations.
- **Validation Metric (Silhouette Coefficient):**
  $$s(i) = \frac{b(i) - a(i)}{\max(a(i), b(i))}$$
  where $a(i)$ is the mean intra-cluster distance and $b(i)$ is the mean nearest-cluster distance.

### 2.2 Apriori Market Basket Association (`AprioriService`)
- **Support:**
  $$\text{Support}(X \implies Y) = \frac{|\{T \in D : X \cup Y \subseteq T\}|}{|D|}$$
- **Confidence:**
  $$\text{Confidence}(X \implies Y) = \frac{\text{Support}(X \cup Y)}{\text{Support}(X)}$$
- **Lift Ratio:**
  $$\text{Lift}(X \implies Y) = \frac{\text{Support}(X \cup Y)}{\text{Support}(X) \times \text{Support}(Y)}$$
  - $\text{Lift} > 1.0$: Strong positive purchasing correlation.
  - $\text{Lift} = 1.0$: Statistical independence.
  - $\text{Lift} < 1.0$: Negative substitute relationship.

### 2.3 Random Forest Product Demand Classification (`RandomForestClassifier`)
- **Splitting Criterion (Gini Impurity):**
  $$I_G(p) = 1 - \sum_{i=1}^C p_i^2$$
  Information gain $\Delta I_G$ maximized over $\lfloor\sqrt{M}\rfloor$ randomly sampled features.
- **Ensemble Aggregation:** 100 decision trees trained on bootstrap resamples (with replacement).
- **Out-of-Bag (OOB) Accuracy:** Evaluated against unselected samples ($\approx 36.8\%$ per tree) without data leakage.

### 2.4 Multiple Linear Regression Sales Forecasting (`MultipleLinearRegression`)
- **Closed-Form Ordinary Least Squares with Ridge Regularization:**
  $$\beta = (X^T X + \lambda I)^{-1} X^T Y$$
  where $\lambda = 10^{-6}$ provides numerical stability against singular/multicollinear feature matrices.
- **Goodness-of-Fit Evaluation:**
  - **$R^2$ (Coefficient of Determination):**
    $$R^2 = 1 - \frac{\sum_{i=1}^N (y_i - \hat{y}_i)^2}{\sum_{i=1}^N (y_i - \bar{y})^2}$$
  - **RMSE (Root Mean Squared Error):**
    $$\text{RMSE} = \sqrt{\frac{1}{N} \sum_{i=1}^N (y_i - \hat{y}_i)^2}$$
  - **MAE (Mean Absolute Error):**
    $$\text{MAE} = \frac{1}{N} \sum_{i=1}^N |y_i - \hat{y}_i|$$

---

## 3. Mathematical Verification Summary

| Metric / Algorithm | Theoretical Formulation | Java 21 Implementation Class | Status |
| :--- | :--- | :--- | :--- |
| **Gross Margin** | $(\text{Rev} - \text{COGS}) / \text{Rev}$ | `AnalyticsMathUtils` | **VERIFIED** |
| **Period Delta** | $\Delta / \|\text{Prev}\|$ | `AnalyticsMathUtils` | **VERIFIED** |
| **K-Means++** | $P(x) \propto D(x)^2$ | `KMeansService` | **VERIFIED** |
| **Apriori** | $L_k \times L_k$ join & prune | `AprioriService` | **VERIFIED** |
| **Random Forest** | Bagging + Gini Impurity | `RandomForestClassifier` | **VERIFIED** |
| **OLS Regression** | $(X^T X + \lambda I)^{-1} X^T Y$ | `MultipleLinearRegression` | **VERIFIED** |
