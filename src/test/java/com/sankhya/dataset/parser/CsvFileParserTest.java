package com.sankhya.dataset.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvFileParserTest {

    private final CsvFileParser parser = new CsvFileParser();

    @Test
    void testSupports() {
        assertTrue(parser.supports("csv"));
        assertTrue(parser.supports("CSV"));
        assertFalse(parser.supports("xlsx"));
    }

    @Test
    void testReadPreviewAndAllRows(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("test_products.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("product_id,product_name,category,unit_cost,selling_price\n");
            writer.write("PROD_001,Amul Butter 500g,Dairy,240.00,275.00\n");
            writer.write("PROD_002,Tata Salt 1kg,Staples,22.00,28.00\n");
            writer.write("PROD_003,Maggi Noodles 280g,Packaged,48.00,56.00\n");
        }

        ParsedFileData preview = parser.readPreviewAndHeaders(csvFile, 2);
        assertNotNull(preview);
        assertEquals(5, preview.getHeaders().size());
        assertEquals("product_id", preview.getHeaders().get(0));
        assertEquals(2, preview.getPreviewRows().size());
        assertEquals(3, preview.getTotalRowCount());

        List<Map<String, String>> allRows = parser.readAllRows(csvFile);
        assertEquals(3, allRows.size());
        assertEquals("PROD_001", allRows.get(0).get("product_id"));
        assertEquals("Amul Butter 500g", allRows.get(0).get("product_name"));
        assertEquals("240.00", allRows.get(0).get("unit_cost"));
    }

    @Test
    void testStreamingAndBatchProcessing(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("test_streaming.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id,name,value\n");
            for (int i = 1; i <= 25; i++) {
                writer.write("ID_" + i + ",Name " + i + "," + (i * 10) + "\n");
            }
        }

        // Test streaming consumer
        java.util.concurrent.atomic.AtomicInteger streamCount = new java.util.concurrent.atomic.AtomicInteger(0);
        parser.processRowsStreaming(csvFile, (rowNum, row) -> {
            assertTrue(rowNum >= 2);
            assertEquals("ID_" + (rowNum - 1), row.get("id"));
            streamCount.incrementAndGet();
        });
        assertEquals(25, streamCount.get());

        // Test batch consumer (batch size 10 -> batches of 10, 10, 5)
        List<Integer> batchSizes = new java.util.ArrayList<>();
        parser.processBatches(csvFile, 10, batch -> batchSizes.add(batch.size()));
        assertEquals(List.of(10, 10, 5), batchSizes);
    }
}
