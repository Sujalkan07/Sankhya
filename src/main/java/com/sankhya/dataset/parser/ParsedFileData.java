package com.sankhya.dataset.parser;

import java.util.List;
import java.util.Map;

public class ParsedFileData {

    private final List<String> headers;
    private final List<Map<String, String>> previewRows;
    private final int totalRowCount;

    public ParsedFileData(List<String> headers, List<Map<String, String>> previewRows, int totalRowCount) {
        this.headers = headers;
        this.previewRows = previewRows;
        this.totalRowCount = totalRowCount;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<Map<String, String>> getPreviewRows() {
        return previewRows;
    }

    public List<Map<String, String>> getRows() {
        return previewRows;
    }

    public int getTotalRowCount() {
        return totalRowCount;
    }

    public int getTotalRows() {
        return totalRowCount;
    }
}
