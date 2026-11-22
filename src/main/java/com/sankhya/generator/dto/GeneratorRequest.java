package com.sankhya.generator.dto;

public class GeneratorRequest {

    private int months = 24;
    private String scale = "MEDIUM";
    private boolean clearExisting = false;

    public GeneratorRequest() {
    }

    public GeneratorRequest(int months, String scale, boolean clearExisting) {
        this.months = months;
        this.scale = scale;
        this.clearExisting = clearExisting;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public boolean isClearExisting() {
        return clearExisting;
    }

    public void setClearExisting(boolean clearExisting) {
        this.clearExisting = clearExisting;
    }
}
