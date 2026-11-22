package com.sankhya.dataset.service;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.customers.dto.CustomerIntelligenceResponse;
import com.sankhya.analytics.customers.service.CustomerIntelligenceService;
import com.sankhya.analytics.inventory.dto.InventoryIntelligenceResponse;
import com.sankhya.analytics.inventory.service.InventoryIntelligenceService;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.overview.service.ExecutiveOverviewService;
import com.sankhya.analytics.products.dto.ProductIntelligenceResponse;
import com.sankhya.analytics.products.service.ProductIntelligenceService;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.service.BusinessRecommendationService;
import com.sankhya.analytics.reports.service.AnalyticalReportingService;
import com.sankhya.analytics.sales.dto.SalesAnalyticsResponse;
import com.sankhya.analytics.sales.service.SalesAnalyticsService;
import com.sankhya.common.enums.UserRole;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.UnifiedSalesMart;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ImportStatus;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.enums.ExecutionStatus;
import com.sankhya.ml.execution.MLJobService;
import com.sankhya.ml.repository.CustomerClusterAssignmentRepository;
import com.sankhya.ml.repository.DemandPredictionRepository;
import com.sankhya.ml.repository.ForecastResultRepository;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.dto.WorkspaceCreateRequest;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Unified Sales Mart Ingestion, Analytics & ML Integration Tests")
class UnifiedMartIngestionIntegrationTest {

    @Autowired private DatasetImportService datasetImportService;
    @Autowired private DatasetQueryService datasetQueryService;
    @Autowired private UnifiedSalesMartRepository unifiedSalesMartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InventorySnapshotRepository inventorySnapshotRepository;
    @Autowired private ExecutiveOverviewService executiveOverviewService;
    @Autowired private SalesAnalyticsService salesAnalyticsService;
    @Autowired private CustomerIntelligenceService customerIntelligenceService;
    @Autowired private ProductIntelligenceService productIntelligenceService;
    @Autowired private InventoryIntelligenceService inventoryIntelligenceService;
    @Autowired private BusinessRecommendationService businessRecommendationService;
    @Autowired private AnalyticalReportingService analyticalReportingService;
    @Autowired private MLJobService mlJobService;
    @Autowired private CustomerClusterAssignmentRepository clusterAssignmentRepository;
    @Autowired private DemandPredictionRepository demandPredictionRepository;
    @Autowired private ForecastResultRepository forecastResultRepository;
    @Autowired private WorkspaceService workspaceService;
    @Autowired private UserRepository userRepository;

    private Workspace workspaceA;
    private Workspace workspaceB;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User("Mart Tester", "mart.tester@sankhya.com", "password", UserRole.ROLE_USER));
        workspaceA = workspaceService.createWorkspace(new WorkspaceCreateRequest("Mart Store Alpha", "Store A"), testUser);
        workspaceB = workspaceService.createWorkspace(new WorkspaceCreateRequest("Mart Store Beta", "Store B"), testUser);
    }

    private Map<String, String> canonicalMapping() {
        Map<String, String> mapping = new HashMap<>();
        String[] cols = {
                "transaction_id", "transaction_date", "customer_id", "customer_name", "city",
                "customer_segment", "product_id", "product_name", "category", "subcategory",
                "brand", "unit_cost", "selling_price", "quantity", "unit_price", "discount",
                "payment_method", "cashier_name", "latest_inventory_snapshot_date",
                "current_closing_stock", "current_reorder_level", "current_inventory_status"
        };
        for (String col : cols) {
            mapping.put(col, col);
        }
        return mapping;
    }

    @Test
    @DisplayName("End-to-end Unified Mart ingestion, idempotent re-import, analytics, ML execution, and tenant isolation")
    void testUnifiedMartIngestionLifecycle() throws IOException {
        // Rich 8-day multi-SKU, multi-customer dataset enabling all 6 analytics engines + all 4 ML pipelines
        String csvContent = String.join("\n",
                "transaction_id,transaction_date,customer_id,customer_name,city,customer_segment,product_id,product_name,category,subcategory,brand,unit_cost,selling_price,quantity,unit_price,discount,payment_method,cashier_name,latest_inventory_snapshot_date,current_closing_stock,current_reorder_level,current_inventory_status",
                "BILL-001,2026-11-01 10:00:00,CUST-01,Ananya Rao,Bengaluru,VIP,SKU-001,Organic Milk 1L,Dairy,Milk,Heritage,40.00,55.00,2,55.00,10.00,Card,Rohan,2026-11-20,120,30,HEALTHY",
                "BILL-001,2026-11-01 10:00:00,CUST-01,Ananya Rao,Bengaluru,VIP,SKU-002,Whole Wheat Bread,Bakery,Bread,Modern,25.00,35.00,1,35.00,0.00,Card,Rohan,2026-11-20,80,25,HEALTHY",
                "BILL-002,2026-11-02 11:15:00,CUST-02,Dev Patel,Mumbai,REGULAR,SKU-001,Organic Milk 1L,Dairy,Milk,Heritage,40.00,55.00,1,55.00,5.00,UPI,Priya,2026-11-20,120,30,HEALTHY",
                "BILL-002,2026-11-02 11:15:00,CUST-02,Dev Patel,Mumbai,REGULAR,SKU-002,Whole Wheat Bread,Bakery,Bread,Modern,25.00,35.00,2,35.00,0.00,UPI,Priya,2026-11-20,80,25,HEALTHY",
                "BILL-003,2026-11-04 14:20:00,CUST-03,Kiran Nair,Kochi,VIP,SKU-003,Basmati Rice 5kg,Groceries,Staples,Daawat,300.00,420.00,1,420.00,20.00,Cash,Rohan,2026-11-20,10,20,LOW_STOCK",
                "BILL-003,2026-11-04 14:20:00,CUST-03,Kiran Nair,Kochi,VIP,SKU-004,Toor Dal 1kg,Groceries,Pulses,Tata,120.00,160.00,1,160.00,0.00,Cash,Rohan,2026-11-20,0,15,OUT_OF_STOCK",
                "BILL-004,2026-11-06 16:45:00,CUST-04,Meera Iyer,Chennai,NEW,SKU-005,Green Tea 100g,Beverages,Tea,Lipton,90.00,140.00,3,140.00,0.00,UPI,Priya,2026-11-20,60,15,OVERSTOCKED",
                "BILL-005,2026-11-08 18:10:00,CUST-01,Ananya Rao,Bengaluru,VIP,SKU-001,Organic Milk 1L,Dairy,Milk,Heritage,40.00,55.00,1,55.00,0.00,Card,Rohan,2026-11-20,120,30,HEALTHY",
                "BILL-006,2026-11-10 09:30:00,CUST-05,Vikram Joshi,Pune,REGULAR,SKU-006,Filter Coffee 200g,Beverages,Coffee,Bru,150.00,210.00,1,210.00,10.00,Cash,Priya,2026-11-20,45,15,HEALTHY",
                "BILL-007,2026-11-12 12:00:00,CUST-06,Sonal Kapoor,Delhi,REGULAR,SKU-002,Whole Wheat Bread,Bakery,Bread,Modern,25.00,35.00,2,35.00,0.00,UPI,Rohan,2026-11-20,80,25,HEALTHY",
                "BILL-008,2026-11-15 17:30:00,CUST-02,Dev Patel,Mumbai,REGULAR,SKU-001,Organic Milk 1L,Dairy,Milk,Heritage,40.00,55.00,2,55.00,0.00,Card,Priya,2026-11-20,120,30,HEALTHY",
                "BILL-009,2026-11-18 19:00:00,CUST-03,Kiran Nair,Kochi,VIP,SKU-003,Basmati Rice 5kg,Groceries,Staples,Daawat,300.00,420.00,1,420.00,0.00,UPI,Rohan,2026-11-20,10,20,LOW_STOCK"
        ) + "\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "sankhya_unified_mart.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // 1. Stage, Map, Validate, Confirm
        DatasetImport staged = datasetImportService.stageUpload(workspaceA, DatasetType.UNIFIED_MART, file);
        assertEquals(ImportStatus.PENDING, staged.getStatus());

        datasetImportService.saveColumnMapping(staged.getId(), workspaceA.getId(), canonicalMapping());
        datasetImportService.validateImport(staged.getId(), workspaceA.getId());
        DatasetImport validated = datasetImportService.getImport(staged.getId(), workspaceA.getId());
        assertEquals(ImportStatus.READY_FOR_CONFIRMATION, validated.getStatus());

        datasetImportService.confirmImport(staged.getId(), workspaceA.getId(), true);
        DatasetImport completed = datasetImportService.getImport(staged.getId(), workspaceA.getId());
        assertEquals(ImportStatus.COMPLETED, completed.getStatus());
        assertEquals(12, completed.getValidRows());
        assertEquals("COMPLETED", workspaceA.getOnboardingStatus());

        // 2. Verify mart persistence & synchronized dimensions
        List<UnifiedSalesMart> rows = unifiedSalesMartRepository.findByWorkspaceId(workspaceA.getId());
        assertEquals(12, rows.size());
        assertEquals(6, productRepository.countByWorkspaceId(workspaceA.getId()));
        assertEquals(6, customerRepository.countByWorkspaceId(workspaceA.getId()));
        assertEquals(6, inventorySnapshotRepository.countByWorkspaceId(workspaceA.getId()));

        // Verify exact revenue/profit math on Row 1 (qty=2, unit_price=55, disc=10 -> rev=100, profit=20)
        UnifiedSalesMart row1 = rows.stream().filter(r -> "BILL-001".equals(r.getTransactionId()) && "SKU-001".equals(r.getProductId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("100.00"), row1.getRevenue());
        assertEquals(new BigDecimal("20.00"), row1.getGrossProfit());

        // 3. Test idempotent second upload updating existing SKUs/Customers without unique constraint failures
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "sankhya_unified_mart_update.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        DatasetImport staged2 = datasetImportService.stageUpload(workspaceA, DatasetType.UNIFIED_MART, file2);
        datasetImportService.saveColumnMapping(staged2.getId(), workspaceA.getId(), canonicalMapping());
        datasetImportService.validateImport(staged2.getId(), workspaceA.getId());
        assertDoesNotThrow(() -> datasetImportService.confirmImport(staged2.getId(), workspaceA.getId(), true));
        // Still 6 distinct products & 6 distinct customers (updated, not duplicated!)
        assertEquals(6, productRepository.countByWorkspaceId(workspaceA.getId()));
        assertEquals(6, customerRepository.countByWorkspaceId(workspaceA.getId()));

        // Clean back to 12 rows by deleting second import's mart rows so downstream math assertions are exact
        unifiedSalesMartRepository.deleteByWorkspaceIdAndDatasetImportId(workspaceA.getId(), staged2.getId());

        // 4. Verify Readiness Logic (Module-Specific + Metadata)
        AnalyticsReadinessResponse readinessA = datasetQueryService.getAnalyticsReadiness(workspaceA.getId());
        assertEquals("UNIFIED_SALES_MART", readinessA.getDataSource());
        assertEquals(12, readinessA.getSalesRecordCount());
        assertEquals(9, readinessA.getDistinctTransactionCount());
        assertEquals(6, readinessA.getProductRecordCount());
        assertEquals(6, readinessA.getCustomerRecordCount());
        assertEquals(6, readinessA.getInventoryRecordCount());
        assertTrue(readinessA.isOverallReady());
        assertTrue(readinessA.isSalesReady());
        assertTrue(readinessA.isCustomerReady());
        assertTrue(readinessA.isProductReady());
        assertTrue(readinessA.isInventoryReady());
        assertTrue(readinessA.isAprioriReady());
        assertTrue(readinessA.isKmeansReady());
        assertTrue(readinessA.isRandomForestReady());
        assertTrue(readinessA.isLinearRegressionReady());

        // 5. Verify Multi-Tenant Workspace Isolation (Workspace B must be completely empty)
        assertTrue(unifiedSalesMartRepository.findByWorkspaceId(workspaceB.getId()).isEmpty());
        AnalyticsReadinessResponse readinessB = datasetQueryService.getAnalyticsReadiness(workspaceB.getId());
        assertEquals("NONE", readinessB.getDataSource());
        assertFalse(readinessB.isSalesReady());
        assertFalse(readinessB.isAprioriReady());

        // 6. Verify all 6 Analytics Modules + CSV Export produce genuine non-empty results from the mart
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.CUSTOM, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        ExecutiveOverviewResponse overview = executiveOverviewService.getExecutiveOverview(workspaceA.getId(), filter);
        // Total revenue across 12 lines: 100 + 35 + 50 + 70 + 400 + 160 + 420 + 55 + 200 + 70 + 110 + 420 = 2090.00
        assertEquals(new BigDecimal("2090.00"), overview.getTotalRevenue().getCurrentValue());
        assertEquals(9, ((Number) overview.getTotalTransactions().getCurrentValue()).intValue());
        assertEquals(2L, overview.getLowStockAlertCount()); // SKU-003 (10<=20) and SKU-004 (0<=15)

        SalesAnalyticsResponse sales = salesAnalyticsService.getSalesAnalytics(workspaceA.getId(), filter);
        assertEquals(new BigDecimal("2090.00"), sales.getKpis().getTotalRevenue().getCurrentValue());
        assertFalse(sales.getCategoryPerformance().isEmpty());
        assertFalse(sales.getPaymentMethods().isEmpty());

        CustomerIntelligenceResponse customers = customerIntelligenceService.getCustomerIntelligence(workspaceA.getId(), filter);
        assertEquals(6L, ((Number) customers.getKpis().getTotalCustomers().getCurrentValue()).longValue());
        assertFalse(customers.getRfmCustomers().isEmpty());
        assertTrue(customers.getRfmCustomers().stream().anyMatch(c -> "Ananya Rao".equals(c.getCustomerName())));

        ProductIntelligenceResponse products = productIntelligenceService.getProductIntelligence(workspaceA.getId(), filter);
        assertEquals(6L, products.getKpis().getTotalCatalogProducts());
        assertFalse(products.getAllProducts().isEmpty());
        assertTrue(products.getAllProducts().stream().anyMatch(p -> "Organic Milk 1L".equals(p.getProductName())));

        InventoryIntelligenceResponse inventory = inventoryIntelligenceService.getInventoryIntelligence(workspaceA.getId(), filter);
        // Deduplicated SKUs = 6 (NOT summed across 12 lines!)
        // Cost valuation: (120*40) + (80*25) + (10*300) + (0*120) + (60*90) + (45*150) = 4800 + 2000 + 3000 + 0 + 5400 + 6750 = 21950.00
        assertEquals(new BigDecimal("21950.00"), inventory.getKpis().getTotalCostValuation());
        assertEquals(1L, inventory.getStockHealth().getOutOfStockCount());
        assertEquals(1L, inventory.getStockHealth().getLowStockCount());

        List<BusinessRecommendation> recs = businessRecommendationService.generateRecommendations(workspaceA.getId(), filter);
        assertFalse(recs.isEmpty());
        assertTrue(recs.stream().anyMatch(r -> "RULE_STOCKOUT_RISK".equals(r.getRuleIdentifier())));

        StringWriter csvOut = new StringWriter();
        analyticalReportingService.exportSalesTransactionsCsv(workspaceA.getId(), filter, csvOut);
        assertTrue(csvOut.toString().contains("BILL-001"));
        assertTrue(csvOut.toString().contains("SKU-003"));

        // 7. Verify all 4 ML Engines run cleanly against the Unified Mart and persist outputs
        MLModelExecution kmeansExec = mlJobService.executeKMeans(workspaceA.getId(), 3);
        assertEquals(ExecutionStatus.COMPLETED, kmeansExec.getStatus());
        assertFalse(clusterAssignmentRepository.findByWorkspaceIdOrderByClusterIdAsc(workspaceA.getId()).isEmpty());

        MLModelExecution aprioriExec = mlJobService.executeApriori(workspaceA.getId(), 0.05, 0.20, 1.0);
        assertEquals(ExecutionStatus.COMPLETED, aprioriExec.getStatus());

        MLModelExecution rfExec = mlJobService.executeRandomForest(workspaceA.getId(), 30);
        assertEquals(ExecutionStatus.COMPLETED, rfExec.getStatus());
        assertFalse(demandPredictionRepository.findByWorkspaceIdOrderByPredictedUnitsNext7dDesc(workspaceA.getId()).isEmpty());

        MLModelExecution lrExec = mlJobService.executeLinearRegression(workspaceA.getId(), 7);
        assertEquals(ExecutionStatus.COMPLETED, lrExec.getStatus());
        assertEquals(7, forecastResultRepository.findByWorkspaceIdOrderByForecastDateAsc(workspaceA.getId()).size());
    }
}
