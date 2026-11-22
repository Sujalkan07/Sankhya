package com.sankhya.analytics.products.dto;

import java.math.BigDecimal;

public class ProductKpiResponse {

    private long totalCatalogProducts;
    private long activeSellingProducts;
    private long totalUnitsSold;
    private BigDecimal overallGrossMarginPercentage;
    private String topPerformingCategory;

    public ProductKpiResponse() {}

    public long getTotalCatalogProducts() {
        return totalCatalogProducts;
    }

    public void setTotalCatalogProducts(long totalCatalogProducts) {
        this.totalCatalogProducts = totalCatalogProducts;
    }

    public long getActiveSellingProducts() {
        return activeSellingProducts;
    }

    public void setActiveSellingProducts(long activeSellingProducts) {
        this.activeSellingProducts = activeSellingProducts;
    }

    public long getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(long totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public BigDecimal getOverallGrossMarginPercentage() {
        return overallGrossMarginPercentage;
    }

    public void setOverallGrossMarginPercentage(BigDecimal overallGrossMarginPercentage) {
        this.overallGrossMarginPercentage = overallGrossMarginPercentage;
    }

    public String getTopPerformingCategory() {
        return topPerformingCategory;
    }

    public void setTopPerformingCategory(String topPerformingCategory) {
        this.topPerformingCategory = topPerformingCategory;
    }
}
