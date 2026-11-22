package com.sankhya.workspace.enums;

public enum OnboardingStatus {
    STORE_SETUP("Store Setup"),
    DATASET_ONBOARDING("Dataset Ingestion"),
    PARTIALLY_COMPLETED("Setup Partially Completed"),
    COMPLETED_WITHOUT_DATA("Setup Completed Without Data"),
    SKIPPED("Setup Skipped"),
    COMPLETED("Completed");

    private final String displayName;

    OnboardingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
