package com.sankhya.dataset.parser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FileParser {

    boolean supports(String extension);

    ParsedFileData readPreviewAndHeaders(File file, int maxPreviewRows) throws IOException;

    List<Map<String, String>> readAllRows(File file) throws IOException;

    default ParsedFileData parsePreview(File file, int maxPreviewRows) throws IOException {
        return readPreviewAndHeaders(file, maxPreviewRows);
    }

    default ParsedFileData parseFull(File file) throws IOException {
        ParsedFileData preview = readPreviewAndHeaders(file, 1);
        List<Map<String, String>> allRows = readAllRows(file);
        return new ParsedFileData(preview.getHeaders(), allRows, allRows.size());
    }

    default void processRowsStreaming(File file, java.util.function.BiConsumer<Long, Map<String, String>> rowConsumer) throws IOException {
        List<Map<String, String>> allRows = readAllRows(file);
        long rowNum = 2;
        for (Map<String, String> row : allRows) {
            rowConsumer.accept(rowNum++, row);
        }
    }

    default void processBatches(File file, int batchSize, java.util.function.Consumer<List<Map<String, String>>> batchConsumer) throws IOException {
        List<Map<String, String>> allRows = readAllRows(file);
        for (int i = 0; i < allRows.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allRows.size());
            batchConsumer.accept(allRows.subList(i, end));
        }
    }
}
