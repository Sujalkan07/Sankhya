package com.sankhya.analytics.sales.dto;

import java.math.BigDecimal;

public class TopSellingProductDto {

    private String productId;
    private String productName;
    private String category;
    private long unitsSold;
    private BigDecimal revenue;
    private BigDecimal grossProfit;

    public TopSellingProductDto() {}

    public TopSellingProductDto(String productId, String productName, String category,
                                long unitsSold, BigDecimal revenue, BigDecimal grossProfit) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.unitsSold = unitsSold;
        this.revenue = revenue;
        this.grossProfit = grossProfit;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(long unitsSold) {
        this.unitsSold = unitsSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }
}
