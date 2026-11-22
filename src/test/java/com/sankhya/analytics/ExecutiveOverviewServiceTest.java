package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.overview.service.ExecutiveOverviewService;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
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
@DisplayName("ExecutiveOverviewService Unit Tests")
class ExecutiveOverviewServiceTest {

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Mock
    private BusinessRecommendationRepository businessRecommendationRepository;

    private ExecutiveOverviewService executiveOverviewService;

    @BeforeEach
    void setUp() {
        executiveOverviewService = new ExecutiveOverviewService(
                salesTransactionRepository,
                inventorySnapshotRepository,
                businessRecommendationRepository
        );
    }

    @Test
    @DisplayName("getExecutiveOverview aggregates KPIs and comparisons accurately")
    void testGetExecutiveOverviewSuccess() {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);

        // Current period mocks
        when(salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(eq(workspaceId), any(), any()))
                .thenReturn(new BigDecimal("10000.00"), new BigDecimal("8000.00")); // current, prev
        when(salesTransactionRepository.countDistinctTransactionsBetween(eq(workspaceId), any(), any()))
                .thenReturn(200L, 160L); // current, prev
        when(salesTransactionRepository.countActiveCustomersBetween(eq(workspaceId), any(), any()))
                .thenReturn(150L, 120L); // current, prev
        when(salesTransactionRepository.sumEstimatedCogsBetween(eq(workspaceId), any(), any()))
                .thenReturn(new BigDecimal("6000.00"));

        // Low stock alerts
        when(inventorySnapshotRepository.findLowStockAlertsByWorkspaceId(workspaceId))
                .thenReturn(Collections.emptyList());

        // Daily trend
        when(salesTransactionRepository.findDailyAggregationsBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.now(), new BigDecimal("500.00"), 10L, 25L}));

        // Category contribution
        when(salesTransactionRepository.findCategorySalesContributionBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"Beverages", new BigDecimal("5000.00"), 100L, 50L, new BigDecimal("3000.00")}));

        when(businessRecommendationRepository.findActiveByWorkspaceId(workspaceId))
                .thenReturn(Collections.emptyList());

        ExecutiveOverviewResponse response = executiveOverviewService.getExecutiveOverview(workspaceId, filter);

        assertNotNull(response);
        assertEquals(new BigDecimal("10000.00"), response.getTotalRevenue().getCurrentValue());
        assertEquals(new BigDecimal("8000.00"), response.getTotalRevenue().getPreviousValue());
        assertEquals(new BigDecimal("25.00"), response.getTotalRevenue().getPercentageChange()); // (10000-8000)/8000 * 100 = 25%

        assertEquals(new BigDecimal("40.00"), response.getGrossMarginPercentage()); // (10000-6000)/10000 * 100 = 40%
        assertEquals(0, response.getLowStockAlertCount());

        assertFalse(response.getDescriptiveInsights().isEmpty());
        assertTrue(response.getDescriptiveInsights().stream().anyMatch(i -> i.contains("Beverages")));
    }

    @Test
    @DisplayName("getExecutiveOverview handles empty data gracefully without NPE")
    void testGetExecutiveOverviewEmptyData() {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);

        when(salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(eq(workspaceId), any(), any()))
                .thenReturn(null);
        when(salesTransactionRepository.countDistinctTransactionsBetween(eq(workspaceId), any(), any()))
                .thenReturn(0L);
        when(salesTransactionRepository.countActiveCustomersBetween(eq(workspaceId), any(), any()))
                .thenReturn(0L);
        when(salesTransactionRepository.sumEstimatedCogsBetween(eq(workspaceId), any(), any()))
                .thenReturn(null);

        when(inventorySnapshotRepository.findLowStockAlertsByWorkspaceId(workspaceId))
                .thenReturn(Collections.emptyList());
        when(salesTransactionRepository.findDailyAggregationsBetween(eq(workspaceId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(salesTransactionRepository.findCategorySalesContributionBetween(eq(workspaceId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(businessRecommendationRepository.findActiveByWorkspaceId(workspaceId))
                .thenReturn(Collections.emptyList());

        ExecutiveOverviewResponse response = executiveOverviewService.getExecutiveOverview(workspaceId, filter);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue().getCurrentValue());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getGrossMarginPercentage()));
        assertFalse(response.getDescriptiveInsights().isEmpty());
    }
}
