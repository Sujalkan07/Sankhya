package com.sankhya.analytics.customers.dto;

import java.math.BigDecimal;

public class RfmCustomerDto {

    private String customerId;
    private String customerName;
    private long recencyDays;
    private long frequency;
    private BigDecimal monetary;
    private int rScore;
    private int fScore;
    private int mScore;
    private String rfmScore;
    private String segment;

    public RfmCustomerDto() {}

    public RfmCustomerDto(String customerId, String customerName, long recencyDays, long frequency,
                          BigDecimal monetary, int rScore, int fScore, int mScore, String segment) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.recencyDays = recencyDays;
        this.frequency = frequency;
        this.monetary = monetary;
        this.rScore = rScore;
        this.fScore = fScore;
        this.mScore = mScore;
        this.rfmScore = "" + rScore + fScore + mScore;
        this.segment = segment;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public long getRecencyDays() {
        return recencyDays;
    }

    public void setRecencyDays(long recencyDays) {
        this.recencyDays = recencyDays;
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    public BigDecimal getMonetary() {
        return monetary;
    }

    public void setMonetary(BigDecimal monetary) {
        this.monetary = monetary;
    }

    public int getrScore() {
        return rScore;
    }

    public void setrScore(int rScore) {
        this.rScore = rScore;
        this.rfmScore = "" + this.rScore + this.fScore + this.mScore;
    }

    public int getfScore() {
        return fScore;
    }

    public void setfScore(int fScore) {
        this.fScore = fScore;
        this.rfmScore = "" + this.rScore + this.fScore + this.mScore;
    }

    public int getmScore() {
        return mScore;
    }

    public void setmScore(int mScore) {
        this.mScore = mScore;
        this.rfmScore = "" + this.rScore + this.fScore + this.mScore;
    }

    public String getRfmScore() {
        return rfmScore;
    }

    public void setRfmScore(String rfmScore) {
        this.rfmScore = rfmScore;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }
}
