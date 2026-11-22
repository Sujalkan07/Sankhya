package com.sankhya.dataset.enums;

public enum ErrorSeverity {
    CRITICAL("Critical", "Blocks import of the record"),
    ERROR("Error", "Validation failure for this record"),
    WARNING("Warning", "Non-critical issue that can be safely defaulted or flagged");

    private final String displayName;
    private final String description;

    ErrorSeverity(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
