package com.sankhya.analytics.sales.dto;

import java.math.BigDecimal;

public class HourlySalesDto {

    private int hour;
    private String hourFormatted;
    private long transactionCount;
    private BigDecimal revenue;

    public HourlySalesDto() {}

    public HourlySalesDto(int hour, String hourFormatted, long transactionCount, BigDecimal revenue) {
        this.hour = hour;
        this.hourFormatted = hourFormatted;
        this.transactionCount = transactionCount;
        this.revenue = revenue;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public String getHourFormatted() {
        return hourFormatted;
    }

    public void setHourFormatted(String hourFormatted) {
        this.hourFormatted = hourFormatted;
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
}
