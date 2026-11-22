package com.sankhya.analytics.inventory.dto;

import java.math.BigDecimal;

public class StockHealthDto {

    private long healthyCount;
    private long lowStockCount;
    private long outOfStockCount;
    private long overstockedCount;

    private BigDecimal healthyPercentage;
    private BigDecimal lowStockPercentage;
    private BigDecimal outOfStockPercentage;
    private BigDecimal overstockedPercentage;

    public StockHealthDto() {}

    public StockHealthDto(long healthyCount, long lowStockCount, long outOfStockCount, long overstockedCount,
                          BigDecimal healthyPercentage, BigDecimal lowStockPercentage,
                          BigDecimal outOfStockPercentage, BigDecimal overstockedPercentage) {
        this.healthyCount = healthyCount;
        this.lowStockCount = lowStockCount;
        this.outOfStockCount = outOfStockCount;
        this.overstockedCount = overstockedCount;
        this.healthyPercentage = healthyPercentage;
        this.lowStockPercentage = lowStockPercentage;
        this.outOfStockPercentage = outOfStockPercentage;
        this.overstockedPercentage = overstockedPercentage;
    }

    public long getHealthyCount() {
        return healthyCount;
    }

    public void setHealthyCount(long healthyCount) {
        this.healthyCount = healthyCount;
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

    public long getOverstockedCount() {
        return overstockedCount;
    }

    public void setOverstockedCount(long overstockedCount) {
        this.overstockedCount = overstockedCount;
    }

    public BigDecimal getHealthyPercentage() {
        return healthyPercentage;
    }

    public void setHealthyPercentage(BigDecimal healthyPercentage) {
        this.healthyPercentage = healthyPercentage;
    }

    public BigDecimal getLowStockPercentage() {
        return lowStockPercentage;
    }

    public void setLowStockPercentage(BigDecimal lowStockPercentage) {
        this.lowStockPercentage = lowStockPercentage;
    }

    public BigDecimal getOutOfStockPercentage() {
        return outOfStockPercentage;
    }

    public void setOutOfStockPercentage(BigDecimal outOfStockPercentage) {
        this.outOfStockPercentage = outOfStockPercentage;
    }

    public BigDecimal getOverstockedPercentage() {
        return overstockedPercentage;
    }

    public void setOverstockedPercentage(BigDecimal overstockedPercentage) {
        this.overstockedPercentage = overstockedPercentage;
    }
}
