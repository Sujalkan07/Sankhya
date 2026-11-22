package com.sankhya.dataset.schema;

import java.util.List;

public class CanonicalField {

    private final String name;
    private final String displayName;
    private final String dataType;
    private final boolean required;
    private final String description;
    private final List<String> aliases;

    public CanonicalField(String name, String displayName, String dataType, boolean required,
                          String description, List<String> aliases) {
        this.name = name;
        this.displayName = displayName;
        this.dataType = dataType;
        this.required = required;
        this.description = description;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public String canonicalName() {
        return name;
    }

    public String name() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String getDataType() {
        return dataType;
    }

    public String dataType() {
        return dataType;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean required() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String description() {
        return description;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> aliases() {
        return aliases;
    }

    public boolean matches(String headerName) {
        if (headerName == null) return false;
        String clean = headerName.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        if (clean.equalsIgnoreCase(this.name)) return true;
        for (String alias : aliases) {
            String cleanAlias = alias.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
            if (clean.equalsIgnoreCase(cleanAlias)) return true;
        }
        return false;
    }
}
