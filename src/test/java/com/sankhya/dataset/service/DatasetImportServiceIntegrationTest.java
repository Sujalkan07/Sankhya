package com.sankhya.dataset.service;

import com.sankhya.common.enums.UserRole;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ImportStatus;
import com.sankhya.dataset.repository.DatasetImportRepository;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.dto.WorkspaceCreateRequest;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatasetImportServiceIntegrationTest {

    @Autowired
    private DatasetImportService datasetImportService;

    @Autowired
    private DatasetImportRepository importRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private UserRepository userRepository;

    private Workspace workspaceA;
    private Workspace workspaceB;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("Test Admin", "admin.import@test.com", "pass", UserRole.ROLE_USER));

        workspaceA = workspaceService.createWorkspace(
                new WorkspaceCreateRequest("Supermarket A", "Store A"), user);
        workspaceB = workspaceService.createWorkspace(
                new WorkspaceCreateRequest("Supermarket B", "Store B"), user);
    }

    @Test
    void testEndToEndImportWorkflowAndWorkspaceIsolation() throws IOException {
        String csvContent = "sku,product_name,category,cost,price\n" +
                "SKU_100,Amul Pure Ghee 1L,Dairy,540.00,610.00\n" +
                "SKU_101,Tata Tea Gold 500g,Beverages,280.00,325.00\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products_batch.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // 1. Stage upload for Workspace A
        DatasetImport staged = datasetImportService.stageUpload(workspaceA, DatasetType.PRODUCT_CATALOG, file);
        assertNotNull(staged.getId());
        assertEquals(ImportStatus.PENDING, staged.getStatus());
        assertEquals(2, staged.getTotalRows());

        // 2. Set mapping
        Map<String, String> mapping = Map.of(
                "sku", "product_id",
                "product_name", "product_name",
                "category", "category",
                "cost", "unit_cost",
                "price", "selling_price"
        );
        datasetImportService.saveColumnMapping(staged.getId(), workspaceA.getId(), mapping);

        // 3. Validate
        datasetImportService.validateImport(staged.getId(), workspaceA.getId());
        DatasetImport validated = datasetImportService.getImport(staged.getId(), workspaceA.getId());
        assertEquals(ImportStatus.READY_FOR_CONFIRMATION, validated.getStatus());
        assertEquals(2, validated.getValidRows());
        assertEquals(0, validated.getInvalidRows());
        assertTrue(validated.getQualityScore() >= 95.0);

        // 4. Confirm import into domain database
        datasetImportService.confirmImport(staged.getId(), workspaceA.getId(), true);
        DatasetImport completed = datasetImportService.getImport(staged.getId(), workspaceA.getId());
        assertEquals(ImportStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());

        // 5. Verify Workspace Isolation
        long workspaceAProductCount = productRepository.countByWorkspaceId(workspaceA.getId());
        long workspaceBProductCount = productRepository.countByWorkspaceId(workspaceB.getId());

        assertEquals(2, workspaceAProductCount, "Workspace A should contain the 2 imported products");
        assertEquals(0, workspaceBProductCount, "Workspace B must remain isolated with 0 products");

        // 6. Test Error Export (empty CSV header if no errors)
        byte[] errorCsv = datasetImportService.exportErrorsCsv(staged.getId(), workspaceA.getId());
        assertNotNull(errorCsv);
        assertTrue(new String(errorCsv, StandardCharsets.UTF_8).contains("Row Number,Column Name"));
    }
}
