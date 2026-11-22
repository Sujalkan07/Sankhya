package com.sankhya.analytics;

import com.sankhya.analytics.common.enums.TrendDirection;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnalyticsMathUtils Unit Tests")
class AnalyticsMathUtilsTest {

    @Test
    @DisplayName("calculatePercentageChange calculates positive delta correctly")
    void testCalculatePercentageChangePositive() {
        BigDecimal current = new BigDecimal("150.00");
        BigDecimal previous = new BigDecimal("100.00");
        BigDecimal change = AnalyticsMathUtils.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("50.00"), change);
    }

    @Test
    @DisplayName("calculatePercentageChange calculates negative delta correctly")
    void testCalculatePercentageChangeNegative() {
        BigDecimal current = new BigDecimal("80.00");
        BigDecimal previous = new BigDecimal("100.00");
        BigDecimal change = AnalyticsMathUtils.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("-20.00"), change);
    }

    @Test
    @DisplayName("calculatePercentageChange handles zero previous baseline gracefully")
    void testCalculatePercentageChangeZeroPrevious() {
        BigDecimal current = new BigDecimal("100.00");
        BigDecimal previous = BigDecimal.ZERO;
        BigDecimal change = AnalyticsMathUtils.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("100.00"), change);

        // When both zero
        BigDecimal changeZero = AnalyticsMathUtils.calculatePercentageChange(BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), changeZero);
    }

    @Test
    @DisplayName("safeDivide handles division by zero and nulls without throwing ArithmeticException")
    void testSafeDivide() {
        assertEquals(new BigDecimal("2.50"), AnalyticsMathUtils.safeDivide(new BigDecimal("5.00"), new BigDecimal("2.00")));
        assertEquals(new BigDecimal("0.00"), AnalyticsMathUtils.safeDivide(new BigDecimal("5.00"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.00"), AnalyticsMathUtils.safeDivide(null, new BigDecimal("2.00")));
        assertEquals(new BigDecimal("0.00"), AnalyticsMathUtils.safeDivide(new BigDecimal("5.00"), null));
    }

    @Test
    @DisplayName("safePercentage calculates percentage of total correctly")
    void testSafePercentage() {
        BigDecimal part = new BigDecimal("25.00");
        BigDecimal total = new BigDecimal("100.00");
        assertEquals(new BigDecimal("25.00"), AnalyticsMathUtils.safePercentage(part, total));

        // Total <= 0
        assertEquals(new BigDecimal("0.00"), AnalyticsMathUtils.safePercentage(part, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("determineDirection returns UP, DOWN, or FLAT correctly")
    void testDetermineDirection() {
        assertEquals(TrendDirection.UP, AnalyticsMathUtils.determineDirection(5.0));
        assertEquals(TrendDirection.DOWN, AnalyticsMathUtils.determineDirection(-3.2));
        assertEquals(TrendDirection.FLAT, AnalyticsMathUtils.determineDirection(0.02));
        assertEquals(TrendDirection.FLAT, AnalyticsMathUtils.determineDirection(-0.02));
    }

    @Test
    @DisplayName("formatInr formats Indian rupee numbering accurately with lakh and crore separators")
    void testFormatInr() {
        assertEquals("₹0.00", AnalyticsMathUtils.formatInr(null));
        assertEquals("₹0.00", AnalyticsMathUtils.formatInr(BigDecimal.ZERO));
        assertEquals("₹1,250.00", AnalyticsMathUtils.formatInr(new BigDecimal("1250")));
        assertEquals("₹1,25,000.00", AnalyticsMathUtils.formatInr(new BigDecimal("125000")));
        assertEquals("₹18,25,42,617.45", AnalyticsMathUtils.formatInr(new BigDecimal("182542617.45")));
        assertEquals("-₹50,000.50", AnalyticsMathUtils.formatInr(new BigDecimal("-50000.50")));
    }
}
