package com.sankhya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sankhya — AI-Powered Supermarket Analytics and Business Intelligence Platform.
 * Phase 1: Foundation, Architecture, Database, Authentication & UI Design.
 */
@SpringBootApplication
public class SankhyaApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(SankhyaApplication.class, args);
    }

    public static void loadDotEnv() {
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                for (String line : java.nio.file.Files.readAllLines(envPath)) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        // Strip quotes if any
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Best-effort local .env loading
        }
    }
}
