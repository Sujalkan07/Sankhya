package com.sankhya.dataset.validator;

import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.enums.ErrorSeverity;
import com.sankhya.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SalesDatasetValidatorTest {

    private ProductRepository productRepository;
    private CustomerRepository customerRepository;
    private SalesDatasetValidator validator;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        validator = new SalesDatasetValidator(productRepository, customerRepository);
    }

    @Test
    void testValidSalesTransaction() {
        when(productRepository.findAllProductIdsByWorkspaceId(1L)).thenReturn(Set.of("PROD_001"));
        when(customerRepository.findAllCustomerIdsByWorkspaceId(1L)).thenReturn(Set.of("CUST_001"));

        List<Map<String, String>> rows = List.of(
                Map.of(
                        "bill_no", "TXN_1001",
                        "date", "2024-01-15 14:30:00",
                        "sku", "PROD_001",
                        "cust_id", "CUST_001",
                        "qty", "2",
                        "rate", "74.00",
                        "disc", "0.00"
                )
        );

        Map<String, String> mapping = Map.of(
                "bill_no", "transaction_id",
                "date", "transaction_date",
                "sku", "product_id",
                "cust_id", "customer_id",
                "qty", "quantity",
                "rate", "unit_price",
                "disc", "discount"
        );

        DatasetImport datasetImport = new DatasetImport();
        ValidationResult result = validator.validate(rows, mapping, datasetImport, 1L);

        assertEquals(1, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
    }

    @Test
    void testReferentialIntegrityMissingProductWarning() {
        when(productRepository.findAllProductIdsByWorkspaceId(1L)).thenReturn(Set.of("PROD_EXISTING"));
        when(customerRepository.findAllCustomerIdsByWorkspaceId(1L)).thenReturn(Collections.emptySet());

        List<Map<String, String>> rows = List.of(
                Map.of(
                        "bill_no", "TXN_1001",
                        "date", "2024-01-15 14:30:00",
                        "sku", "PROD_UNKNOWN",
                        "qty", "2",
                        "rate", "74.00"
                )
        );

        Map<String, String> mapping = Map.of(
                "bill_no", "transaction_id",
                "date", "transaction_date",
                "sku", "product_id",
                "qty", "quantity",
                "rate", "unit_price"
        );

        DatasetImport datasetImport = new DatasetImport();
        ValidationResult result = validator.validate(rows, mapping, datasetImport, 1L);

        // Warning logged for foreign key violation
        assertTrue(result.getErrors().stream().anyMatch(e -> "FOREIGN_KEY_NOT_FOUND".equals(e.getErrorCode())));
    }
}
