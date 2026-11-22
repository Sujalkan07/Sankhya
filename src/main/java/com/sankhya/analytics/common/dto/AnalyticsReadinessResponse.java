package com.sankhya.analytics.common.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsReadinessResponse {

    private boolean salesReady;
    private boolean customerReady;
    private boolean productReady;
    private boolean inventoryReady;
    private boolean recommendationsReady;
    private boolean overallReady;

    private long salesRecordCount;
    private long productRecordCount;
    private long customerRecordCount;
    private long inventoryRecordCount;

    private List<String> missingDependencies = new ArrayList<>();
    private List<String> dataQualityWarnings = new ArrayList<>();

    public AnalyticsReadinessResponse() {}

    public boolean isSalesReady() {
        return salesReady;
    }

    public void setSalesReady(boolean salesReady) {
        this.salesReady = salesReady;
    }

    public boolean isCustomerReady() {
        return customerReady;
    }

    public void setCustomerReady(boolean customerReady) {
        this.customerReady = customerReady;
    }

    public boolean isProductReady() {
        return productReady;
    }

    public void setProductReady(boolean productReady) {
        this.productReady = productReady;
    }

    public boolean isInventoryReady() {
        return inventoryReady;
    }

    public void setInventoryReady(boolean inventoryReady) {
        this.inventoryReady = inventoryReady;
    }

    public boolean isRecommendationsReady() {
        return recommendationsReady;
    }

    public void setRecommendationsReady(boolean recommendationsReady) {
        this.recommendationsReady = recommendationsReady;
    }

    public boolean isOverallReady() {
        return overallReady;
    }

    public void setOverallReady(boolean overallReady) {
        this.overallReady = overallReady;
    }

    public long getSalesRecordCount() {
        return salesRecordCount;
    }

    public void setSalesRecordCount(long salesRecordCount) {
        this.salesRecordCount = salesRecordCount;
    }

    public long getProductRecordCount() {
        return productRecordCount;
    }

    public void setProductRecordCount(long productRecordCount) {
        this.productRecordCount = productRecordCount;
    }

    public long getCustomerRecordCount() {
        return customerRecordCount;
    }

    public void setCustomerRecordCount(long customerRecordCount) {
        this.customerRecordCount = customerRecordCount;
    }

    public long getInventoryRecordCount() {
        return inventoryRecordCount;
    }

    public void setInventoryRecordCount(long inventoryRecordCount) {
        this.inventoryRecordCount = inventoryRecordCount;
    }

    public List<String> getMissingDependencies() {
        return missingDependencies;
    }

    public void setMissingDependencies(List<String> missingDependencies) {
        this.missingDependencies = missingDependencies;
    }

    public List<String> getDataQualityWarnings() {
        return dataQualityWarnings;
    }

    public void setDataQualityWarnings(List<String> dataQualityWarnings) {
        this.dataQualityWarnings = dataQualityWarnings;
    }
}
