package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.inventory.dto.InventoryIntelligenceResponse;
import com.sankhya.analytics.inventory.service.InventoryIntelligenceService;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
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
@DisplayName("InventoryIntelligenceService Unit Tests")
class InventoryIntelligenceServiceTest {

    @Mock
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    private InventoryIntelligenceService inventoryIntelligenceService;

    @BeforeEach
    void setUp() {
        inventoryIntelligenceService = new InventoryIntelligenceService(
                inventorySnapshotRepository, productRepository, salesTransactionRepository);
    }

    @Test
    @DisplayName("getInventoryIntelligence classifies stock health, valuations and reorder suggestions")
    void testGetInventoryIntelligenceSuccess() {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);

        // Mock 4 snapshots:
        // 1. Healthy: closing 50, reorder 20
        // 2. Low Stock: closing 5, reorder 15 -> urgency HIGH, suggested = 2*15 - 5 = 25
        // 3. Out of Stock: closing 0, reorder 10 -> urgency CRITICAL, suggested = 20
        // 4. Overstocked: closing 100, reorder 20 (> 3*20)
        InventorySnapshot s1 = new InventorySnapshot(null, "INV-1", "PROD-1", LocalDate.now(), 60, 0, 10, 50, 20, "Warehouse A", null);
        s1.setUnitCost(new BigDecimal("10.00"));

        InventorySnapshot s2 = new InventorySnapshot(null, "INV-2", "PROD-2", LocalDate.now(), 20, 0, 15, 5, 15, "Warehouse A", null);
        s2.setUnitCost(new BigDecimal("20.00"));

        InventorySnapshot s3 = new InventorySnapshot(null, "INV-3", "PROD-3", LocalDate.now(), 5, 0, 5, 0, 10, "Warehouse A", null);
        s3.setUnitCost(new BigDecimal("15.00"));

        InventorySnapshot s4 = new InventorySnapshot(null, "INV-4", "PROD-4", LocalDate.now(), 100, 0, 0, 100, 20, "Warehouse A", null);
        s4.setUnitCost(new BigDecimal("5.00"));

        when(inventorySnapshotRepository.findLatestSnapshotsByWorkspaceId(workspaceId))
                .thenReturn(List.of(s1, s2, s3, s4));

        when(productRepository.findByWorkspaceId(workspaceId)).thenReturn(Collections.emptyList());

        when(salesTransactionRepository.sumEstimatedCogsBetween(eq(workspaceId), any(), any()))
                .thenReturn(new BigDecimal("2200.00"));

        when(inventorySnapshotRepository.findDailyMovementTotalsBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.now(), 185L, 0L, 30L, 155L}));

        InventoryIntelligenceResponse response = inventoryIntelligenceService.getInventoryIntelligence(workspaceId, filter);

        assertNotNull(response);
        assertEquals(4L, response.getKpis().getTotalSkusTracked());
        // Cost valuation = 50*10 + 5*20 + 0*15 + 100*5 = 500 + 100 + 0 + 500 = 1100.00
        assertEquals(new BigDecimal("1100.00"), response.getKpis().getTotalCostValuation());

        assertEquals(1L, response.getKpis().getLowStockCount());
        assertEquals(1L, response.getKpis().getOutOfStockCount());

        // Turnover = 2200 / 1100 = 2.00
        assertEquals(new BigDecimal("2.00"), response.getKpis().getInventoryTurnoverRatio());

        // Stock Health
        assertEquals(1L, response.getStockHealth().getHealthyCount());
        assertEquals(1L, response.getStockHealth().getLowStockCount());
        assertEquals(1L, response.getStockHealth().getOutOfStockCount());
        assertEquals(1L, response.getStockHealth().getOverstockedCount());

        // Reorder Recommendations
        assertEquals(2, response.getReorderRecommendations().size());
        assertEquals("CRITICAL", response.getReorderRecommendations().get(0).getUrgency()); // PROD-3
        assertEquals(20, response.getReorderRecommendations().get(0).getSuggestedQuantity());
        assertEquals("HIGH", response.getReorderRecommendations().get(1).getUrgency()); // PROD-2
        assertEquals(25, response.getReorderRecommendations().get(1).getSuggestedQuantity());
    }
}
