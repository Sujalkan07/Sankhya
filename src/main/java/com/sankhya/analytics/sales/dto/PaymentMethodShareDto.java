package com.sankhya.analytics.sales.dto;

import java.math.BigDecimal;

public class PaymentMethodShareDto {

    private String paymentMethod;
    private long transactionCount;
    private BigDecimal revenue;
    private BigDecimal percentageShare;

    public PaymentMethodShareDto() {}

    public PaymentMethodShareDto(String paymentMethod, long transactionCount, BigDecimal revenue, BigDecimal percentageShare) {
        this.paymentMethod = paymentMethod;
        this.transactionCount = transactionCount;
        this.revenue = revenue;
        this.percentageShare = percentageShare;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getPercentageShare() {
        return percentageShare;
    }

    public void setPercentageShare(BigDecimal percentageShare) {
        this.percentageShare = percentageShare;
    }
}
