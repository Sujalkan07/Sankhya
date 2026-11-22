package com.sankhya.dataset.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CsvFileParser implements FileParser {

    @Override
    public boolean supports(String extension) {
        return extension != null && extension.equalsIgnoreCase("csv");
    }

    @Override
    public ParsedFileData readPreviewAndHeaders(File file, int maxPreviewRows) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = format.parse(reader)) {
                List<String> headers = cleanHeaders(parser.getHeaderNames());
                List<Map<String, String>> previewRows = new ArrayList<>();
                int totalCount = 0;

                for (CSVRecord record : parser) {
                    totalCount++;
                    if (previewRows.size() < maxPreviewRows) {
                        Map<String, String> row = new LinkedHashMap<>();
                        for (String header : headers) {
                            String val = record.isMapped(header) ? record.get(header) : "";
                            row.put(header, val != null ? val.trim() : "");
                        }
                        previewRows.add(row);
                    }
                }
                return new ParsedFileData(headers, previewRows, totalCount);
            }
        }
    }

    @Override
    public List<Map<String, String>> readAllRows(File file) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = format.parse(reader)) {
                List<String> headers = cleanHeaders(parser.getHeaderNames());
                List<Map<String, String>> allRows = new ArrayList<>();

                for (CSVRecord record : parser) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String header : headers) {
                        String val = record.isMapped(header) ? record.get(header) : "";
                        row.put(header, val != null ? val.trim() : "");
                    }
                    allRows.add(row);
                }
                return allRows;
            }
        }
    }

    private List<String> cleanHeaders(List<String> rawHeaders) {
        List<String> cleaned = new ArrayList<>();
        for (String h : rawHeaders) {
            if (h != null) {
                // Strip UTF-8 BOM if present
                String c = h.replace("\uFEFF", "").trim();
                if (!c.isEmpty()) {
                    cleaned.add(c);
                }
            }
        }
        return cleaned;
    }
}
