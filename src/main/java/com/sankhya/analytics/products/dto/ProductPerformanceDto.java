package com.sankhya.analytics.products.dto;

import java.math.BigDecimal;

public class ProductPerformanceDto {

    private String productId;
    private String productName;
    private String category;
    private long quantitySold;
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal grossProfit;
    private BigDecimal grossMarginPercentage;
    private String quadrant; // Star, Cash Cow, Opportunity, Underperformer

    public ProductPerformanceDto() {}

    public ProductPerformanceDto(String productId, String productName, String category,
                                 long quantitySold, BigDecimal revenue, BigDecimal cogs,
                                 BigDecimal grossProfit, BigDecimal grossMarginPercentage, String quadrant) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
        this.cogs = cogs;
        this.grossProfit = grossProfit;
        this.grossMarginPercentage = grossMarginPercentage;
        this.quadrant = quadrant;
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

    public long getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(long quantitySold) {
        this.quantitySold = quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getCogs() {
        return cogs;
    }

    public void setCogs(BigDecimal cogs) {
        this.cogs = cogs;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public BigDecimal getGrossMarginPercentage() {
        return grossMarginPercentage;
    }

    public void setGrossMarginPercentage(BigDecimal grossMarginPercentage) {
        this.grossMarginPercentage = grossMarginPercentage;
    }

    public String getQuadrant() {
        return quadrant;
    }

    public void setQuadrant(String quadrant) {
        this.quadrant = quadrant;
    }
}
