package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.customers.dto.CustomerIntelligenceResponse;
import com.sankhya.analytics.customers.dto.RfmCustomerDto;
import com.sankhya.analytics.customers.service.CustomerIntelligenceService;
import com.sankhya.analytics.inventory.dto.InventoryIntelligenceResponse;
import com.sankhya.analytics.inventory.dto.ReorderRecommendationDto;
import com.sankhya.analytics.inventory.service.InventoryIntelligenceService;
import com.sankhya.analytics.products.dto.ProductIntelligenceResponse;
import com.sankhya.analytics.products.dto.ProductPerformanceDto;
import com.sankhya.analytics.products.service.ProductIntelligenceService;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.enums.RecommendationCategory;
import com.sankhya.analytics.recommendations.enums.RecommendationSeverity;
import com.sankhya.analytics.recommendations.service.BusinessRecommendationService;
import com.sankhya.analytics.reports.service.AnalyticalReportingService;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.workspace.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticalReportingService Unit Tests")
class AnalyticalReportingServiceTest {

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private com.sankhya.dataset.repository.UnifiedSalesMartRepository unifiedSalesMartRepository;

    @Mock
    private ProductIntelligenceService productIntelligenceService;

    @Mock
    private CustomerIntelligenceService customerIntelligenceService;

    @Mock
    private InventoryIntelligenceService inventoryIntelligenceService;

    @Mock
    private BusinessRecommendationService businessRecommendationService;

    private AnalyticalReportingService analyticalReportingService;

    @BeforeEach
    void setUp() {
        analyticalReportingService = new AnalyticalReportingService(
                salesTransactionRepository,
                unifiedSalesMartRepository,
                productIntelligenceService,
                customerIntelligenceService,
                inventoryIntelligenceService,
                businessRecommendationService
        );
    }

    @Test
    @DisplayName("exportSalesTransactionsCsv generates valid RFC-4180 header and content")
    void testExportSalesCsv() throws IOException {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);
        StringWriter writer = new StringWriter();

        when(salesTransactionRepository.findTransactionsInDateRange(eq(workspaceId), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        analyticalReportingService.exportSalesTransactionsCsv(workspaceId, filter, writer);

        String csv = writer.toString();
        assertTrue(csv.startsWith("Transaction ID,Date,Product ID,Quantity,Unit Price,Discount,Revenue,Payment Method,Customer ID,Status"));
    }

    @Test
    @DisplayName("exportProductPerformanceCsv exports product lines accurately")
    void testExportProductPerformanceCsv() throws IOException {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);
        StringWriter writer = new StringWriter();

        ProductIntelligenceResponse prodResponse = new ProductIntelligenceResponse();
        prodResponse.setAllProducts(List.of(
                new ProductPerformanceDto("SKU-1", "Organic Milk", "Dairy", 100L,
                        new BigDecimal("500.00"), new BigDecimal("350.00"),
                        new BigDecimal("150.00"), new BigDecimal("30.00"), "Star")
        ));
        when(productIntelligenceService.getProductIntelligence(workspaceId, filter)).thenReturn(prodResponse);

        analyticalReportingService.exportProductPerformanceCsv(workspaceId, filter, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("SKU-1"));
        assertTrue(csv.contains("Organic Milk"));
        assertTrue(csv.contains("Star"));
    }

    @Test
    @DisplayName("exportCustomerIntelligenceCsv exports customer RFM lines accurately")
    void testExportCustomerIntelligenceCsv() throws IOException {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);
        StringWriter writer = new StringWriter();

        CustomerIntelligenceResponse custResponse = new CustomerIntelligenceResponse();
        custResponse.setRfmCustomers(List.of(
                new RfmCustomerDto("CUST-1", "Alice Smith", 2L, 5L,
                        new BigDecimal("600.00"), 5, 4, 5, "Champions")
        ));
        when(customerIntelligenceService.getCustomerIntelligence(workspaceId, filter)).thenReturn(custResponse);

        analyticalReportingService.exportCustomerIntelligenceCsv(workspaceId, filter, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("CUST-1"));
        assertTrue(csv.contains("Alice Smith"));
        assertTrue(csv.contains("Champions"));
        assertTrue(csv.contains("545"));
    }

    @Test
    @DisplayName("exportInventoryHealthCsv exports reorder recommendations accurately")
    void testExportInventoryHealthCsv() throws IOException {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);
        StringWriter writer = new StringWriter();

        InventoryIntelligenceResponse invResponse = new InventoryIntelligenceResponse();
        invResponse.setReorderRecommendations(List.of(
                new ReorderRecommendationDto("PROD-1", "Almond Butter", "Spreads",
                        2, 10, 18, new BigDecimal("8.00"), new BigDecimal("144.00"), "CRITICAL")
        ));
        when(inventoryIntelligenceService.getInventoryIntelligence(workspaceId, filter)).thenReturn(invResponse);

        analyticalReportingService.exportInventoryHealthCsv(workspaceId, filter, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("PROD-1"));
        assertTrue(csv.contains("Almond Butter"));
        assertTrue(csv.contains("CRITICAL"));
    }

    @Test
    @DisplayName("exportRecommendationsCsv exports recommendation log accurately")
    void testExportRecommendationsCsv() throws IOException {
        Long workspaceId = 1L;
        StringWriter writer = new StringWriter();

        Workspace workspace = new Workspace("Store", "Desc", null);
        BusinessRecommendation rec = new BusinessRecommendation(
                workspace, RecommendationCategory.INVENTORY, RecommendationSeverity.CRITICAL, 1,
                "RULE_STOCKOUT_RISK", "Replenish Milk", "Stock low", "Order 20 units",
                "PROD-1", "2", "10"
        );
        when(businessRecommendationService.getRecommendations(workspaceId, null, null))
                .thenReturn(List.of(rec));

        analyticalReportingService.exportRecommendationsCsv(workspaceId, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("RULE_STOCKOUT_RISK"));
        assertTrue(csv.contains("Replenish Milk"));
        assertTrue(csv.contains("CRITICAL"));
    }
}
