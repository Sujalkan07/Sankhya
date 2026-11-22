package com.sankhya.analytics.recommendations.enums;

public enum RecommendationSeverity {
    CRITICAL("Critical", 1),
    HIGH("High", 2),
    MEDIUM("Medium", 3),
    LOW("Low", 4),
    INFO("Info", 5);

    private final String displayName;
    private final int rank;

    RecommendationSeverity(String displayName, int rank) {
        this.displayName = displayName;
        this.rank = rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRank() {
        return rank;
    }
}
