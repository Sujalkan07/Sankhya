package com.sankhya.analytics.sales.dto;

import com.sankhya.analytics.common.dto.ChartDataResponse;

import java.util.ArrayList;
import java.util.List;

public class SalesAnalyticsResponse {

    private SalesKpiResponse kpis;
    private ChartDataResponse salesTrendChart;
    private ChartDataResponse paymentMethodChart;
    private ChartDataResponse categoryChart;
    private ChartDataResponse hourlySalesChart;

    private List<PaymentMethodShareDto> paymentMethods = new ArrayList<>();
    private List<CategorySalesDto> categoryPerformance = new ArrayList<>();
    private List<HourlySalesDto> hourlyDistribution = new ArrayList<>();
    private List<TopSellingProductDto> topProducts = new ArrayList<>();

    public SalesAnalyticsResponse() {}

    public SalesKpiResponse getKpis() {
        return kpis;
    }

    public void setKpis(SalesKpiResponse kpis) {
        this.kpis = kpis;
    }

    public ChartDataResponse getSalesTrendChart() {
        return salesTrendChart;
    }

    public void setSalesTrendChart(ChartDataResponse salesTrendChart) {
        this.salesTrendChart = salesTrendChart;
    }

    public ChartDataResponse getPaymentMethodChart() {
        return paymentMethodChart;
    }

    public void setPaymentMethodChart(ChartDataResponse paymentMethodChart) {
        this.paymentMethodChart = paymentMethodChart;
    }

    public ChartDataResponse getCategoryChart() {
        return categoryChart;
    }

    public void setCategoryChart(ChartDataResponse categoryChart) {
        this.categoryChart = categoryChart;
    }

    public ChartDataResponse getHourlySalesChart() {
        return hourlySalesChart;
    }

    public void setHourlySalesChart(ChartDataResponse hourlySalesChart) {
        this.hourlySalesChart = hourlySalesChart;
    }

    public List<PaymentMethodShareDto> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethodShareDto> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public List<CategorySalesDto> getCategoryPerformance() {
        return categoryPerformance;
    }

    public void setCategoryPerformance(List<CategorySalesDto> categoryPerformance) {
        this.categoryPerformance = categoryPerformance;
    }

    public List<HourlySalesDto> getHourlyDistribution() {
        return hourlyDistribution;
    }

    public void setHourlyDistribution(List<HourlySalesDto> hourlyDistribution) {
        this.hourlyDistribution = hourlyDistribution;
    }

    public List<TopSellingProductDto> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<TopSellingProductDto> topProducts) {
        this.topProducts = topProducts;
    }
}
