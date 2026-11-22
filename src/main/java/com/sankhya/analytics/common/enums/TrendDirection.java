package com.sankhya.analytics.common.enums;

public enum TrendDirection {
    UP("Up"),
    DOWN("Down"),
    FLAT("Flat");

    private final String displayName;

    TrendDirection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
