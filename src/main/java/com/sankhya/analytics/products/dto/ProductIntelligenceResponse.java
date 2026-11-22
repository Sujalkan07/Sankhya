package com.sankhya.analytics.products.dto;

import com.sankhya.analytics.common.dto.ChartDataResponse;

import java.util.ArrayList;
import java.util.List;

public class ProductIntelligenceResponse {

    private ProductKpiResponse kpis;
    private ChartDataResponse categoryProfitabilityChart;
    private List<CategoryProfitabilityDto> categoryProfitability = new ArrayList<>();
    private List<ProductPerformanceDto> topProductsByRevenue = new ArrayList<>();
    private List<ProductPerformanceDto> topProductsByVolume = new ArrayList<>();
    private List<ProductPerformanceDto> topProductsByMargin = new ArrayList<>();
    private List<ProductPerformanceDto> allProducts = new ArrayList<>();

    // Quadrant counts
    private long starCount;
    private long cashCowCount;
    private long opportunityCount;
    private long underperformerCount;

    public ProductIntelligenceResponse() {}

    public ProductKpiResponse getKpis() {
        return kpis;
    }

    public void setKpis(ProductKpiResponse kpis) {
        this.kpis = kpis;
    }

    public ChartDataResponse getCategoryProfitabilityChart() {
        return categoryProfitabilityChart;
    }

    public void setCategoryProfitabilityChart(ChartDataResponse categoryProfitabilityChart) {
        this.categoryProfitabilityChart = categoryProfitabilityChart;
    }

    public List<CategoryProfitabilityDto> getCategoryProfitability() {
        return categoryProfitability;
    }

    public void setCategoryProfitability(List<CategoryProfitabilityDto> categoryProfitability) {
        this.categoryProfitability = categoryProfitability;
    }

    public List<ProductPerformanceDto> getTopProductsByRevenue() {
        return topProductsByRevenue;
    }

    public void setTopProductsByRevenue(List<ProductPerformanceDto> topProductsByRevenue) {
        this.topProductsByRevenue = topProductsByRevenue;
    }

    public List<ProductPerformanceDto> getTopProductsByVolume() {
        return topProductsByVolume;
    }

    public void setTopProductsByVolume(List<ProductPerformanceDto> topProductsByVolume) {
        this.topProductsByVolume = topProductsByVolume;
    }

    public List<ProductPerformanceDto> getTopProductsByMargin() {
        return topProductsByMargin;
    }

    public void setTopProductsByMargin(List<ProductPerformanceDto> topProductsByMargin) {
        this.topProductsByMargin = topProductsByMargin;
    }

    public List<ProductPerformanceDto> getAllProducts() {
        return allProducts;
    }

    public void setAllProducts(List<ProductPerformanceDto> allProducts) {
        this.allProducts = allProducts;
    }

    public long getStarCount() {
        return starCount;
    }

    public void setStarCount(long starCount) {
        this.starCount = starCount;
    }

    public long getCashCowCount() {
        return cashCowCount;
    }

    public void setCashCowCount(long cashCowCount) {
        this.cashCowCount = cashCowCount;
    }

    public long getOpportunityCount() {
        return opportunityCount;
    }

    public void setOpportunityCount(long opportunityCount) {
        this.opportunityCount = opportunityCount;
    }

    public long getUnderperformerCount() {
        return underperformerCount;
    }

    public void setUnderperformerCount(long underperformerCount) {
        this.underperformerCount = underperformerCount;
    }
}
