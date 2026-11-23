package com.sankhya.dataset.service;

import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatasetQueryService Health & Readiness Tests")
class DatasetQueryServiceTest {

    @Mock private DatasetRepository datasetRepository;
    @Mock private SalesTransactionRepository salesTransactionRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private InventorySnapshotRepository inventorySnapshotRepository;
    @Mock private UnifiedSalesMartRepository unifiedSalesMartRepository;

    private DatasetQueryService service;

    @BeforeEach
    void setUp() {
        service = new DatasetQueryService(
                datasetRepository,
                salesTransactionRepository,
                productRepository,
                customerRepository,
                inventorySnapshotRepository,
                unifiedSalesMartRepository
        );
    }

    @Test
    @DisplayName("Health summary accurately reflects single active mart without artificial inflation")
    void testHealthSummarySingleMart() {
        Long workspaceId = 1L;
        when(datasetRepository.findByWorkspaceId(workspaceId)).thenReturn(Collections.emptyList());
        when(unifiedSalesMartRepository.countByWorkspaceId(workspaceId)).thenReturn(500_000L);

        Map<String, Object> summary = service.getDatasetHealthSummary(workspaceId);

        assertEquals(1, summary.get("activeDatasets"));
        assertEquals(500_000L, summary.get("totalRecords"));
        assertTrue((Boolean) summary.get("isReadyForAnalytics"));
    }

    @Test
    @DisplayName("Readiness checks verify specific mart counts for products, customers, and inventory")
    void testReadinessChecks() {
        Long workspaceId = 1L;

        // Products
        when(productRepository.countByWorkspaceId(workspaceId)).thenReturn(0L);
        when(unifiedSalesMartRepository.countTotalProductsByWorkspaceId(workspaceId)).thenReturn(1000L);
        assertTrue(service.isProductIntelligenceReady(workspaceId));

        // Customers
        when(customerRepository.countByWorkspaceId(workspaceId)).thenReturn(0L);
        when(unifiedSalesMartRepository.countTotalCustomersByWorkspaceId(workspaceId)).thenReturn(9978L);
        assertTrue(service.isCustomerIntelligenceReady(workspaceId));

        // Inventory
        when(inventorySnapshotRepository.countByWorkspaceId(workspaceId)).thenReturn(0L);
        when(unifiedSalesMartRepository.countInventoryObservationsByWorkspaceId(workspaceId)).thenReturn(1000L);
        assertTrue(service.isInventoryIntelligenceReady(workspaceId));
    }
}
