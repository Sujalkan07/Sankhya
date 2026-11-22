package com.sankhya.analytics.products.dto;

import java.math.BigDecimal;

public class CategoryProfitabilityDto {

    private String category;
    private long skuCount;
    private long quantitySold;
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal grossProfit;
    private BigDecimal grossMarginPercentage;

    public CategoryProfitabilityDto() {}

    public CategoryProfitabilityDto(String category, long skuCount, long quantitySold,
                                    BigDecimal revenue, BigDecimal cogs,
                                    BigDecimal grossProfit, BigDecimal grossMarginPercentage) {
        this.category = category;
        this.skuCount = skuCount;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
        this.cogs = cogs;
        this.grossProfit = grossProfit;
        this.grossMarginPercentage = grossMarginPercentage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getSkuCount() {
        return skuCount;
    }

    public void setSkuCount(long skuCount) {
        this.skuCount = skuCount;
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
}
