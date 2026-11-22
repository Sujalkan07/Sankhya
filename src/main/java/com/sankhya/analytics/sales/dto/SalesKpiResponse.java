package com.sankhya.analytics.sales.dto;

import com.sankhya.analytics.common.dto.ComparisonMetric;
import java.math.BigDecimal;

public class SalesKpiResponse {

    private ComparisonMetric totalRevenue;
    private ComparisonMetric totalTransactions;
    private ComparisonMetric totalUnitsSold;
    private ComparisonMetric averageTransactionValue;
    private ComparisonMetric averageBasketSize;
    private ComparisonMetric totalDiscounts;

    public SalesKpiResponse() {}

    public ComparisonMetric getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(ComparisonMetric totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public ComparisonMetric getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(ComparisonMetric totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public ComparisonMetric getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(ComparisonMetric totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public ComparisonMetric getAverageTransactionValue() {
        return averageTransactionValue;
    }

    public void setAverageTransactionValue(ComparisonMetric averageTransactionValue) {
        this.averageTransactionValue = averageTransactionValue;
    }

    public ComparisonMetric getAverageBasketSize() {
        return averageBasketSize;
    }

    public void setAverageBasketSize(ComparisonMetric averageBasketSize) {
        this.averageBasketSize = averageBasketSize;
    }

    public ComparisonMetric getTotalDiscounts() {
        return totalDiscounts;
    }

    public void setTotalDiscounts(ComparisonMetric totalDiscounts) {
        this.totalDiscounts = totalDiscounts;
    }
}
