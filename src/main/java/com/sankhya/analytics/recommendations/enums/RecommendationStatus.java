package com.sankhya.analytics.recommendations.enums;

public enum RecommendationStatus {
    NEW("New"),
    REVIEWED("Reviewed"),
    DISMISSED("Dismissed"),
    RESOLVED("Resolved");

    private final String displayName;

    RecommendationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
