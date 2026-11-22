package com.sankhya.workspace.enums;

public enum StoreFormat {
    SUPERMARKET("Supermarket"),
    HYPERMARKET("Hypermarket"),
    GROCERY_MART("Grocery Mart"),
    CONVENIENCE("Convenience Store");

    private final String displayName;

    StoreFormat(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StoreFormat fromString(String text) {
        if (text == null || text.isBlank()) {
            return SUPERMARKET;
        }
        for (StoreFormat sf : StoreFormat.values()) {
            if (sf.name().equalsIgnoreCase(text.trim()) || sf.displayName.equalsIgnoreCase(text.trim())) {
                return sf;
            }
        }
        return SUPERMARKET;
    }
}
