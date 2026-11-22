package com.sankhya.analytics.common.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsReadinessResponse {

    private boolean salesReady;
    private boolean customerReady;
    private boolean productReady;
    private boolean inventoryReady;
    private boolean recommendationsReady;
    private boolean overallReady;

    // ML module-specific readiness flags
    private boolean aprioriReady;
    private boolean kmeansReady;
    private boolean randomForestReady;
    private boolean linearRegressionReady;

    private String dataSource = "NONE";
    private long salesRecordCount;
    private long productRecordCount;
    private long customerRecordCount;
    private long inventoryRecordCount;
    private long distinctTransactionCount;
    private LocalDateTime minTransactionDate;
    private LocalDateTime maxTransactionDate;

    private List<String> missingDependencies = new ArrayList<>();
    private List<String> dataQualityWarnings = new ArrayList<>();
    private Map<String, String> moduleBlockReasons = new LinkedHashMap<>();

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

    public boolean isAprioriReady() {
        return aprioriReady;
    }

    public void setAprioriReady(boolean aprioriReady) {
        this.aprioriReady = aprioriReady;
    }

    public boolean isKmeansReady() {
        return kmeansReady;
    }

    public void setKmeansReady(boolean kmeansReady) {
        this.kmeansReady = kmeansReady;
    }

    public boolean isRandomForestReady() {
        return randomForestReady;
    }

    public void setRandomForestReady(boolean randomForestReady) {
        this.randomForestReady = randomForestReady;
    }

    public boolean isLinearRegressionReady() {
        return linearRegressionReady;
    }

    public void setLinearRegressionReady(boolean linearRegressionReady) {
        this.linearRegressionReady = linearRegressionReady;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public long getDistinctTransactionCount() {
        return distinctTransactionCount;
    }

    public void setDistinctTransactionCount(long distinctTransactionCount) {
        this.distinctTransactionCount = distinctTransactionCount;
    }

    public LocalDateTime getMinTransactionDate() {
        return minTransactionDate;
    }

    public void setMinTransactionDate(LocalDateTime minTransactionDate) {
        this.minTransactionDate = minTransactionDate;
    }

    public LocalDateTime getMaxTransactionDate() {
        return maxTransactionDate;
    }

    public void setMaxTransactionDate(LocalDateTime maxTransactionDate) {
        this.maxTransactionDate = maxTransactionDate;
    }

    public Map<String, String> getModuleBlockReasons() {
        return moduleBlockReasons;
    }

    public void setModuleBlockReasons(Map<String, String> moduleBlockReasons) {
        this.moduleBlockReasons = moduleBlockReasons;
    }
}
