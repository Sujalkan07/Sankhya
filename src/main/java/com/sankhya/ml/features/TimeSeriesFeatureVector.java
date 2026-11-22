package com.sankhya.ml.features;

import java.time.LocalDate;

public class TimeSeriesFeatureVector {
    private final LocalDate date;
    private final double revenue;
    private final double dayOfWeek;
    private final double isWeekend;
    private final double lag1;
    private final double lag7;
    private final double rolling7Avg;
    private final double trendIndex;

    public TimeSeriesFeatureVector(LocalDate date, double revenue, double dayOfWeek, double isWeekend,
                                  double lag1, double lag7, double rolling7Avg, double trendIndex) {
        this.date = date;
        this.revenue = revenue;
        this.dayOfWeek = dayOfWeek;
        this.isWeekend = isWeekend;
        this.lag1 = lag1;
        this.lag7 = lag7;
        this.rolling7Avg = rolling7Avg;
        this.trendIndex = trendIndex;
    }

    public double[] getFeatureArray() {
        return new double[]{trendIndex, dayOfWeek, isWeekend, lag1, lag7, rolling7Avg};
    }

    public LocalDate getDate() {
        return date;
    }

    public double getRevenue() {
        return revenue;
    }

    public double getDayOfWeek() {
        return dayOfWeek;
    }

    public double getIsWeekend() {
        return isWeekend;
    }

    public double getLag1() {
        return lag1;
    }

    public double getLag7() {
        return lag7;
    }

    public double getRolling7Avg() {
        return rolling7Avg;
    }

    public double getTrendIndex() {
        return trendIndex;
    }
}
