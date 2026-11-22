package com.sankhya.analytics.inventory.dto;

import java.math.BigDecimal;

public class ReorderRecommendationDto {

    private String productId;
    private String productName;
    private String category;
    private int currentStock;
    private int reorderLevel;
    private int suggestedQuantity;
    private BigDecimal unitCost;
    private BigDecimal estimatedRestockCost;
    private String urgency; // CRITICAL, HIGH, MEDIUM

    public ReorderRecommendationDto() {}

    public ReorderRecommendationDto(String productId, String productName, String category,
                                   int currentStock, int reorderLevel, int suggestedQuantity,
                                   BigDecimal unitCost, BigDecimal estimatedRestockCost, String urgency) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.currentStock = currentStock;
        this.reorderLevel = reorderLevel;
        this.suggestedQuantity = suggestedQuantity;
        this.unitCost = unitCost;
        this.estimatedRestockCost = estimatedRestockCost;
        this.urgency = urgency;
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

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public int getSuggestedQuantity() {
        return suggestedQuantity;
    }

    public void setSuggestedQuantity(int suggestedQuantity) {
        this.suggestedQuantity = suggestedQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getEstimatedRestockCost() {
        return estimatedRestockCost;
    }

    public void setEstimatedRestockCost(BigDecimal estimatedRestockCost) {
        this.estimatedRestockCost = estimatedRestockCost;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }
}
