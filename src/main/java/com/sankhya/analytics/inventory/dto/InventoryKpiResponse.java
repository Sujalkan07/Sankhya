package com.sankhya.analytics.inventory.dto;

import java.math.BigDecimal;

public class InventoryKpiResponse {

    private long totalSkusTracked;
    private BigDecimal totalCostValuation;
    private BigDecimal totalRetailValuation;
    private long lowStockCount;
    private long outOfStockCount;
    private BigDecimal inventoryTurnoverRatio;

    public InventoryKpiResponse() {}

    public long getTotalSkusTracked() {
        return totalSkusTracked;
    }

    public void setTotalSkusTracked(long totalSkusTracked) {
        this.totalSkusTracked = totalSkusTracked;
    }

    public BigDecimal getTotalCostValuation() {
        return totalCostValuation;
    }

    public void setTotalCostValuation(BigDecimal totalCostValuation) {
        this.totalCostValuation = totalCostValuation;
    }

    public BigDecimal getTotalRetailValuation() {
        return totalRetailValuation;
    }

    public void setTotalRetailValuation(BigDecimal totalRetailValuation) {
        this.totalRetailValuation = totalRetailValuation;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public BigDecimal getInventoryTurnoverRatio() {
        return inventoryTurnoverRatio;
    }

    public void setInventoryTurnoverRatio(BigDecimal inventoryTurnoverRatio) {
        this.inventoryTurnoverRatio = inventoryTurnoverRatio;
    }
}
