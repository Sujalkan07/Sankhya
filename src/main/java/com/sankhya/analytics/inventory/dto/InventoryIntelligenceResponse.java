package com.sankhya.analytics.inventory.dto;

import com.sankhya.analytics.common.dto.ChartDataResponse;

import java.util.ArrayList;
import java.util.List;

public class InventoryIntelligenceResponse {

    private InventoryKpiResponse kpis;
    private StockHealthDto stockHealth;
    private ChartDataResponse stockHealthChart;
    private ChartDataResponse stockMovementChart;
    private List<ReorderRecommendationDto> reorderRecommendations = new ArrayList<>();
    private List<DailyStockMovementDto> stockMovements = new ArrayList<>();

    public InventoryIntelligenceResponse() {}

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
}
