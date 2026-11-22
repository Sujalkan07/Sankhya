package com.sankhya.analytics.overview.dto;

import com.sankhya.analytics.common.dto.ChartDataResponse;
import com.sankhya.analytics.common.dto.ComparisonMetric;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExecutiveOverviewResponse {

    private ComparisonMetric totalRevenue;
    private ComparisonMetric totalTransactions;
    private ComparisonMetric averageTransactionValue;
    private ComparisonMetric activeCustomers;
    private BigDecimal grossMarginPercentage;
    private long lowStockAlertCount;

    private ChartDataResponse salesTrendChart;
    private ChartDataResponse categoryDistributionChart;

    private List<String> descriptiveInsights = new ArrayList<>();
    private List<BusinessRecommendation> urgentRecommendations = new ArrayList<>();

    public ExecutiveOverviewResponse() {}

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

    public ComparisonMetric getAverageTransactionValue() {
        return averageTransactionValue;
    }

    public void setAverageTransactionValue(ComparisonMetric averageTransactionValue) {
        this.averageTransactionValue = averageTransactionValue;
    }

    public ComparisonMetric getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(ComparisonMetric activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public BigDecimal getGrossMarginPercentage() {
        return grossMarginPercentage;
    }

    public void setGrossMarginPercentage(BigDecimal grossMarginPercentage) {
        this.grossMarginPercentage = grossMarginPercentage;
    }

    public long getLowStockAlertCount() {
        return lowStockAlertCount;
    }

    public void setLowStockAlertCount(long lowStockAlertCount) {
        this.lowStockAlertCount = lowStockAlertCount;
    }

    public ChartDataResponse getSalesTrendChart() {
        return salesTrendChart;
    }

    public void setSalesTrendChart(ChartDataResponse salesTrendChart) {
        this.salesTrendChart = salesTrendChart;
    }

    public ChartDataResponse getCategoryDistributionChart() {
        return categoryDistributionChart;
    }

    public void setCategoryDistributionChart(ChartDataResponse categoryDistributionChart) {
        this.categoryDistributionChart = categoryDistributionChart;
    }

    public List<String> getDescriptiveInsights() {
        return descriptiveInsights;
    }

    public void setDescriptiveInsights(List<String> descriptiveInsights) {
        this.descriptiveInsights = descriptiveInsights;
    }

    public List<BusinessRecommendation> getUrgentRecommendations() {
        return urgentRecommendations;
    }

    public void setUrgentRecommendations(List<BusinessRecommendation> urgentRecommendations) {
        this.urgentRecommendations = urgentRecommendations;
    }
}
