package com.sankhya.analytics.sales.service;

import com.sankhya.analytics.common.dto.ChartDataResponse;
import com.sankhya.analytics.common.dto.ComparisonMetric;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import com.sankhya.analytics.sales.dto.*;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.common.cache.AnalyticsCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
public class SalesAnalyticsService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;
    private final AnalyticsCacheService cacheService;

    public SalesAnalyticsService(SalesTransactionRepository salesTransactionRepository,
                                 UnifiedSalesMartRepository unifiedSalesMartRepository) {
        this(salesTransactionRepository, unifiedSalesMartRepository, new AnalyticsCacheService());
    }

    @Autowired
    public SalesAnalyticsService(SalesTransactionRepository salesTransactionRepository,
                                 UnifiedSalesMartRepository unifiedSalesMartRepository,
                                 AnalyticsCacheService cacheService) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
        this.cacheService = cacheService != null ? cacheService : new AnalyticsCacheService();
    }

    @Transactional(readOnly = true)
    public SalesAnalyticsResponse getSalesAnalytics(Long workspaceId, DateRangeFilter filter) {
        if (workspaceId == null || filter == null) {
            return computeSalesAnalytics(workspaceId, filter);
        }
        String key = "ws-" + workspaceId + ":" + filter.getPreset() + ":" + filter.getStartDate() + ":" + filter.getEndDate();
        return cacheService.getOrCompute("sales", key, Duration.ofMinutes(10),
                () -> computeSalesAnalytics(workspaceId, filter));
    }

    private SalesAnalyticsResponse computeSalesAnalytics(Long workspaceId, DateRangeFilter filter) {
        SalesAnalyticsResponse response = new SalesAnalyticsResponse();

        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;

        // 1. Current Period KPIs
        BigDecimal currRevenue;
        long currTxns;
        long currUnits;
        long currLines = 0;
        BigDecimal currDiscounts;

        DateRangeFilter prevFilter = filter.getPrecedingComparisonFilter();
        BigDecimal prevRevenue;
        long prevTxns;
        long prevUnits;
        long prevLines = 0;
        BigDecimal prevDiscounts;

        List<Object[]> dailyData;
        List<Object[]> pmtData;
        List<Object[]> catData;
        List<Object[]> hourlyData;
        List<Object[]> prodData;

        if (useMart) {
            currRevenue = unifiedSalesMartRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currRevenue == null) currRevenue = BigDecimal.ZERO;

            currTxns = unifiedSalesMartRepository.countDistinctTransactionsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            currLines = unifiedSalesMartRepository.countLinesBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            Long currUnitsLong = unifiedSalesMartRepository.sumQuantityBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            currUnits = currUnitsLong != null ? currUnitsLong : 0L;

            currDiscounts = unifiedSalesMartRepository.sumDiscountBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currDiscounts == null) currDiscounts = BigDecimal.ZERO;

            prevRevenue = unifiedSalesMartRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;

            prevTxns = unifiedSalesMartRepository.countDistinctTransactionsBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());

            prevLines = unifiedSalesMartRepository.countLinesBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());

            Long prevUnitsLong = unifiedSalesMartRepository.sumQuantityBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            prevUnits = prevUnitsLong != null ? prevUnitsLong : 0L;

            prevDiscounts = unifiedSalesMartRepository.sumDiscountBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            if (prevDiscounts == null) prevDiscounts = BigDecimal.ZERO;

            dailyData = unifiedSalesMartRepository.findDailyAggregationsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            pmtData = unifiedSalesMartRepository.findPaymentMethodDistributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            catData = unifiedSalesMartRepository.findCategorySalesContributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            hourlyData = unifiedSalesMartRepository.findHourlySalesDistributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            prodData = unifiedSalesMartRepository.findProductPerformanceBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        } else {
            currRevenue = salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currRevenue == null) currRevenue = BigDecimal.ZERO;

            currTxns = salesTransactionRepository.countDistinctTransactionsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            Long currUnitsLong = salesTransactionRepository.sumQuantityBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            currUnits = currUnitsLong != null ? currUnitsLong : 0L;

            currDiscounts = salesTransactionRepository.sumDiscountBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
            if (currDiscounts == null) currDiscounts = BigDecimal.ZERO;

            prevRevenue = salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;

            prevTxns = salesTransactionRepository.countDistinctTransactionsBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());

            Long prevUnitsLong = salesTransactionRepository.sumQuantityBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            prevUnits = prevUnitsLong != null ? prevUnitsLong : 0L;

            prevDiscounts = salesTransactionRepository.sumDiscountBetween(
                    workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
            if (prevDiscounts == null) prevDiscounts = BigDecimal.ZERO;

            dailyData = salesTransactionRepository.findDailyAggregationsBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            pmtData = salesTransactionRepository.findPaymentMethodDistributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            catData = salesTransactionRepository.findCategorySalesContributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            hourlyData = salesTransactionRepository.findHourlySalesDistributionBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

            prodData = salesTransactionRepository.findProductPerformanceBetween(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        }

        BigDecimal currAtv = currTxns > 0
                ? AnalyticsMathUtils.safeDivide(currRevenue, BigDecimal.valueOf(currTxns))
                : BigDecimal.ZERO;

        BigDecimal currBasketSize = currTxns > 0
                ? AnalyticsMathUtils.safeDivide(
                        BigDecimal.valueOf(useMart ? currLines : currUnits),
                        BigDecimal.valueOf(currTxns))
                : BigDecimal.ZERO;

        BigDecimal prevAtv = prevTxns > 0
                ? AnalyticsMathUtils.safeDivide(prevRevenue, BigDecimal.valueOf(prevTxns))
                : BigDecimal.ZERO;

        BigDecimal prevBasketSize = prevTxns > 0
                ? AnalyticsMathUtils.safeDivide(
                        BigDecimal.valueOf(useMart ? prevLines : prevUnits),
                        BigDecimal.valueOf(prevTxns))
                : BigDecimal.ZERO;

        // 3. Assemble KPIs
        SalesKpiResponse kpis = new SalesKpiResponse();
        kpis.setTotalRevenue(AnalyticsMathUtils.calculateComparison(
                currRevenue, prevRevenue, AnalyticsMathUtils.formatInr(currRevenue)));
        kpis.setTotalTransactions(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(currTxns), BigDecimal.valueOf(prevTxns), String.valueOf(currTxns)));
        kpis.setTotalUnitsSold(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(currUnits), BigDecimal.valueOf(prevUnits), String.valueOf(currUnits)));
        kpis.setAverageTransactionValue(AnalyticsMathUtils.calculateComparison(
                currAtv, prevAtv, AnalyticsMathUtils.formatInr(currAtv)));
        kpis.setAverageBasketSize(AnalyticsMathUtils.calculateComparison(
                currBasketSize, prevBasketSize, AnalyticsMathUtils.roundMoney(currBasketSize) + " items"));
        kpis.setTotalDiscounts(AnalyticsMathUtils.calculateComparison(
                currDiscounts, prevDiscounts, AnalyticsMathUtils.formatInr(currDiscounts)));
        response.setKpis(kpis);

        // 4. Daily Sales Trend Chart
        ChartDataResponse trendChart = new ChartDataResponse();
        List<BigDecimal> trendRevValues = new ArrayList<>();
        List<BigDecimal> trendTxnValues = new ArrayList<>();
        for (Object[] row : dailyData) {
            trendChart.getLabels().add(row[0].toString());
            trendRevValues.add((BigDecimal) row[1]);
            trendTxnValues.add(BigDecimal.valueOf(((Number) row[2]).longValue()));
        }
        trendChart.addDataset("Revenue", trendRevValues);
        trendChart.addDataset("Transactions", trendTxnValues);
        response.setSalesTrendChart(trendChart);

        // 5. Payment Methods Distribution
        List<PaymentMethodShareDto> pmtList = new ArrayList<>();
        ChartDataResponse pmtChart = new ChartDataResponse();
        List<BigDecimal> pmtChartValues = new ArrayList<>();

        for (Object[] row : pmtData) {
            String method = (String) row[0];
            if (method == null || method.trim().isEmpty()) method = "Unknown";
            long count = ((Number) row[1]).longValue();
            BigDecimal rev = (BigDecimal) row[2];
            BigDecimal share = currRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? AnalyticsMathUtils.safePercentage(rev, currRevenue)
                    : BigDecimal.ZERO;

            pmtList.add(new PaymentMethodShareDto(method, count, rev, share));
            pmtChart.getLabels().add(method);
            pmtChartValues.add(rev);
        }
        pmtChart.addDataset("Revenue Share", pmtChartValues);
        response.setPaymentMethods(pmtList);
        response.setPaymentMethodChart(pmtChart);

        // 6. Category Performance
        List<CategorySalesDto> catList = new ArrayList<>();
        ChartDataResponse catChart = new ChartDataResponse();
        List<BigDecimal> catChartValues = new ArrayList<>();

        for (Object[] row : catData) {
            String cat = (String) row[0];
            if (cat == null) cat = "Uncategorized";
            BigDecimal rev = (BigDecimal) row[1];
            long qty = ((Number) row[2]).longValue();
            long txns = ((Number) row[3]).longValue();
            BigDecimal cogs = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;
            BigDecimal profit = rev.subtract(cogs);
            BigDecimal margin = rev.compareTo(BigDecimal.ZERO) > 0
                    ? AnalyticsMathUtils.safePercentage(profit, rev)
                    : BigDecimal.ZERO;

            catList.add(new CategorySalesDto(cat, rev, qty, txns, cogs, profit, margin));
            catChart.getLabels().add(cat);
            catChartValues.add(rev);
        }
        catChart.addDataset("Category Revenue", catChartValues);
        response.setCategoryPerformance(catList);
        response.setCategoryChart(catChart);

        // 7. Hourly Sales Distribution (0 to 23 hours)
        Map<Integer, Object[]> hourMap = new HashMap<>();
        for (Object[] row : hourlyData) {
            if (row[0] != null) {
                int h = ((Number) row[0]).intValue();
                hourMap.put(h, row);
            }
        }

        List<HourlySalesDto> hourlyList = new ArrayList<>();
        ChartDataResponse hourlyChart = new ChartDataResponse();
        List<BigDecimal> hourlyRevValues = new ArrayList<>();

        for (int h = 0; h < 24; h++) {
            String label = String.format("%02d:00", h);
            hourlyChart.getLabels().add(label);
            if (hourMap.containsKey(h)) {
                Object[] r = hourMap.get(h);
                long txnCount = ((Number) r[1]).longValue();
                BigDecimal hRev = (BigDecimal) r[2];
                hourlyList.add(new HourlySalesDto(h, label, txnCount, hRev));
                hourlyRevValues.add(hRev);
            } else {
                hourlyList.add(new HourlySalesDto(h, label, 0L, BigDecimal.ZERO));
                hourlyRevValues.add(BigDecimal.ZERO);
            }
        }
        hourlyChart.addDataset("Hourly Revenue", hourlyRevValues);
        response.setHourlyDistribution(hourlyList);
        response.setHourlySalesChart(hourlyChart);

        // 8. Top Selling Products (Limit 10)
        List<TopSellingProductDto> topProds = new ArrayList<>();
        int count = 0;
        for (Object[] row : prodData) {
            if (count++ >= 10) break;
            String pId = (String) row[0];
            String pName = (String) row[1];
            String cat = (String) row[2];
            long qty = ((Number) row[3]).longValue();
            BigDecimal rev = (BigDecimal) row[4];
            BigDecimal cogs = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;
            BigDecimal profit = rev.subtract(cogs);

            topProds.add(new TopSellingProductDto(pId, pName, cat, qty, rev, profit));
        }
        response.setTopProducts(topProds);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<SalesTransaction> getSalesTransactionsPage(Long workspaceId, DateRangeFilter filter, Pageable pageable) {
        return salesTransactionRepository.findTransactionsInDateRange(
                workspaceId, filter.getStartDateTime(), filter.getEndDateTime(), pageable);
    }
}
