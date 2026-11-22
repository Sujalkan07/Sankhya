package com.sankhya.dataset.validator;

import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import com.sankhya.dataset.parser.CsvFileParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnifiedMartValidator Unit Tests")
class UnifiedMartValidatorTest {

    private UnifiedMartValidator validator;
    private DatasetImport datasetImport;
    private Map<String, String> columnMapping;

    @BeforeEach
    void setUp() {
        validator = new UnifiedMartValidator();
        datasetImport = new DatasetImport();
        datasetImport.setId(1L);
        datasetImport.setDatasetType(DatasetType.UNIFIED_MART);

        // Identity mapping (header == canonical)
        columnMapping = new HashMap<>();
        String[] cols = {
                "transaction_id", "transaction_date", "customer_id", "customer_name", "city",
                "customer_segment", "product_id", "product_name", "category", "subcategory",
                "brand", "unit_cost", "selling_price", "quantity", "unit_price", "discount",
                "payment_method", "cashier_name", "latest_inventory_snapshot_date",
                "current_closing_stock", "current_reorder_level", "current_inventory_status"
        };
        for (String c : cols) {
            columnMapping.put(c, c);
        }
    }

    @Test
    @DisplayName("supports UNIFIED_MART dataset type")
    void testSupports() {
        assertTrue(validator.supports(DatasetType.UNIFIED_MART));
        assertFalse(validator.supports(DatasetType.SALES_TRANSACTIONS));
    }

    @Test
    @DisplayName("valid 22-column row passes validation")
    void testValidRow() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("transaction_id", "BILL-0000204");
        row.put("transaction_date", "2026-09-21 09:00:00");
        row.put("customer_id", "CUST-115481");
        row.put("customer_name", "Rhea More");
        row.put("city", "Bengaluru");
        row.put("customer_segment", "REGULAR");
        row.put("product_id", "SKU-0607");
        row.put("product_name", "Shredded Mozzarella");
        row.put("category", "Dairy Eggs");
        row.put("subcategory", "Packaged Cheese");
        row.put("brand", "MeadowVale Dairy");
        row.put("unit_cost", "44.00");
        row.put("selling_price", "60.25");
        row.put("quantity", "2");
        row.put("unit_price", "60.25");
        row.put("discount", "5.00");
        row.put("payment_method", "Cash");
        row.put("cashier_name", "Aarav Sharma");
        row.put("latest_inventory_snapshot_date", "2026-11-22");
        row.put("current_closing_stock", "90");
        row.put("current_reorder_level", "45");
        row.put("current_inventory_status", "HEALTHY");

        ValidationResult result = validator.validate(List.of(row), columnMapping, datasetImport, 1L);

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
        // Should contain provenance disclosure warning
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getSeverity() == ErrorSeverity.WARNING));
    }

    @Test
    @DisplayName("invalid numeric bounds and discount exceeding gross line value flag critical errors")
    void testInvalidRowValidation() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("transaction_id", "BILL-0000205");
        row.put("transaction_date", "2026-09-21 09:00:00");
        row.put("product_id", "SKU-0607");
        row.put("product_name", "Mozzarella");
        row.put("category", "Dairy");
        row.put("unit_cost", "-10.00"); // Negative cost
        row.put("selling_price", "50.00");
        row.put("quantity", "0"); // Invalid quantity <= 0
        row.put("unit_price", "50.00");
        row.put("discount", "200.00"); // Exceeds line gross

        ValidationResult result = validator.validate(List.of(row), columnMapping, datasetImport, 1L);

        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getValidRows());
        assertEquals(1, result.getInvalidRows());
        assertFalse(result.getErrors().isEmpty());

        boolean hasQtyError = result.getErrors().stream().anyMatch(e -> "INVALID_QUANTITY".equals(e.getErrorCode()));
        boolean hasCostError = result.getErrors().stream().anyMatch(e -> "INVALID_NUMERIC_BOUNDS".equals(e.getErrorCode()));
        assertTrue(hasQtyError);
        assertTrue(hasCostError);
    }

    @Test
    @DisplayName("streaming validation on CSV file fixture")
    void testStreamingValidation(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("test_unified_mart.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("transaction_id,transaction_date,customer_id,customer_name,city,customer_segment,product_id,product_name,category,subcategory,brand,unit_cost,selling_price,quantity,unit_price,discount,payment_method,cashier_name,latest_inventory_snapshot_date,current_closing_stock,current_reorder_level,current_inventory_status\n");
            writer.write("BILL-1,2026-09-21 09:00:00,CUST-1,Rhea,City,REGULAR,SKU-1,Prod1,Cat,Sub,Brand,10.0,20.0,2,20.0,0.0,Cash,Cashier,2026-11-22,50,20,HEALTHY\n");
            writer.write("BILL-2,2026-09-21 10:00:00,CUST-2,John,City,VIP,SKU-2,Prod2,Cat,Sub,Brand,15.0,30.0,1,30.0,0.0,Card,Cashier,2026-11-22,40,10,HEALTHY\n");
        }

        CsvFileParser parser = new CsvFileParser();
        ValidationResult result = validator.validateStreaming(csvFile, parser, columnMapping, datasetImport, 1L);

        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getSeverity() == ErrorSeverity.WARNING));
    }
}
