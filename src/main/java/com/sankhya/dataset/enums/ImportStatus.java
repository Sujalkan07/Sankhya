package com.sankhya.dataset.enums;

public enum ImportStatus {
    PENDING("Pending", "File uploaded and awaiting processing"),
    PROCESSING("Processing", "File contents are being read and parsed"),
    VALIDATING("Validating", "Schema, data types, and referential integrity being validated"),
    READY_FOR_CONFIRMATION("Ready for Confirmation", "Validation complete; awaiting user confirmation to import"),
    COMPLETED("Completed", "Successfully imported into workspace"),
    COMPLETED_WITH_ERRORS("Completed with Warnings", "Imported with non-critical warnings or skipped invalid records"),
    FAILED("Failed", "Critical errors encountered; import rejected"),
    CANCELLED("Cancelled", "Import cancelled by user");

    private final String displayName;
    private final String description;

    ImportStatus(String displayName, String description) {
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
