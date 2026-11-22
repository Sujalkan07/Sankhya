package com.sankhya.dataset.parser;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileParserFactory {

    private final List<FileParser> parsers;

    public FileParserFactory(List<FileParser> parsers) {
        this.parsers = parsers;
    }

    public FileParser getParser(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return parsers.stream()
                .filter(p -> p.supports(ext))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file format: ." + ext + ". Supported formats are .csv and .xlsx"));
    }
}
