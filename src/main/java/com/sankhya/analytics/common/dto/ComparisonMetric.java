package com.sankhya.analytics.common.dto;

import com.sankhya.analytics.common.enums.TrendDirection;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ComparisonMetric<T> {

    private final T currentValue;
    private final T previousValue;
    private final BigDecimal percentageChange;
    private final TrendDirection direction;
    private final String formattedDelta;
    private String formattedCurrent;

    public ComparisonMetric(BigDecimal current, BigDecimal previous) {
        this(current, previous, current != null ? AnalyticsMathUtils.formatInr(current) : "₹0.00");
    }

    @SuppressWarnings("unchecked")
    public ComparisonMetric(BigDecimal current, BigDecimal previous, String formattedCurrent) {
        this.currentValue = (T) (current != null ? current : BigDecimal.ZERO);
        this.previousValue = (T) (previous != null ? previous : BigDecimal.ZERO);
        this.formattedCurrent = formattedCurrent;

        BigDecimal pctChange = AnalyticsMathUtils.calculatePercentageChange(current, previous);
        this.percentageChange = pctChange;
        this.direction = AnalyticsMathUtils.determineDirection(pctChange);

        double diff = (current != null ? current.doubleValue() : 0.0) - (previous != null ? previous.doubleValue() : 0.0);
        this.formattedDelta = (diff >= 0 ? "+" : "") + String.format("%.2f", diff);
    }

    @SuppressWarnings("unchecked")
    public ComparisonMetric(long current, long previous, String formattedCurrent) {
        this.currentValue = (T) Long.valueOf(current);
        this.previousValue = (T) Long.valueOf(previous);
        this.formattedCurrent = formattedCurrent;

        double pctChangeDouble = AnalyticsMathUtils.calculatePercentageChange(current, previous);
        this.percentageChange = BigDecimal.valueOf(pctChangeDouble).setScale(2, RoundingMode.HALF_UP);
        this.direction = AnalyticsMathUtils.determineDirection(pctChangeDouble);

        long diff = current - previous;
        this.formattedDelta = (diff >= 0 ? "+" : "") + diff;
    }

    public T getCurrentValue() {
        return currentValue;
    }

    public T getPreviousValue() {
        return previousValue;
    }

    public BigDecimal getPercentageChange() {
        return percentageChange;
    }

    public TrendDirection getDirection() {
        return direction;
    }

    public String getFormattedDelta() {
        return formattedDelta;
    }

    public String getFormattedCurrent() {
        return formattedCurrent;
    }

    public void setFormattedCurrent(String formattedCurrent) {
        this.formattedCurrent = formattedCurrent;
    }
}
