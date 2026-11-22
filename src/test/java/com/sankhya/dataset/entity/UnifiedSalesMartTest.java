package com.sankhya.dataset.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnifiedSalesMart Canonical Formula Tests")
class UnifiedSalesMartTest {

    @Test
    @DisplayName("calculateRevenue: quantity * unitPrice - discount (clamped to non-negative)")
    void testCalculateRevenue() {
        // Normal case: 2 units @ $60.25 with $5 discount = $115.50
        BigDecimal rev = UnifiedSalesMart.calculateRevenue(2, new BigDecimal("60.25"), new BigDecimal("5.00"));
        assertEquals(new BigDecimal("115.50"), rev);

        // Zero discount: 3 units @ $10.00 = $30.00
        BigDecimal revNoDisc = UnifiedSalesMart.calculateRevenue(3, new BigDecimal("10.00"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("30.00"), revNoDisc);

        // Null discount treated as 0: 1 unit @ $45.50 = $45.50
        BigDecimal revNullDisc = UnifiedSalesMart.calculateRevenue(1, new BigDecimal("45.50"), null);
        assertEquals(new BigDecimal("45.50"), revNullDisc);

        // Oversized discount clamped to 0: 1 unit @ $10 with $20 discount = 0
        BigDecimal revClamped = UnifiedSalesMart.calculateRevenue(1, new BigDecimal("10.00"), new BigDecimal("20.00"));
        assertEquals(BigDecimal.ZERO, revClamped);

        // Non-positive quantity returns 0
        assertEquals(BigDecimal.ZERO, UnifiedSalesMart.calculateRevenue(0, new BigDecimal("10.00"), BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, UnifiedSalesMart.calculateRevenue(-1, new BigDecimal("10.00"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("calculateGrossProfit: net revenue - (quantity * unitCost)")
    void testCalculateGrossProfit() {
        // Net revenue $115.50, 2 units @ $44.00 cost ($88.00) = $27.50 profit
        BigDecimal profit = UnifiedSalesMart.calculateGrossProfit(
                new BigDecimal("115.50"), 2, new BigDecimal("44.00"));
        assertEquals(new BigDecimal("27.50"), profit);

        // Zero cost
        BigDecimal profitZeroCost = UnifiedSalesMart.calculateGrossProfit(
                new BigDecimal("50.00"), 1, BigDecimal.ZERO);
        assertEquals(new BigDecimal("50.00"), profitZeroCost);
    }
}
