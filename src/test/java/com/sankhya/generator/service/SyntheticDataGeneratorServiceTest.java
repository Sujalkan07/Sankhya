package com.sankhya.generator.service;

import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.dataset.service.DatasetService;
import com.sankhya.generator.dto.GeneratorRequest;
import com.sankhya.generator.dto.GeneratorSummaryResponse;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.workspace.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class SyntheticDataGeneratorServiceTest {

    private ProductRepository productRepository;
    private CustomerRepository customerRepository;
    private SalesTransactionRepository salesTransactionRepository;
    private InventorySnapshotRepository inventorySnapshotRepository;
    private DatasetRepository datasetRepository;
    private DatasetService datasetService;
    private SyntheticDataGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        salesTransactionRepository = Mockito.mock(SalesTransactionRepository.class);
        inventorySnapshotRepository = Mockito.mock(InventorySnapshotRepository.class);
        datasetRepository = Mockito.mock(DatasetRepository.class);
        datasetService = Mockito.mock(DatasetService.class);

        generatorService = new SyntheticDataGeneratorService(
                productRepository, customerRepository, salesTransactionRepository,
                inventorySnapshotRepository, datasetRepository, datasetService
        );
    }

    @Test
    void testGenerateSupermarketDataSmallScale() {
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        workspace.setName("SmartMart Test");

        when(datasetService.findOrCreateDataset(any(), any(DatasetType.class)))
                .thenAnswer(inv -> new Dataset(workspace, inv.getArgument(1), "Test Dataset", "Desc"));

        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(salesTransactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(inventorySnapshotRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        GeneratorRequest request = new GeneratorRequest(12, "SMALL", false);
        GeneratorSummaryResponse response = generatorService.generateSupermarketData(workspace, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(15, response.getProductsGenerated());
        assertEquals(40, response.getCustomersGenerated());
        assertTrue(response.getTransactionsGenerated() > 100);
        assertTrue(response.getInventorySnapshotsGenerated() > 100);
        assertEquals(12, response.getDurationMonths());
    }
}
