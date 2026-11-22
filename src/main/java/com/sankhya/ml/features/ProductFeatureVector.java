package com.sankhya.ml.features;

import com.sankhya.ml.enums.DemandClass;

public class ProductFeatureVector {
    private final Long productId;
    private final String productCode;
    private final String productName;
    private final String category;
    private final double totalUnitsSold;
    private final double dailyVelocity;
    private final double unitPrice;
    private final double currentStock;
    private final DemandClass label;

    public ProductFeatureVector(Long productId, String productCode, String productName, String category,
                                double totalUnitsSold, double dailyVelocity, double unitPrice,
                                double currentStock, DemandClass label) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.category = category;
        this.totalUnitsSold = totalUnitsSold;
        this.dailyVelocity = dailyVelocity;
        this.unitPrice = unitPrice;
        this.currentStock = currentStock;
        this.label = label;
    }

    public double[] toArray() {
        return new double[]{totalUnitsSold, dailyVelocity, unitPrice, currentStock};
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public double getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public double getDailyVelocity() {
        return dailyVelocity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getCurrentStock() {
        return currentStock;
    }

    public DemandClass getLabel() {
        return label;
    }
}
