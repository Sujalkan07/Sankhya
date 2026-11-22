package com.sankhya.analytics.recommendations.service;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.enums.RecommendationCategory;
import com.sankhya.analytics.recommendations.enums.RecommendationSeverity;
import com.sankhya.analytics.recommendations.enums.RecommendationStatus;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusinessRecommendationService {

    private final BusinessRecommendationRepository businessRecommendationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;

    public BusinessRecommendationService(BusinessRecommendationRepository businessRecommendationRepository,
                                         WorkspaceRepository workspaceRepository,
                                         SalesTransactionRepository salesTransactionRepository,
                                         InventorySnapshotRepository inventorySnapshotRepository,
                                         ProductRepository productRepository,
                                         CustomerRepository customerRepository,
                                         UnifiedSalesMartRepository unifiedSalesMartRepository) {
        this.businessRecommendationRepository = businessRecommendationRepository;
        this.workspaceRepository = workspaceRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
    }

    @Transactional
    public List<BusinessRecommendation> generateRecommendations(Long workspaceId, DateRangeFilter filter) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;
        List<BusinessRecommendation> generated = new ArrayList<>();

        // 1. RULE_STOCKOUT_RISK & 2. RULE_OVERSTOCK_HOLDING_COST
        if (useMart) {
            List<Object[]> martInv = unifiedSalesMartRepository.findLatestInventoryPerProductByWorkspaceId(workspaceId);
            for (Object[] row : martInv) {
                String pId = (String) row[0];
                String name = row[1] != null ? row[1].toString() : pId;
                int closing = row[6] != null ? ((Number) row[6]).intValue() : 0;
                int reorder = row[7] != null ? ((Number) row[7]).intValue() : 0;
                if (row[6] != null) {
                    evaluateInventoryRules(workspace, workspaceId, pId, name, closing, reorder, generated);
                }
            }
        } else {
            List<InventorySnapshot> snapshots = inventorySnapshotRepository.findLatestSnapshotsByWorkspaceId(workspaceId);
            List<Product> products = productRepository.findByWorkspaceId(workspaceId);
            Map<String, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

            for (InventorySnapshot s : snapshots) {
                String pId = s.getProductId();
                Product p = productMap.get(pId);
                String name = p != null ? p.getProductName() : pId;
                int closing = s.getClosingStock() != null ? s.getClosingStock() : 0;
                int reorder = s.getReorderLevel() != null ? s.getReorderLevel() : 0;
                evaluateInventoryRules(workspace, workspaceId, pId, name, closing, reorder, generated);
            }
        }

        // 3. RULE_TOP_PRODUCT_MARGIN_OPTIMIZATION
        List<Object[]> prodRows = useMart
                ? unifiedSalesMartRepository.findProductPerformanceBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime())
                : salesTransactionRepository.findProductPerformanceBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        for (Object[] row : prodRows) {
            String pId = (String) row[0];
            String pName = (String) row[1];
            long qty = ((Number) row[3]).longValue();
            BigDecimal rev = (BigDecimal) row[4];
            BigDecimal cogs = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;
            BigDecimal profit = rev.subtract(cogs);
            BigDecimal margin = rev.compareTo(BigDecimal.ZERO) > 0
                    ? AnalyticsMathUtils.safePercentage(profit, rev)
                    : BigDecimal.ZERO;

            if (qty >= 20 && margin.compareTo(BigDecimal.valueOf(20.0)) < 0) {
                String ruleId = "RULE_TOP_PRODUCT_MARGIN_OPTIMIZATION";
                if (!isAlreadyActive(workspaceId, ruleId, pId)) {
                    BusinessRecommendation rec = new BusinessRecommendation(
                            workspace,
                            RecommendationCategory.PRODUCT,
                            RecommendationSeverity.MEDIUM,
                            3,
                            ruleId,
                            "Optimize Margin: " + pName,
                            String.format("High-volume product '%s' sold %d units but yields only %s%% gross margin (%s profit on %s revenue).",
                                    pName, qty, margin, AnalyticsMathUtils.formatInr(profit), AnalyticsMathUtils.formatInr(rev)),
                            "Review supplier wholesale cost or consider a 3-5% retail price adjustment to improve category profitability.",
                            pId,
                            margin + "%",
                            "20.0%"
                    );
                    generated.add(businessRecommendationRepository.save(rec));
                }
            }
        }

        // 4. RULE_CUSTOMER_CHURN_RISK
        List<Object[]> rfmRows = useMart
                ? unifiedSalesMartRepository.findCustomerRfmMetricsBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime())
                : salesTransactionRepository.findCustomerRfmMetricsBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        LocalDateTime endDateTime = filter.getEndDateTime();

        for (Object[] row : rfmRows) {
            String cId = (String) row[0];
            if (cId == null || cId.trim().isEmpty()) continue;
            String displayName = useMart && row[1] != null ? row[1].toString() : cId;
            LocalDateTime lastTxn = useMart ? (LocalDateTime) row[4] : (LocalDateTime) row[1];
            if (lastTxn == null) continue;
            long recencyDays = Math.max(0, Duration.between(lastTxn, endDateTime).toDays());
            long freq = ((Number) (useMart ? row[5] : row[2])).longValue();
            BigDecimal spend = (BigDecimal) (useMart ? row[6] : row[3]);

            if (recencyDays > 30 && freq >= 3 && spend != null && spend.compareTo(BigDecimal.valueOf(150.0)) >= 0) {
                String ruleId = "RULE_CUSTOMER_CHURN_RISK";
                if (!isAlreadyActive(workspaceId, ruleId, cId)) {
                    BusinessRecommendation rec = new BusinessRecommendation(
                            workspace,
                            RecommendationCategory.CUSTOMER,
                            RecommendationSeverity.HIGH,
                            2,
                            ruleId,
                            "Win-Back High Value Customer: " + displayName,
                            String.format("Loyal customer %s (%s) previously completed %d purchases (%s spend) but has not returned in %d days.",
                                    displayName, cId, freq, AnalyticsMathUtils.formatInr(spend), recencyDays),
                            "Trigger a personalized SMS or email win-back campaign with a 10% re-engagement voucher.",
                            cId,
                            recencyDays + " days",
                            "30 days"
                    );
                    generated.add(businessRecommendationRepository.save(rec));
                }
            }
        }

        // 5. RULE_REVENUE_DECLINE_ALERT
        BigDecimal currRevenue = useMart
                ? unifiedSalesMartRepository.sumRevenueByWorkspaceIdAndDateBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime())
                : salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        if (currRevenue == null) currRevenue = BigDecimal.ZERO;

        DateRangeFilter prevFilter = filter.getPrecedingComparisonFilter();
        BigDecimal prevRevenue = useMart
                ? unifiedSalesMartRepository.sumRevenueByWorkspaceIdAndDateBetween(workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime())
                : salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
        if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;

        if (prevRevenue.compareTo(BigDecimal.valueOf(100.0)) > 0) {
            BigDecimal revDeltaPct = AnalyticsMathUtils.calculatePercentageChange(currRevenue, prevRevenue);
            if (revDeltaPct.compareTo(BigDecimal.valueOf(-10.0)) <= 0) {
                String ruleId = "RULE_REVENUE_DECLINE_ALERT";
                if (!isAlreadyActive(workspaceId, ruleId, "WORKSPACE_REVENUE")) {
                    BusinessRecommendation rec = new BusinessRecommendation(
                            workspace,
                            RecommendationCategory.SALES,
                            RecommendationSeverity.HIGH,
                            1,
                            ruleId,
                            "Revenue Decline Warning",
                            String.format("Revenue dropped by %s%% (%s vs prior period %s).",
                                    revDeltaPct.abs(), AnalyticsMathUtils.formatInr(currRevenue), AnalyticsMathUtils.formatInr(prevRevenue)),
                            "Examine sales drop across key categories and review basket sizes to diagnose footfall vs pricing issues.",
                            "WORKSPACE_REVENUE",
                            AnalyticsMathUtils.formatInr(currRevenue),
                            AnalyticsMathUtils.formatInr(prevRevenue)
                    );
                    generated.add(businessRecommendationRepository.save(rec));
                }
            }
        }

        // 6. RULE_CATEGORY_CONCENTRATION
        List<Object[]> catRows = useMart
                ? unifiedSalesMartRepository.findCategorySalesContributionBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime())
                : salesTransactionRepository.findCategorySalesContributionBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        for (Object[] row : catRows) {
            String cat = (String) row[0];
            BigDecimal rev = (BigDecimal) row[1];
            if (currRevenue.compareTo(BigDecimal.ZERO) > 0 && rev != null) {
                BigDecimal share = AnalyticsMathUtils.safePercentage(rev, currRevenue);
                if (share.compareTo(BigDecimal.valueOf(50.0)) >= 0 && catRows.size() > 1) {
                    String ruleId = "RULE_CATEGORY_CONCENTRATION";
                    if (!isAlreadyActive(workspaceId, ruleId, cat)) {
                        BusinessRecommendation rec = new BusinessRecommendation(
                                workspace,
                                RecommendationCategory.SALES,
                                RecommendationSeverity.LOW,
                                5,
                                ruleId,
                                "Category Concentration: " + cat,
                                String.format("Category '%s' accounts for %s%% (%s) of total store revenue.",
                                        cat, share, AnalyticsMathUtils.formatInr(rev)),
                                "Diversify sales mix by launching promotional campaigns for complementary departments.",
                                cat,
                                share + "%",
                                "50.0%"
                        );
                        generated.add(businessRecommendationRepository.save(rec));
                    }
                }
            }
        }

        // 7. RULE_REPEAT_PURCHASE_LOW
        long activeCustCount = useMart
                ? unifiedSalesMartRepository.countActiveCustomersBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime())
                : salesTransactionRepository.countActiveCustomersBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        if (activeCustCount >= 10) {
            List<String> returningIds = useMart
                    ? unifiedSalesMartRepository.findReturningCustomerIdsBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime())
                    : salesTransactionRepository.findReturningCustomerIdsBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            BigDecimal repeatRate = AnalyticsMathUtils.safePercentage(BigDecimal.valueOf(returningIds.size()), BigDecimal.valueOf(activeCustCount));
            if (repeatRate.compareTo(BigDecimal.valueOf(20.0)) < 0) {
                String ruleId = "RULE_REPEAT_PURCHASE_LOW";
                if (!isAlreadyActive(workspaceId, ruleId, "REPEAT_PURCHASE")) {
                    BusinessRecommendation rec = new BusinessRecommendation(
                            workspace,
                            RecommendationCategory.CUSTOMER,
                            RecommendationSeverity.MEDIUM,
                            3,
                            ruleId,
                            "Low Repeat Purchase Rate",
                            String.format("Repeat purchase rate is %s%% (%d of %d active customers completed multiple orders).",
                                    repeatRate, returningIds.size(), activeCustCount),
                            "Introduce a customer loyalty card or send bounce-back coupons valid within 7 days of initial purchase.",
                            "REPEAT_PURCHASE",
                            repeatRate + "%",
                            "20.0%"
                    );
                    generated.add(businessRecommendationRepository.save(rec));
                }
            }
        }

        return businessRecommendationRepository.findByWorkspaceIdOrderByPriorityAscGeneratedAtDesc(workspaceId);
    }

    private void evaluateInventoryRules(Workspace workspace, Long workspaceId, String pId, String name,
                                        int closing, int reorder, List<BusinessRecommendation> generated) {
        if (closing <= reorder) {
            String ruleId = "RULE_STOCKOUT_RISK";
            if (!isAlreadyActive(workspaceId, ruleId, pId)) {
                int suggested = Math.max((2 * reorder) - closing, Math.max(reorder, 10));
                RecommendationSeverity severity = closing <= 0 ? RecommendationSeverity.CRITICAL : RecommendationSeverity.HIGH;
                int priority = closing <= 0 ? 1 : 2;

                BusinessRecommendation rec = new BusinessRecommendation(
                        workspace,
                        RecommendationCategory.INVENTORY,
                        severity,
                        priority,
                        ruleId,
                        "Replenish Stock: " + name,
                        String.format("Product '%s' (SKU: %s) has closing stock of %d units, below safety threshold of %d units.",
                                name, pId, closing, reorder),
                        String.format("Place an immediate purchase order for %d units to prevent stockout and lost sales.", suggested),
                        pId,
                        String.valueOf(closing),
                        String.valueOf(reorder)
                );
                generated.add(businessRecommendationRepository.save(rec));
            }
        }

        if (reorder > 0 && closing > 3 * reorder) {
            String ruleId = "RULE_OVERSTOCK_HOLDING_COST";
            if (!isAlreadyActive(workspaceId, ruleId, pId)) {
                BusinessRecommendation rec = new BusinessRecommendation(
                        workspace,
                        RecommendationCategory.INVENTORY,
                        RecommendationSeverity.MEDIUM,
                        4,
                        ruleId,
                        "Excess Inventory: " + name,
                        String.format("Product '%s' (SKU: %s) has %d units in stock, exceeding 3x safety reorder level (%d units).",
                                name, pId, closing, reorder),
                        "Consider a promotional discount, feature display, or product bundle to improve inventory turnover.",
                        pId,
                        String.valueOf(closing),
                        String.valueOf(reorder * 3)
                );
                generated.add(businessRecommendationRepository.save(rec));
            }
        }
    }

    private boolean isAlreadyActive(Long workspaceId, String ruleId, String metric) {
        return businessRecommendationRepository.existsByWorkspaceIdAndRuleIdentifierAndSupportingMetricAndStatus(
                workspaceId, ruleId, metric, RecommendationStatus.NEW)
                || businessRecommendationRepository.existsByWorkspaceIdAndRuleIdentifierAndSupportingMetricAndStatus(
                workspaceId, ruleId, metric, RecommendationStatus.REVIEWED);
    }

    @Transactional(readOnly = true)
    public List<BusinessRecommendation> getRecommendations(Long workspaceId, RecommendationCategory category, RecommendationStatus status) {
        if (category != null) {
            return businessRecommendationRepository.findByWorkspaceIdAndCategoryOrderByPriorityAsc(workspaceId, category);
        }
        if (status != null) {
            return businessRecommendationRepository.findByWorkspaceIdAndStatusOrderByPriorityAsc(workspaceId, status);
        }
        return businessRecommendationRepository.findByWorkspaceIdOrderByPriorityAscGeneratedAtDesc(workspaceId);
    }

    @Transactional
    public BusinessRecommendation updateStatus(Long workspaceId, Long recommendationId, RecommendationStatus newStatus) {
        BusinessRecommendation rec = businessRecommendationRepository.findByIdAndWorkspaceId(recommendationId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found or unauthorized"));
        rec.setStatus(newStatus);
        return businessRecommendationRepository.save(rec);
    }
}
