package com.sankhya.analytics.customers.dto;

import java.math.BigDecimal;

public class CustomerSegmentSummaryDto {

    private String segmentName;
    private long customerCount;
    private BigDecimal percentageShare;
    private BigDecimal totalMonetary;
    private BigDecimal avgMonetary;

    public CustomerSegmentSummaryDto() {}

    public CustomerSegmentSummaryDto(String segmentName, long customerCount, BigDecimal percentageShare,
                                     BigDecimal totalMonetary, BigDecimal avgMonetary) {
        this.segmentName = segmentName;
        this.customerCount = customerCount;
        this.percentageShare = percentageShare;
        this.totalMonetary = totalMonetary;
        this.avgMonetary = avgMonetary;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public long getCustomerCount() {
        return customerCount;
    }

    public void setCustomerCount(long customerCount) {
        this.customerCount = customerCount;
    }

    public BigDecimal getPercentageShare() {
        return percentageShare;
    }

    public void setPercentageShare(BigDecimal percentageShare) {
        this.percentageShare = percentageShare;
    }

    public BigDecimal getTotalMonetary() {
        return totalMonetary;
    }

    public void setTotalMonetary(BigDecimal totalMonetary) {
        this.totalMonetary = totalMonetary;
    }

    public BigDecimal getAvgMonetary() {
        return avgMonetary;
    }

    public void setAvgMonetary(BigDecimal avgMonetary) {
        this.avgMonetary = avgMonetary;
    }
}
