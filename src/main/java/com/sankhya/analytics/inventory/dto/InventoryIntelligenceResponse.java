package com.sankhya.analytics.inventory.dto;

import com.sankhya.analytics.common.dto.ChartDataResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InventoryIntelligenceResponse {

    private InventoryKpiResponse kpis;
    private StockHealthDto stockHealth;
    private ChartDataResponse stockHealthChart;
    private ChartDataResponse stockMovementChart;
    private List<ReorderRecommendationDto> reorderRecommendations = new ArrayList<>();
    private List<DailyStockMovementDto> stockMovements = new ArrayList<>();
    private boolean singleSnapshotLimitation;
    private String latestSnapshotDate;
    private long belowReorderThresholdCount;
    private List<SnapshotSkuDto> snapshotItems = new ArrayList<>();

    public InventoryIntelligenceResponse() {}

    public static class SnapshotSkuDto {
        private String productId;
        private String productName;
        private String category;
        private int closingStock;
        private int reorderLevel;
        private BigDecimal unitCost;
        private BigDecimal sellingPrice;
        private BigDecimal stockCostValue;
        private String status;
        private String snapshotDate;

        public SnapshotSkuDto() {}

        public SnapshotSkuDto(String productId, String productName, String category,
                              int closingStock, int reorderLevel, BigDecimal unitCost,
                              BigDecimal sellingPrice, BigDecimal stockCostValue,
                              String status, String snapshotDate) {
            this.productId = productId;
            this.productName = productName;
            this.category = category;
            this.closingStock = closingStock;
            this.reorderLevel = reorderLevel;
            this.unitCost = unitCost;
            this.sellingPrice = sellingPrice;
            this.stockCostValue = stockCostValue;
            this.status = status;
            this.snapshotDate = snapshotDate;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCategory() { return category; }
        public int getClosingStock() { return closingStock; }
        public int getReorderLevel() { return reorderLevel; }
        public BigDecimal getUnitCost() { return unitCost; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public BigDecimal getStockCostValue() { return stockCostValue; }
        public String getStatus() { return status; }
        public String getSnapshotDate() { return snapshotDate; }
    }

    public InventoryKpiResponse getKpis() {
        return kpis;
    }

    public void setKpis(InventoryKpiResponse kpis) {
        this.kpis = kpis;
    }

    public StockHealthDto getStockHealth() {
        return stockHealth;
    }

    public void setStockHealth(StockHealthDto stockHealth) {
        this.stockHealth = stockHealth;
    }

    public ChartDataResponse getStockHealthChart() {
        return stockHealthChart;
    }

    public void setStockHealthChart(ChartDataResponse stockHealthChart) {
        this.stockHealthChart = stockHealthChart;
    }

    public ChartDataResponse getStockMovementChart() {
        return stockMovementChart;
    }

    public void setStockMovementChart(ChartDataResponse stockMovementChart) {
        this.stockMovementChart = stockMovementChart;
    }

    public List<ReorderRecommendationDto> getReorderRecommendations() {
        return reorderRecommendations;
    }

    public void setReorderRecommendations(List<ReorderRecommendationDto> reorderRecommendations) {
        this.reorderRecommendations = reorderRecommendations;
    }

    public List<DailyStockMovementDto> getStockMovements() {
        return stockMovements;
    }

    public void setStockMovements(List<DailyStockMovementDto> stockMovements) {
        this.stockMovements = stockMovements;
    }

    public boolean isSingleSnapshotLimitation() {
        return singleSnapshotLimitation;
    }

    public void setSingleSnapshotLimitation(boolean singleSnapshotLimitation) {
        this.singleSnapshotLimitation = singleSnapshotLimitation;
    }

    public String getLatestSnapshotDate() {
        return latestSnapshotDate;
    }

    public void setLatestSnapshotDate(String latestSnapshotDate) {
        this.latestSnapshotDate = latestSnapshotDate;
    }

    public long getBelowReorderThresholdCount() {
        return belowReorderThresholdCount;
    }

    public void setBelowReorderThresholdCount(long belowReorderThresholdCount) {
        this.belowReorderThresholdCount = belowReorderThresholdCount;
    }

    public List<SnapshotSkuDto> getSnapshotItems() {
        return snapshotItems;
    }

    public void setSnapshotItems(List<SnapshotSkuDto> snapshotItems) {
        this.snapshotItems = snapshotItems;
    }
}
