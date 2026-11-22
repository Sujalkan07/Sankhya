package com.sankhya.analytics.sales.dto;

import java.math.BigDecimal;

public class CategorySalesDto {

    private String category;
    private BigDecimal revenue;
    private long unitsSold;
    private long transactionCount;
    private BigDecimal cogs;
    private BigDecimal grossProfit;
    private BigDecimal grossMarginPercentage;

    public CategorySalesDto() {}

    public CategorySalesDto(String category, BigDecimal revenue, long unitsSold, long transactionCount,
                            BigDecimal cogs, BigDecimal grossProfit, BigDecimal grossMarginPercentage) {
        this.category = category;
        this.revenue = revenue;
        this.unitsSold = unitsSold;
        this.transactionCount = transactionCount;
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

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public long getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(long unitsSold) {
        this.unitsSold = unitsSold;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
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
