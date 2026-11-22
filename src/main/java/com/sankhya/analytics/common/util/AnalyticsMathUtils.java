package com.sankhya.analytics.common.util;

import com.sankhya.analytics.common.dto.ComparisonMetric;
import com.sankhya.analytics.common.enums.TrendDirection;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AnalyticsMathUtils {

    private AnalyticsMathUtils() {}

    public static BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100.0).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal delta = current.subtract(previous);
        return delta.divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static double calculatePercentageChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double delta = (double) (current - previous);
        return Math.round((delta / previous * 100.0) * 100.0) / 100.0;
    }

    public static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal safePercentage(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return part.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static double calculatePercentage(BigDecimal part, BigDecimal total) {
        return safePercentage(part, total).doubleValue();
    }

    public static double calculatePercentage(long part, long total) {
        if (total <= 0) return 0.0;
        return Math.round(((double) part / total * 100.0) * 100.0) / 100.0;
    }

    public static ComparisonMetric<BigDecimal> calculateComparison(BigDecimal current, BigDecimal previous, String formattedCurrent) {
        return new ComparisonMetric<>(current, previous, formattedCurrent);
    }

    public static TrendDirection determineDirection(double percentageChange) {
        if (percentageChange > 0.05) return TrendDirection.UP;
        if (percentageChange < -0.05) return TrendDirection.DOWN;
        return TrendDirection.FLAT;
    }

    public static TrendDirection determineDirection(BigDecimal percentageChange) {
        if (percentageChange == null) return TrendDirection.FLAT;
        return determineDirection(percentageChange.doubleValue());
    }

    public static BigDecimal roundMoney(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
