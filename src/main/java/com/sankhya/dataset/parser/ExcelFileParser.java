package com.sankhya.dataset.parser;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ExcelFileParser implements FileParser {

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public boolean supports(String extension) {
        return extension != null && (extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("xls"));
    }

    @Override
    public ParsedFileData readPreviewAndHeaders(File file, int maxPreviewRows) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return new ParsedFileData(Collections.emptyList(), Collections.emptyList(), 0);
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return new ParsedFileData(Collections.emptyList(), Collections.emptyList(), 0);
            }

            // Header row
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                String val = dataFormatter.formatCellValue(cell).trim();
                headers.add(val);
            }

            List<Map<String, String>> previewRows = new ArrayList<>();
            int totalCount = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                totalCount++;

                if (previewRows.size() < maxPreviewRows) {
                    Map<String, String> rowMap = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String header = headers.get(i);
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String val = formatCellValue(cell);
                        rowMap.put(header, val);
                    }
                    previewRows.add(rowMap);
                }
            }

            return new ParsedFileData(headers, previewRows, totalCount);
        }
    }

    @Override
    public List<Map<String, String>> readAllRows(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return Collections.emptyList();
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return Collections.emptyList();
            }

            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(dataFormatter.formatCellValue(cell).trim());
            }

            List<Map<String, String>> allRows = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean isEmpty = true;

                for (int i = 0; i < headers.size(); i++) {
                    String header = headers.get(i);
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = formatCellValue(cell);
                    if (!val.isEmpty()) isEmpty = false;
                    rowMap.put(header, val);
                }

                if (!isEmpty) {
                    allRows.add(rowMap);
                }
            }
            return allRows;
        }
    }

    private String formatCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            if (date != null) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
            }
        }
        return dataFormatter.formatCellValue(cell).trim();
    }
}
