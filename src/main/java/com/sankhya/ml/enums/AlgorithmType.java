package com.sankhya.ml.enums;

public enum AlgorithmType {
    K_MEANS("K-Means Customer Segmentation"),
    APRIORI("Apriori Market Basket Association"),
    RANDOM_FOREST("Random Forest Product Demand Classification"),
    LINEAR_REGRESSION("Linear Regression Sales Forecasting");

    private final String displayName;

    AlgorithmType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
