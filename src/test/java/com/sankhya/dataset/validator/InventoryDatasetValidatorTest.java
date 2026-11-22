package com.sankhya.dataset.validator;

import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class InventoryDatasetValidatorTest {

    private ProductRepository productRepository;
    private InventoryDatasetValidator validator;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        validator = new InventoryDatasetValidator(productRepository);
    }

    @Test
    void testStockFormulaConsistency() {
        when(productRepository.findAllProductIdsByWorkspaceId(1L)).thenReturn(Set.of("PROD_001"));

        // Formula: opening (50) + received (20) - sold (15) = closing (55)
        List<Map<String, String>> rows = List.of(
                Map.of(
                        "inv_id", "INV_001",
                        "date", "2024-01-15",
                        "sku", "PROD_001",
                        "open_stock", "50",
                        "inward", "20",
                        "sold", "15",
                        "close_stock", "55"
                )
        );

        Map<String, String> mapping = Map.of(
                "inv_id", "inventory_id",
                "date", "snapshot_date",
                "sku", "product_id",
                "open_stock", "opening_stock",
                "inward", "received_stock",
                "sold", "sold_quantity",
                "close_stock", "closing_stock"
        );

        DatasetImport datasetImport = new DatasetImport();
        ValidationResult result = validator.validate(rows, mapping, datasetImport, 1L);

        assertEquals(1, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
        assertEquals(100.0, result.getConsistencyScore());
    }

    @Test
    void testStockFormulaInconsistencyLogsWarning() {
        when(productRepository.findAllProductIdsByWorkspaceId(1L)).thenReturn(Set.of("PROD_001"));

        // Formula mismatch: opening (50) + received (20) - sold (15) = 55, but file says closing is 99
        List<Map<String, String>> rows = List.of(
                Map.of(
                        "inv_id", "INV_001",
                        "date", "2024-01-15",
                        "sku", "PROD_001",
                        "open_stock", "50",
                        "inward", "20",
                        "sold", "15",
                        "close_stock", "99"
                )
        );

        Map<String, String> mapping = Map.of(
                "inv_id", "inventory_id",
                "date", "snapshot_date",
                "sku", "product_id",
                "open_stock", "opening_stock",
                "inward", "received_stock",
                "sold", "sold_quantity",
                "close_stock", "closing_stock"
        );

        DatasetImport datasetImport = new DatasetImport();
        ValidationResult result = validator.validate(rows, mapping, datasetImport, 1L);

        assertTrue(result.getErrors().stream().anyMatch(e -> "INCONSISTENT_CALCULATION".equals(e.getErrorCode())));
    }
}
