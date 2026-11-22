package com.sankhya.dataset.parser;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelFileParserTest {

    private final ExcelFileParser parser = new ExcelFileParser();

    @Test
    void testSupports() {
        assertTrue(parser.supports("xlsx"));
        assertTrue(parser.supports("XLSX"));
        assertTrue(parser.supports("xls"));
        assertFalse(parser.supports("csv"));
    }

    @Test
    void testReadExcelPreviewAndAllRows(@TempDir Path tempDir) throws IOException {
        File excelFile = tempDir.resolve("test_customers.xlsx").toFile();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile)) {
            Sheet sheet = workbook.createSheet("Customers");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("customer_id");
            headerRow.createCell(1).setCellValue("customer_name");
            headerRow.createCell(2).setCellValue("age");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("CUST_001");
            row1.createCell(1).setCellValue("Rajesh Sharma");
            row1.createCell(2).setCellValue(35);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("CUST_002");
            row2.createCell(1).setCellValue("Priya Patel");
            row2.createCell(2).setCellValue(28);

            workbook.write(fos);
        }

        ParsedFileData preview = parser.readPreviewAndHeaders(excelFile, 1);
        assertNotNull(preview);
        assertEquals(3, preview.getHeaders().size());
        assertEquals("customer_id", preview.getHeaders().get(0));
        assertEquals(1, preview.getPreviewRows().size());
        assertEquals(2, preview.getTotalRowCount());

        List<Map<String, String>> allRows = parser.readAllRows(excelFile);
        assertEquals(2, allRows.size());
        assertEquals("CUST_001", allRows.get(0).get("customer_id"));
        assertEquals("Rajesh Sharma", allRows.get(0).get("customer_name"));
        assertEquals("35", allRows.get(0).get("age"));
    }
}
