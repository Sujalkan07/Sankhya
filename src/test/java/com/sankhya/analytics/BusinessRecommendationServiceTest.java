package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.enums.RecommendationCategory;
import com.sankhya.analytics.recommendations.enums.RecommendationSeverity;
import com.sankhya.analytics.recommendations.enums.RecommendationStatus;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.analytics.recommendations.service.BusinessRecommendationService;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessRecommendationService Unit Tests")
class BusinessRecommendationServiceTest {

    @Mock
    private BusinessRecommendationRepository businessRecommendationRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private com.sankhya.dataset.repository.UnifiedSalesMartRepository unifiedSalesMartRepository;

    private BusinessRecommendationService businessRecommendationService;

    @BeforeEach
    void setUp() {
        businessRecommendationService = new BusinessRecommendationService(
                businessRecommendationRepository,
                workspaceRepository,
                salesTransactionRepository,
                inventorySnapshotRepository,
                productRepository,
                customerRepository,
                unifiedSalesMartRepository
        );
    }

    @Test
    @DisplayName("generateRecommendations triggers rules and respects idempotency")
    void testGenerateRecommendationsSuccess() {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);
        Workspace workspace = new Workspace("Test Store", "Test", null);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        // 1. Inventory mock: stockout risk
        InventorySnapshot lowStock = new InventorySnapshot(null, "INV-1", "PROD-1", LocalDate.now(), 10, 0, 8, 2, 10, "Store", null);
        when(inventorySnapshotRepository.findLatestSnapshotsByWorkspaceId(workspaceId))
                .thenReturn(List.of(lowStock));
        when(productRepository.findByWorkspaceId(workspaceId)).thenReturn(Collections.emptyList());

        // Check idempotency: not already active
        when(businessRecommendationRepository.existsByWorkspaceIdAndRuleIdentifierAndSupportingMetricAndStatus(
                eq(workspaceId), eq("RULE_STOCKOUT_RISK"), eq("PROD-1"), any()))
                .thenReturn(false);

        // 2. Product mock: no low margin on top product
        when(salesTransactionRepository.findProductPerformanceBetween(eq(workspaceId), any(), any()))
                .thenReturn(Collections.emptyList());

        // 3. Customer RFM mock: no churn
        when(salesTransactionRepository.findCustomerRfmMetricsBetween(eq(workspaceId), any(), any()))
                .thenReturn(Collections.emptyList());

        // 4. Sales revenue decline mock: current 1000, prev 1000 (no decline)
        when(salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(eq(workspaceId), any(), any()))
                .thenReturn(new BigDecimal("1000.00"), new BigDecimal("1000.00"));

        // 5. Category concentration mock
        when(salesTransactionRepository.findCategorySalesContributionBetween(eq(workspaceId), any(), any()))
                .thenReturn(Collections.emptyList());

        // 6. Repeat purchase mock
        when(salesTransactionRepository.countActiveCustomersBetween(eq(workspaceId), any(), any()))
                .thenReturn(0L);

        // Save mock
        when(businessRecommendationRepository.save(any(BusinessRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<BusinessRecommendation> recs = businessRecommendationService.generateRecommendations(workspaceId, filter);

        // Verify save was called for RULE_STOCKOUT_RISK
        verify(businessRecommendationRepository, times(1)).save(any(BusinessRecommendation.class));
    }

    @Test
    @DisplayName("updateStatus transitions recommendation lifecycle properly")
    void testUpdateStatusSuccess() {
        Long workspaceId = 1L;
        Long recId = 42L;
        Workspace workspace = new Workspace("Store", "Desc", null);
        BusinessRecommendation rec = new BusinessRecommendation(
                workspace, RecommendationCategory.INVENTORY, RecommendationSeverity.HIGH, 1,
                "RULE_STOCKOUT_RISK", "Replenish Milk", "Stock low", "Order 20 units",
                "PROD-1", "2", "10"
        );

        when(businessRecommendationRepository.findByIdAndWorkspaceId(recId, workspaceId))
                .thenReturn(Optional.of(rec));
        when(businessRecommendationRepository.save(any(BusinessRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessRecommendation updated = businessRecommendationService.updateStatus(workspaceId, recId, RecommendationStatus.RESOLVED);

        assertNotNull(updated);
        assertEquals(RecommendationStatus.RESOLVED, updated.getStatus());
        verify(businessRecommendationRepository, times(1)).save(rec);
    }
}
