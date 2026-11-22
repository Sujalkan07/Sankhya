package com.sankhya.dataset.service;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.overview.service.ExecutiveOverviewService;
import com.sankhya.common.enums.UserRole;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.UnifiedSalesMart;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ImportStatus;
import com.sankhya.dataset.repository.DatasetImportRepository;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Unified Sales Mart Ingestion & Analytics Integration Tests")
class UnifiedMartIngestionIntegrationTest {

    @Autowired
    private DatasetImportService datasetImportService;

    @Autowired
    private DatasetImportRepository importRepository;

    @Autowired
    private UnifiedSalesMartRepository unifiedSalesMartRepository;

    @Autowired
    private ExecutiveOverviewService executiveOverviewService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private UserRepository userRepository;

    private Workspace workspaceA;
    private Workspace workspaceB;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User("Mart Tester", "mart.tester@sankhya.com", "password", UserRole.ROLE_USER));
        workspaceA = workspaceService.createWorkspace(new WorkspaceCreateRequest("Mart Store Alpha", "Store A"), testUser);
        workspaceB = workspaceService.createWorkspace(new WorkspaceCreateRequest("Mart Store Beta", "Store B"), testUser);
    }

    @Test
    @DisplayName("Complete end-to-end ingestion of sankhya_unified_mart rows and multi-tenant isolation")
    void testUnifiedMartIngestionLifecycle() throws IOException {
        String csvContent = "transaction_id,transaction_date,customer_id,customer_name,city,customer_segment,product_id,product_name,category,subcategory,brand,unit_cost,selling_price,quantity,unit_price,discount,payment_method,cashier_name,latest_inventory_snapshot_date,current_closing_stock,current_reorder_level,current_inventory_status\n" +
                "BILL-001,2026-10-15 10:30:00,CUST-01,Ananya Rao,Bengaluru,VIP,SKU-001,Organic Milk 1L,Dairy,Milk,Heritage,40.00,55.00,2,55.00,10.00,Credit Card,Rohan,2026-11-20,120,30,HEALTHY\n" +
                "BILL-001,2026-10-15 10:30:00,CUST-01,Ananya Rao,Bengaluru,VIP,SKU-002,Whole Wheat Bread,Bakery,Bread,Modern,25.00,35.00,1,35.00,0.00,Credit Card,Rohan,2026-11-20,80,25,HEALTHY\n" +
                "BILL-002,2026-10-16 11:15:00,CUST-02,Dev Patel,Mumbai,REGULAR,SKU-001,Organic Milk 1L,Dairy,Milk,Heritage,40.00,55.00,1,55.00,5.00,UPI,Priya,2026-11-20,120,30,HEALTHY\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "sankhya_unified_mart.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // Step 1: Stage upload
        DatasetImport staged = datasetImportService.stageUpload(workspaceA, DatasetType.UNIFIED_MART, file);
        assertNotNull(staged);
        assertEquals(ImportStatus.PENDING, staged.getStatus());
        assertEquals(DatasetType.UNIFIED_MART, staged.getDatasetType());

        // Step 2: Set mapping for all 22 columns
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
        datasetImportService.saveColumnMapping(staged.getId(), workspaceA.getId(), mapping);

        // Step 3: Validate import
        datasetImportService.validateImport(staged.getId(), workspaceA.getId());
        DatasetImport validated = datasetImportService.getImport(staged.getId(), workspaceA.getId());
        assertEquals(ImportStatus.READY_FOR_CONFIRMATION, validated.getStatus());

        // Step 4: Confirm import
        datasetImportService.confirmImport(staged.getId(), workspaceA.getId(), true);
        DatasetImport completed = datasetImportService.getImport(staged.getId(), workspaceA.getId());
        assertEquals(ImportStatus.COMPLETED, completed.getStatus());

        // Step 5: Verify mart records in database
        List<UnifiedSalesMart> rows = unifiedSalesMartRepository.findByWorkspaceId(workspaceA.getId());
        assertEquals(3, rows.size());

        // Verify calculations:
        // Row 1: qty=2, unit_price=55, disc=10 -> rev=100.00, cost=2*40=80 -> profit=20.00
        UnifiedSalesMart row1 = rows.stream().filter(r -> "BILL-001".equals(r.getTransactionId()) && "SKU-001".equals(r.getProductId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("100.00"), row1.getRevenue());
        assertEquals(new BigDecimal("20.00"), row1.getGrossProfit());
        assertEquals("Ananya Rao", row1.getCustomerName());
        assertEquals("Organic Milk 1L", row1.getProductName());

        // Row 2: qty=1, unit_price=35, disc=0 -> rev=35.00, cost=25 -> profit=10.00
        UnifiedSalesMart row2 = rows.stream().filter(r -> "BILL-001".equals(r.getTransactionId()) && "SKU-002".equals(r.getProductId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("35.00"), row2.getRevenue());
        assertEquals(new BigDecimal("10.00"), row2.getGrossProfit());

        // Row 3: qty=1, unit_price=55, disc=5 -> rev=50.00, cost=40 -> profit=10.00
        UnifiedSalesMart row3 = rows.stream().filter(r -> "BILL-002".equals(r.getTransactionId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("50.00"), row3.getRevenue());
        assertEquals(new BigDecimal("10.00"), row3.getGrossProfit());

        // Step 6: Multi-tenant workspace isolation check: Workspace B must have 0 rows
        List<UnifiedSalesMart> rowsB = unifiedSalesMartRepository.findByWorkspaceId(workspaceB.getId());
        assertTrue(rowsB.isEmpty(), "Workspace B must not see records from Workspace A");

        // Step 7: Verify ExecutiveOverviewService derives analytics from the mart!
        ExecutiveOverviewResponse overview = executiveOverviewService.getExecutiveOverview(
                workspaceA.getId(), DateRangeFilter.of(DatePreset.CUSTOM, java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31)));
        assertNotNull(overview);
        assertNotNull(overview.getTotalRevenue());
        // Total revenue: 100 + 35 + 50 = 185.00
        assertEquals(new BigDecimal("185.00"), overview.getTotalRevenue().getCurrentValue());
        // Orders: COUNT(DISTINCT transaction_id) = 2 (BILL-001, BILL-002) - never count rows (3)!
        assertEquals(2, ((Number) overview.getTotalTransactions().getCurrentValue()).intValue());
    }
}
