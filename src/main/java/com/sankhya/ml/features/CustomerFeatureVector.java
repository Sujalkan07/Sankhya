package com.sankhya.ml.features;

public class CustomerFeatureVector {
    private final Long customerId;
    private final String customerCode;
    private final double recency;
    private final double frequency;
    private final double monetary;
    private final double averageTransactionValue;
    private final double totalUnits;

    public CustomerFeatureVector(Long customerId, String customerCode, double recency, double frequency,
                                 double monetary, double averageTransactionValue, double totalUnits) {
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.recency = recency;
        this.frequency = frequency;
        this.monetary = monetary;
        this.averageTransactionValue = averageTransactionValue;
        this.totalUnits = totalUnits;
    }

    public double[] toArray() {
        return new double[]{recency, frequency, monetary, averageTransactionValue};
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public double getRecency() {
        return recency;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getMonetary() {
        return monetary;
    }

    public double getAverageTransactionValue() {
        return averageTransactionValue;
    }

    public double getTotalUnits() {
        return totalUnits;
    }
}
