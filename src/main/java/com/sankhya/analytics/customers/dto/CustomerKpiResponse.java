package com.sankhya.analytics.customers.dto;

import com.sankhya.analytics.common.dto.ComparisonMetric;
import java.math.BigDecimal;

public class CustomerKpiResponse {

    private ComparisonMetric totalCustomers;
    private ComparisonMetric activeCustomers;
    private ComparisonMetric newCustomers;
    private ComparisonMetric returningCustomers;
    private BigDecimal repeatPurchaseRate;
    private BigDecimal averageRevenuePerUser;

    public CustomerKpiResponse() {}

    public ComparisonMetric getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(ComparisonMetric totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public ComparisonMetric getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(ComparisonMetric activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public ComparisonMetric getNewCustomers() {
        return newCustomers;
    }

    public void setNewCustomers(ComparisonMetric newCustomers) {
        this.newCustomers = newCustomers;
    }

    public ComparisonMetric getReturningCustomers() {
        return returningCustomers;
    }

    public void setReturningCustomers(ComparisonMetric returningCustomers) {
        this.returningCustomers = returningCustomers;
    }

    public BigDecimal getRepeatPurchaseRate() {
        return repeatPurchaseRate;
    }

    public void setRepeatPurchaseRate(BigDecimal repeatPurchaseRate) {
        this.repeatPurchaseRate = repeatPurchaseRate;
    }

    public BigDecimal getAverageRevenuePerUser() {
        return averageRevenuePerUser;
    }

    public void setAverageRevenuePerUser(BigDecimal averageRevenuePerUser) {
        this.averageRevenuePerUser = averageRevenuePerUser;
    }
}
