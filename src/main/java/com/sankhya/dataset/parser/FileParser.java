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
}
