package com.sankhya.analytics.products.service;

import com.sankhya.analytics.common.dto.ChartDataResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import com.sankhya.analytics.products.dto.*;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductIntelligenceService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final ProductRepository productRepository;

    public ProductIntelligenceService(SalesTransactionRepository salesTransactionRepository,
                                      ProductRepository productRepository) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ProductIntelligenceResponse getProductIntelligence(Long workspaceId, DateRangeFilter filter) {
        ProductIntelligenceResponse response = new ProductIntelligenceResponse();

        // 1. Catalog Products Count
        long totalCatalog = productRepository.countByWorkspaceId(workspaceId);

        // 2. Query Raw Product Performance
        List<Object[]> rawProdList = salesTransactionRepository.findProductPerformanceBetween(
                workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

        List<ProductPerformanceDto> prods = new ArrayList<>();
        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;
        long totalUnits = 0;

        for (Object[] row : rawProdList) {
            String pId = (String) row[0];
            String pName = (String) row[1];
            String cat = (String) row[2];
            long qty = ((Number) row[3]).longValue();
            BigDecimal rev = (BigDecimal) row[4];
            BigDecimal cogs = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;

            BigDecimal profit = rev.subtract(cogs);
            BigDecimal margin = rev.compareTo(BigDecimal.ZERO) > 0
                    ? AnalyticsMathUtils.safePercentage(profit, rev)
                    : BigDecimal.ZERO;

            prods.add(new ProductPerformanceDto(pId, pName, cat, qty, rev, cogs, profit, margin, null));

            totalRev = totalRev.add(rev);
            totalCogs = totalCogs.add(cogs);
            totalUnits += qty;
        }

        // 3. Compute Medians for Quadrant Matrix
        double medianVolume = 10.0;
        double medianMargin = 25.0;

        if (!prods.isEmpty()) {
            List<Long> volumes = prods.stream()
                    .map(ProductPerformanceDto::getQuantitySold)
                    .sorted()
                    .collect(Collectors.toList());
            medianVolume = volumes.get(volumes.size() / 2);

            List<BigDecimal> margins = prods.stream()
                    .map(ProductPerformanceDto::getGrossMarginPercentage)
                    .sorted()
                    .collect(Collectors.toList());
            medianMargin = margins.get(margins.size() / 2).doubleValue();
        }

        long starCount = 0;
        long cashCowCount = 0;
        long oppCount = 0;
        long underCount = 0;

        for (ProductPerformanceDto p : prods) {
            boolean highVol = p.getQuantitySold() >= medianVolume;
            boolean highMargin = p.getGrossMarginPercentage().doubleValue() >= medianMargin;

            if (highVol && highMargin) {
                p.setQuadrant("Star");
                starCount++;
            } else if (highVol) {
                p.setQuadrant("Cash Cow");
                cashCowCount++;
            } else if (highMargin) {
                p.setQuadrant("Opportunity");
                oppCount++;
            } else {
                p.setQuadrant("Underperformer");
                underCount++;
            }
        }

        response.setStarCount(starCount);
        response.setCashCowCount(cashCowCount);
        response.setOpportunityCount(oppCount);
        response.setUnderperformerCount(underCount);
        response.setAllProducts(prods);

        // 4. Rankings
        // By Revenue
        List<ProductPerformanceDto> topByRev = prods.stream()
                .sorted(Comparator.comparing(ProductPerformanceDto::getRevenue).reversed())
                .limit(10)
                .collect(Collectors.toList());
        response.setTopProductsByRevenue(topByRev);

        // By Volume
        List<ProductPerformanceDto> topByVol = prods.stream()
                .sorted(Comparator.comparingLong(ProductPerformanceDto::getQuantitySold).reversed())
                .limit(10)
                .collect(Collectors.toList());
        response.setTopProductsByVolume(topByVol);

        // By Margin
        List<ProductPerformanceDto> topByMargin = prods.stream()
                .sorted(Comparator.comparing(ProductPerformanceDto::getGrossMarginPercentage).reversed())
                .limit(10)
                .collect(Collectors.toList());
        response.setTopProductsByMargin(topByMargin);

        // 5. Category Profitability
        List<Object[]> catRows = salesTransactionRepository.findCategorySalesContributionBetween(
                workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

        Map<String, Long> categoryCatalogCounts = new HashMap<>();
        List<Object[]> catSummary = productRepository.findCategoryProductSummaryByWorkspaceId(workspaceId);
        for (Object[] r : catSummary) {
            String cat = (String) r[0];
            long count = ((Number) r[1]).longValue();
            categoryCatalogCounts.put(cat, count);
        }

        List<CategoryProfitabilityDto> catProfitList = new ArrayList<>();
        ChartDataResponse catChart = new ChartDataResponse();
        List<BigDecimal> catProfits = new ArrayList<>();
        String topCatName = "None";
        BigDecimal topCatRev = BigDecimal.ZERO;

        for (Object[] row : catRows) {
            String cat = (String) row[0];
            if (cat == null) cat = "Uncategorized";
            BigDecimal rev = (BigDecimal) row[1];
            long qty = ((Number) row[2]).longValue();
            BigDecimal cogs = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;
            BigDecimal profit = rev.subtract(cogs);
            BigDecimal margin = rev.compareTo(BigDecimal.ZERO) > 0
                    ? AnalyticsMathUtils.safePercentage(profit, rev)
                    : BigDecimal.ZERO;
            long skuCount = categoryCatalogCounts.getOrDefault(cat, 0L);

            catProfitList.add(new CategoryProfitabilityDto(cat, skuCount, qty, rev, cogs, profit, margin));
            catChart.getLabels().add(cat);
            catProfits.add(profit);

            if (rev.compareTo(topCatRev) > 0) {
                topCatRev = rev;
                topCatName = cat;
            }
        }
        catChart.addDataset("Gross Profit", catProfits);
        response.setCategoryProfitability(catProfitList);
        response.setCategoryProfitabilityChart(catChart);

        // 6. Overall KPIs
        ProductKpiResponse kpis = new ProductKpiResponse();
        kpis.setTotalCatalogProducts(totalCatalog);
        kpis.setActiveSellingProducts(prods.size());
        kpis.setTotalUnitsSold(totalUnits);
        BigDecimal totalProfit = totalRev.subtract(totalCogs);
        BigDecimal overallMargin = totalRev.compareTo(BigDecimal.ZERO) > 0
                ? AnalyticsMathUtils.safePercentage(totalProfit, totalRev)
                : BigDecimal.ZERO;
        kpis.setOverallGrossMarginPercentage(overallMargin);
        kpis.setTopPerformingCategory(topCatName);
        response.setKpis(kpis);

        return response;
    }
}
