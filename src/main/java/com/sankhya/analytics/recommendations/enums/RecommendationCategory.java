package com.sankhya.analytics.recommendations.enums;

public enum RecommendationCategory {
    SALES("Sales Optimization"),
    INVENTORY("Inventory & Replenishment"),
    PRODUCT("Product & Assortment"),
    CUSTOMER("Customer Retention"),
    PERFORMANCE("Executive Performance");

    private final String displayName;

    RecommendationCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
