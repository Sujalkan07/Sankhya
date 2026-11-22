package com.sankhya.analytics.common.dto;

import java.math.BigDecimal;

public class TimeSeriesPoint {

    private String label;
    private BigDecimal value;
    private Long count;
    private Long volume;

    public TimeSeriesPoint() {}

    public TimeSeriesPoint(String label, BigDecimal value, Long count, Long volume) {
        this.label = label;
        this.value = value != null ? value : BigDecimal.ZERO;
        this.count = count != null ? count : 0L;
        this.volume = volume != null ? volume : 0L;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }
}
