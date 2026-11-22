package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.sales.dto.SalesAnalyticsResponse;
import com.sankhya.analytics.sales.service.SalesAnalyticsService;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesAnalyticsService Unit Tests")
class SalesAnalyticsServiceTest {

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private com.sankhya.dataset.repository.UnifiedSalesMartRepository unifiedSalesMartRepository;

    private SalesAnalyticsService salesAnalyticsService;

    @BeforeEach
    void setUp() {
        salesAnalyticsService = new SalesAnalyticsService(salesTransactionRepository, unifiedSalesMartRepository);
    }

    @Test
    @DisplayName("getSalesAnalytics calculates sales KPIs, hourly curve, and tender mix accurately")
    void testGetSalesAnalyticsSuccess() {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);

        when(salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(eq(workspaceId), any(), any()))
                .thenReturn(new BigDecimal("12000.00"), new BigDecimal("10000.00"));
        when(salesTransactionRepository.countDistinctTransactionsBetween(eq(workspaceId), any(), any()))
                .thenReturn(300L, 250L);
        when(salesTransactionRepository.sumQuantityBetween(eq(workspaceId), any(), any()))
                .thenReturn(900L, 750L);
        when(salesTransactionRepository.sumDiscountBetween(eq(workspaceId), any(), any()))
                .thenReturn(new BigDecimal("500.00"), new BigDecimal("400.00"));

        when(salesTransactionRepository.findDailyAggregationsBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.now(), new BigDecimal("1000.00"), 25L, 80L}));

        when(salesTransactionRepository.findPaymentMethodDistributionBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"UPI", 200L, new BigDecimal("8000.00")},
                        new Object[]{"Cash", 100L, new BigDecimal("4000.00")}
                ));

        when(salesTransactionRepository.findCategorySalesContributionBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"Dairy", new BigDecimal("7000.00"), 500L, 180L, new BigDecimal("4500.00")}
                ));

        when(salesTransactionRepository.findHourlySalesDistributionBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{10, 50L, new BigDecimal("2000.00")},
                        new Object[]{18, 90L, new BigDecimal("4500.00")}
                ));

        when(salesTransactionRepository.findProductPerformanceBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"PROD-1", "Full Cream Milk", "Dairy", 300L, new BigDecimal("3000.00"), new BigDecimal("2100.00")}
                ));

        SalesAnalyticsResponse response = salesAnalyticsService.getSalesAnalytics(workspaceId, filter);

        assertNotNull(response);
        assertNotNull(response.getKpis());
        assertEquals(new BigDecimal("12000.00"), response.getKpis().getTotalRevenue().getCurrentValue());
        assertEquals(new BigDecimal("40.00"), response.getKpis().getAverageTransactionValue().getCurrentValue()); // 12000 / 300 = 40.00
        assertEquals(new BigDecimal("3.00"), response.getKpis().getAverageBasketSize().getCurrentValue()); // 900 / 300 = 3.00

        // Hourly curve must have 24 hours (0-23)
        assertEquals(24, response.getHourlyDistribution().size());
        assertEquals(24, response.getHourlySalesChart().getLabels().size());

        // Payment methods
        assertEquals(2, response.getPaymentMethods().size());
        assertEquals(new BigDecimal("66.67"), response.getPaymentMethods().get(0).getPercentageShare()); // 8000/12000 = 66.67%

        // Top products
        assertEquals(1, response.getTopProducts().size());
        assertEquals("PROD-1", response.getTopProducts().get(0).getProductId());
        assertEquals(new BigDecimal("900.00"), response.getTopProducts().get(0).getGrossProfit());
    }
}
