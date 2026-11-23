package com.sankhya.analytics.inventory.service;

import com.sankhya.analytics.common.dto.ChartDataResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import com.sankhya.analytics.inventory.dto.*;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.common.cache.AnalyticsCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryIntelligenceService {

    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ProductRepository productRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;
    private final AnalyticsCacheService cacheService;

    public InventoryIntelligenceService(InventorySnapshotRepository inventorySnapshotRepository,
                                        ProductRepository productRepository,
                                        SalesTransactionRepository salesTransactionRepository,
                                        UnifiedSalesMartRepository unifiedSalesMartRepository) {
        this(inventorySnapshotRepository, productRepository, salesTransactionRepository,
             unifiedSalesMartRepository, new AnalyticsCacheService());
    }

    @Autowired
    public InventoryIntelligenceService(InventorySnapshotRepository inventorySnapshotRepository,
                                        ProductRepository productRepository,
                                        SalesTransactionRepository salesTransactionRepository,
                                        UnifiedSalesMartRepository unifiedSalesMartRepository,
                                        AnalyticsCacheService cacheService) {
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.productRepository = productRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
        this.cacheService = cacheService != null ? cacheService : new AnalyticsCacheService();
    }

    @Transactional(readOnly = true)
    public InventoryIntelligenceResponse getInventoryIntelligence(Long workspaceId, DateRangeFilter filter) {
        if (workspaceId == null || filter == null) {
            return computeInventoryIntelligence(workspaceId, filter);
        }
        String key = "ws-" + workspaceId + ":" + filter.getPreset() + ":" + filter.getStartDate() + ":" + filter.getEndDate();
        return cacheService.getOrCompute("inventory", key, Duration.ofMinutes(10),
                () -> computeInventoryIntelligence(workspaceId, filter));
    }

    private InventoryIntelligenceResponse computeInventoryIntelligence(Long workspaceId, DateRangeFilter filter) {
        InventoryIntelligenceResponse response = new InventoryIntelligenceResponse();

        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;

        long healthyCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        long overstockedCount = 0;

        BigDecimal totalCostValuation = BigDecimal.ZERO;
        BigDecimal totalRetailValuation = BigDecimal.ZERO;

        List<ReorderRecommendationDto> reorders = new ArrayList<>();
        List<InventoryIntelligenceResponse.SnapshotSkuDto> snapshotItems = new ArrayList<>();
        String latestSnapDate = null;
        long belowReorderThresholdCount = 0;

        if (useMart) {
            // Deduplicated inventory position directly from canonical mart (single latest snapshot)
            List<Object[]> martInv = cacheService.getOrCompute("inventoryLatest", "ws-" + workspaceId, Duration.ofMinutes(15),
                    () -> unifiedSalesMartRepository.findLatestInventoryPerProductByWorkspaceId(workspaceId));

            for (Object[] row : martInv) {
                String pId = (String) row[0];
                String name = row[1] != null ? row[1].toString() : pId;
                String category = row[2] != null ? row[2].toString() : "General";
                BigDecimal unitCost = row[3] instanceof BigDecimal bd ? bd : (row[3] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO);
                BigDecimal sellingPrice = row[4] instanceof BigDecimal bd ? bd : (row[4] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO);
                String snapDate = row[5] != null ? row[5].toString() : "2026-11-22";
                if (latestSnapDate == null || snapDate.compareTo(latestSnapDate) > 0) {
                    latestSnapDate = snapDate;
                }
                int closing = row[6] != null ? ((Number) row[6]).intValue() : 0;
                int reorder = row[7] != null ? ((Number) row[7]).intValue() : 0;
                String rawStatus = row[8] != null ? row[8].toString().trim().toUpperCase() : "";

                BigDecimal skuCostValuation = closing > 0
                        ? unitCost.multiply(BigDecimal.valueOf(closing))
                        : BigDecimal.ZERO;
                if (closing > 0) {
                    totalCostValuation = totalCostValuation.add(skuCostValuation);
                    totalRetailValuation = totalRetailValuation.add(sellingPrice.multiply(BigDecimal.valueOf(closing)));
                }

                String resolvedStatus;
                if (!rawStatus.isEmpty()) {
                    resolvedStatus = switch (rawStatus) {
                        case "OUT_OF_STOCK" -> { outOfStockCount++; yield "OUT_OF_STOCK"; }
                        case "LOW_STOCK" -> { lowStockCount++; yield "LOW_STOCK"; }
                        case "OVERSTOCK" -> { overstockedCount++; yield "OVERSTOCK"; }
                        default -> { healthyCount++; yield "HEALTHY"; }
                    };
                } else if (closing <= 0) {
                    outOfStockCount++;
                    resolvedStatus = "OUT_OF_STOCK";
                } else if (closing <= reorder) {
                    lowStockCount++;
                    resolvedStatus = "LOW_STOCK";
                } else if (reorder > 0 && closing > 3 * reorder) {
                    overstockedCount++;
                    resolvedStatus = "OVERSTOCK";
                } else {
                    healthyCount++;
                    resolvedStatus = "HEALTHY";
                }

                if (closing <= reorder) {
                    belowReorderThresholdCount++;
                }

                snapshotItems.add(new InventoryIntelligenceResponse.SnapshotSkuDto(
                        pId, name, category, closing, reorder, unitCost, sellingPrice,
                        skuCostValuation, resolvedStatus, snapDate));

                boolean isExplicitLowOrOut = "LOW_STOCK".equals(resolvedStatus) || "OUT_OF_STOCK".equals(resolvedStatus);
                if (isExplicitLowOrOut || closing <= reorder) {
                    int suggested = Math.max((2 * reorder) - closing, Math.max(reorder, 10));
                    BigDecimal restockCost = unitCost.multiply(BigDecimal.valueOf(suggested));
                    String urgency;
                    if (closing <= 0 || "OUT_OF_STOCK".equals(resolvedStatus)) {
                        urgency = "CRITICAL";
                    } else if ("LOW_STOCK".equals(resolvedStatus) || closing <= Math.max(1, reorder / 2)) {
                        urgency = "HIGH";
                    } else {
                        urgency = "MEDIUM";
                    }

                    reorders.add(new ReorderRecommendationDto(
                            pId, name, category, closing, reorder, suggested, unitCost, restockCost, urgency));
                }
            }
        } else {
            // Legacy multi-dataset fallback
            List<InventorySnapshot> latestSnapshots = inventorySnapshotRepository.findLatestSnapshotsByWorkspaceId(workspaceId);
            List<Product> products = productRepository.findByWorkspaceId(workspaceId);
            Map<String, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

            for (InventorySnapshot s : latestSnapshots) {
                Product p = productMap.get(s.getProductId());
                String name = p != null ? p.getProductName() : s.getProductId();
                String category = p != null ? p.getCategory() : "General";

                BigDecimal unitCost = s.getUnitCost() != null ? s.getUnitCost()
                        : (p != null && p.getUnitCost() != null ? p.getUnitCost() : BigDecimal.ZERO);
                BigDecimal sellingPrice = p != null && p.getSellingPrice() != null
                        ? p.getSellingPrice() : BigDecimal.ZERO;

                int closing = s.getClosingStock() != null ? s.getClosingStock() : 0;
                int reorder = s.getReorderLevel() != null ? s.getReorderLevel() : 0;

                if (closing > 0) {
                    totalCostValuation = totalCostValuation.add(unitCost.multiply(BigDecimal.valueOf(closing)));
                    totalRetailValuation = totalRetailValuation.add(sellingPrice.multiply(BigDecimal.valueOf(closing)));
                }

                if (closing <= 0) {
                    outOfStockCount++;
                } else if (closing <= reorder) {
                    lowStockCount++;
                } else if (reorder > 0 && closing > 3 * reorder) {
                    overstockedCount++;
                } else {
                    healthyCount++;
                }

                if (closing <= reorder) {
                    int suggested = Math.max((2 * reorder) - closing, Math.max(reorder, 10));
                    BigDecimal restockCost = unitCost.multiply(BigDecimal.valueOf(suggested));
                    String urgency;
                    if (closing <= 0) {
                        urgency = "CRITICAL";
                    } else if (closing <= Math.max(1, reorder / 2)) {
                        urgency = "HIGH";
                    } else {
                        urgency = "MEDIUM";
                    }

                    reorders.add(new ReorderRecommendationDto(
                            s.getProductId(), name, category, closing, reorder, suggested, unitCost, restockCost, urgency));
                }
            }
        }

        // Sort reorders by urgency priority (CRITICAL -> HIGH -> MEDIUM)
        reorders.sort((a, b) -> {
            int priorityA = "CRITICAL".equals(a.getUrgency()) ? 1 : ("HIGH".equals(a.getUrgency()) ? 2 : 3);
            int priorityB = "CRITICAL".equals(b.getUrgency()) ? 1 : ("HIGH".equals(b.getUrgency()) ? 2 : 3);
            return Integer.compare(priorityA, priorityB);
        });
        response.setReorderRecommendations(reorders);

        // 4. Stock Health DTO & Chart
        long totalSkus = healthyCount + lowStockCount + outOfStockCount + overstockedCount;
        BigDecimal healthyPct = totalSkus > 0 ? AnalyticsMathUtils.safePercentage(BigDecimal.valueOf(healthyCount), BigDecimal.valueOf(totalSkus)) : BigDecimal.ZERO;
        BigDecimal lowStockPct = totalSkus > 0 ? AnalyticsMathUtils.safePercentage(BigDecimal.valueOf(lowStockCount), BigDecimal.valueOf(totalSkus)) : BigDecimal.ZERO;
        BigDecimal outOfStockPct = totalSkus > 0 ? AnalyticsMathUtils.safePercentage(BigDecimal.valueOf(outOfStockCount), BigDecimal.valueOf(totalSkus)) : BigDecimal.ZERO;
        BigDecimal overstockedPct = totalSkus > 0 ? AnalyticsMathUtils.safePercentage(BigDecimal.valueOf(overstockedCount), BigDecimal.valueOf(totalSkus)) : BigDecimal.ZERO;

        StockHealthDto stockHealth = new StockHealthDto(
                healthyCount, lowStockCount, outOfStockCount, overstockedCount,
                healthyPct, lowStockPct, outOfStockPct, overstockedPct);
        response.setStockHealth(stockHealth);

        ChartDataResponse healthChart = new ChartDataResponse();
        healthChart.getLabels().addAll(List.of("Healthy", "Low Stock", "Out of Stock", "Overstocked"));
        healthChart.addDataset("SKU Health", List.of(
                BigDecimal.valueOf(healthyCount),
                BigDecimal.valueOf(lowStockCount),
                BigDecimal.valueOf(outOfStockCount),
                BigDecimal.valueOf(overstockedCount)
        ));
        response.setStockHealthChart(healthChart);

        // 5. Inventory Turnover Ratio
        BigDecimal cogs = useMart
                ? unifiedSalesMartRepository.sumEstimatedCogsBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime())
                : salesTransactionRepository.sumEstimatedCogsBetween(workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        if (cogs == null) cogs = BigDecimal.ZERO;

        BigDecimal turnover = totalCostValuation.compareTo(BigDecimal.ZERO) > 0
                ? AnalyticsMathUtils.safeDivide(cogs, totalCostValuation)
                : BigDecimal.ZERO;

        // 6. Assemble KPIs
        InventoryKpiResponse kpis = new InventoryKpiResponse();
        kpis.setTotalSkusTracked(totalSkus);
        kpis.setTotalCostValuation(totalCostValuation);
        kpis.setTotalRetailValuation(totalRetailValuation);
        kpis.setLowStockCount(lowStockCount);
        kpis.setOutOfStockCount(outOfStockCount);
        kpis.setInventoryTurnoverRatio(turnover);
        response.setKpis(kpis);

        // 7. Single-snapshot honesty vs legacy daily stock movement trends
        if (useMart) {
            response.setSingleSnapshotLimitation(true);
            response.setLatestSnapshotDate(latestSnapDate != null ? latestSnapDate : "2026-11-22");
            response.setBelowReorderThresholdCount(belowReorderThresholdCount);
            response.setStockMovements(Collections.emptyList());
            response.setStockMovementChart(null);
            snapshotItems.sort(Comparator
                    .comparing((InventoryIntelligenceResponse.SnapshotSkuDto s) ->
                            "OUT_OF_STOCK".equals(s.getStatus()) ? 0 :
                                    ("LOW_STOCK".equals(s.getStatus()) ? 1 :
                                            ("OVERSTOCK".equals(s.getStatus()) ? 2 : 3)))
                    .thenComparingInt(InventoryIntelligenceResponse.SnapshotSkuDto::getClosingStock));
            response.setSnapshotItems(snapshotItems);
        } else {
            LocalDate startDate = filter.getStartDateTime().toLocalDate();
            LocalDate endDate = filter.getEndDateTime().toLocalDate();
            List<Object[]> dailyMovements = inventorySnapshotRepository.findDailyMovementTotalsBetween(
                    workspaceId, startDate, endDate);

            List<DailyStockMovementDto> movements = new ArrayList<>();
            ChartDataResponse moveChart = new ChartDataResponse();
            List<BigDecimal> closingValues = new ArrayList<>();
            List<BigDecimal> receivedValues = new ArrayList<>();
            List<BigDecimal> soldValues = new ArrayList<>();

            for (Object[] row : dailyMovements) {
                String date = row[0].toString();
                long opening = ((Number) row[1]).longValue();
                long received = ((Number) row[2]).longValue();
                long sold = ((Number) row[3]).longValue();
                long closing = ((Number) row[4]).longValue();

                movements.add(new DailyStockMovementDto(date, opening, received, sold, closing));
                moveChart.getLabels().add(date);
                closingValues.add(BigDecimal.valueOf(closing));
                receivedValues.add(BigDecimal.valueOf(received));
                soldValues.add(BigDecimal.valueOf(sold));
            }

            moveChart.addDataset("Closing Stock", closingValues);
            moveChart.addDataset("Received Stock", receivedValues);
            moveChart.addDataset("Sold Stock", soldValues);

            response.setStockMovements(movements);
            response.setStockMovementChart(moveChart);
        }

        return response;
    }
}
