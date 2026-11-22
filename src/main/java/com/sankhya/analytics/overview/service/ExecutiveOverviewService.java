package com.sankhya.analytics.overview.service;

import com.sankhya.analytics.common.dto.ChartDataResponse;
import com.sankhya.analytics.common.dto.ComparisonMetric;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.enums.RecommendationSeverity;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExecutiveOverviewService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final BusinessRecommendationRepository businessRecommendationRepository;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;

    public ExecutiveOverviewService(SalesTransactionRepository salesTransactionRepository,
                                    InventorySnapshotRepository inventorySnapshotRepository,
                                    BusinessRecommendationRepository businessRecommendationRepository,
                                    UnifiedSalesMartRepository unifiedSalesMartRepository) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.businessRecommendationRepository = businessRecommendationRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
    }

    @Transactional(readOnly = true)
    public ExecutiveOverviewResponse getExecutiveOverview(Long workspaceId, DateRangeFilter filter) {
        ExecutiveOverviewResponse response = new ExecutiveOverviewResponse();

        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;

        // 1. Current Window Metrics
        BigDecimal currRevenue;
        long currTxns;
        long currCustomers;
        BigDecimal currCogs;
        long lowStockCount;
        List<Object[]> dailyData;
        List<Object[]> categoryData;

        DateRangeFilter prevFilter = filter.getPrecedingComparisonFilter();
        BigDecimal prevRevenue;
        long prevTxns;
        long prevCustomers;

        if (useMart) {
            currRevenue = unifiedSalesMartRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currRevenue == null) currRevenue = BigDecimal.ZERO;

            currTxns = unifiedSalesMartRepository.countDistinctTransactionsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            currCustomers = unifiedSalesMartRepository.countActiveCustomersBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            currCogs = unifiedSalesMartRepository.sumEstimatedCogsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currCogs == null) currCogs = BigDecimal.ZERO;

            prevRevenue = unifiedSalesMartRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;

            prevTxns = unifiedSalesMartRepository.countDistinctTransactionsBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());

            prevCustomers = unifiedSalesMartRepository.countActiveCustomersBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());

            List<Object[]> invList = unifiedSalesMartRepository.findLatestInventoryPerProductByWorkspaceId(workspaceId);
            lowStockCount = invList.stream().filter(row -> {
                Number closing = (Number) row[6];
                Number reorder = (Number) row[7];
                return closing != null && reorder != null && closing.intValue() <= reorder.intValue();
            }).count();

            dailyData = unifiedSalesMartRepository.findDailyAggregationsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            categoryData = unifiedSalesMartRepository.findCategorySalesContributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        } else {
            currRevenue = salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currRevenue == null) currRevenue = BigDecimal.ZERO;

            currTxns = salesTransactionRepository.countDistinctTransactionsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            currCustomers = salesTransactionRepository.countActiveCustomersBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            currCogs = salesTransactionRepository.sumEstimatedCogsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currCogs == null) currCogs = BigDecimal.ZERO;

            prevRevenue = salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;

            prevTxns = salesTransactionRepository.countDistinctTransactionsBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());

            prevCustomers = salesTransactionRepository.countActiveCustomersBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());

            lowStockCount = inventorySnapshotRepository.findLowStockAlertsByWorkspaceId(workspaceId).size();

            dailyData = salesTransactionRepository.findDailyAggregationsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            categoryData = salesTransactionRepository.findCategorySalesContributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        }

        BigDecimal currAtv = currTxns > 0
                ? AnalyticsMathUtils.safeDivide(currRevenue, BigDecimal.valueOf(currTxns))
                : BigDecimal.ZERO;

        BigDecimal grossProfit = currRevenue.subtract(currCogs);
        BigDecimal grossMarginPct = currRevenue.compareTo(BigDecimal.ZERO) > 0
                ? AnalyticsMathUtils.safePercentage(grossProfit, currRevenue)
                : BigDecimal.ZERO;
        response.setGrossMarginPercentage(grossMarginPct);

        BigDecimal prevAtv = prevTxns > 0
                ? AnalyticsMathUtils.safeDivide(prevRevenue, BigDecimal.valueOf(prevTxns))
                : BigDecimal.ZERO;

        // 3. Populate Comparison Metrics
        response.setTotalRevenue(AnalyticsMathUtils.calculateComparison(
                currRevenue, prevRevenue, "$" + AnalyticsMathUtils.roundMoney(currRevenue)));
        response.setTotalTransactions(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(currTxns), BigDecimal.valueOf(prevTxns), String.valueOf(currTxns)));
        response.setAverageTransactionValue(AnalyticsMathUtils.calculateComparison(
                currAtv, prevAtv, "$" + AnalyticsMathUtils.roundMoney(currAtv)));
        response.setActiveCustomers(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(currCustomers), BigDecimal.valueOf(prevCustomers), String.valueOf(currCustomers)));

        // 4. Inventory Alerts Count
        response.setLowStockAlertCount(lowStockCount);

        // 5. Daily Sales Trend Chart
        ChartDataResponse trendChart = new ChartDataResponse();
        List<BigDecimal> trendValues = new ArrayList<>();
        for (Object[] row : dailyData) {
            trendChart.getLabels().add(row[0].toString());
            trendValues.add((BigDecimal) row[1]);
        }
        trendChart.addDataset("Daily Revenue", trendValues);
        response.setSalesTrendChart(trendChart);

        // 6. Category Revenue Distribution Chart
        ChartDataResponse catChart = new ChartDataResponse();
        List<BigDecimal> catValues = new ArrayList<>();
        String topCategoryName = null;
        BigDecimal topCategoryRevenue = BigDecimal.ZERO;

        for (Object[] row : categoryData) {
            String cat = (String) row[0];
            BigDecimal rev = (BigDecimal) row[1];
            catChart.getLabels().add(cat != null ? cat : "Uncategorized");
            catValues.add(rev);

            if (topCategoryName == null || rev.compareTo(topCategoryRevenue) > 0) {
                topCategoryName = cat;
                topCategoryRevenue = rev;
            }
        }
        catChart.addDataset("Category Revenue", catValues);
        response.setCategoryDistributionChart(catChart);

        // 7. Deterministic Descriptive Insights
        List<String> insights = new ArrayList<>();
        if (currRevenue.compareTo(BigDecimal.ZERO) > 0) {
            String revDirection = response.getTotalRevenue().getPercentageChange().compareTo(BigDecimal.ZERO) >= 0 ? "increased" : "decreased";
            insights.add(String.format("Total sales reached $%s (%s by %s%% compared to the previous period).",
                    AnalyticsMathUtils.roundMoney(currRevenue), revDirection, response.getTotalRevenue().getPercentageChange().abs()));
        } else {
            insights.add("No sales recorded for the selected date window.");
        }

        if (currTxns > 0) {
            insights.add(String.format("The supermarket recorded %d distinct customer transactions with an average basket value of $%s.",
                    currTxns, AnalyticsMathUtils.roundMoney(currAtv)));
        }

        if (topCategoryName != null && currRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal topCatShare = AnalyticsMathUtils.safePercentage(topCategoryRevenue, currRevenue);
            insights.add(String.format("'%s' is the top revenue-generating category, contributing %s%% ($%s) of total sales.",
                    topCategoryName, topCatShare, AnalyticsMathUtils.roundMoney(topCategoryRevenue)));
        }

        if (lowStockCount > 0) {
            insights.add(String.format("Attention required: %d inventory items are currently at or below their safety reorder threshold.",
                    lowStockCount));
        } else {
            insights.add("Inventory replenishment status is currently healthy with zero stockout alerts.");
        }

        response.setDescriptiveInsights(insights);

        // 8. Urgent Recommendations
        List<BusinessRecommendation> activeRecs = businessRecommendationRepository.findActiveByWorkspaceId(workspaceId);
        List<BusinessRecommendation> urgent = activeRecs.stream()
                .filter(r -> r.getSeverity() == RecommendationSeverity.CRITICAL || r.getSeverity() == RecommendationSeverity.HIGH)
                .limit(5)
                .toList();
        response.setUrgentRecommendations(urgent);

        return response;
    }
}
