package com.sankhya.dataset.validator;

import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import com.sankhya.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ProductDatasetValidatorTest {

    private ProductRepository productRepository;
    private ProductDatasetValidator validator;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        validator = new ProductDatasetValidator(productRepository);
    }

    @Test
    void testValidProductsPass() {
        when(productRepository.findAllProductIdsByWorkspaceId(1L)).thenReturn(Collections.emptySet());

        List<Map<String, String>> rows = List.of(
                Map.of(
                        "sku", "PROD_001",
                        "title", "Amul Butter 500g",
                        "category", "Dairy",
                        "cost", "240.00",
                        "price", "275.00"
                )
        );

        Map<String, String> mapping = Map.of(
                "sku", "product_id",
                "title", "product_name",
                "category", "category",
                "cost", "unit_cost",
                "price", "selling_price"
        );

        DatasetImport datasetImport = new DatasetImport();
        ValidationResult result = validator.validate(rows, mapping, datasetImport, 1L);

        assertEquals(1, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
        assertTrue(result.getQualityScore() >= 90.0);
    }

    @Test
    void testMissingRequiredSkuFails() {
        when(productRepository.findAllProductIdsByWorkspaceId(1L)).thenReturn(Collections.emptySet());

        List<Map<String, String>> rows = List.of(
                Map.of(
                        "sku", "",
                        "title", "Amul Butter 500g",
                        "category", "Dairy",
                        "cost", "240.00",
                        "price", "275.00"
                )
        );

        Map<String, String> mapping = Map.of(
                "sku", "product_id",
                "title", "product_name",
                "category", "category",
                "cost", "unit_cost",
                "price", "selling_price"
        );

        DatasetImport datasetImport = new DatasetImport();
        ValidationResult result = validator.validate(rows, mapping, datasetImport, 1L);

        assertEquals(0, result.getValidRows());
        assertEquals(1, result.getInvalidRows());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getSeverity() == ErrorSeverity.CRITICAL));
    }
}
