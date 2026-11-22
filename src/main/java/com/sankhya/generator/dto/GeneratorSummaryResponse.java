package com.sankhya.generator.dto;

public class GeneratorSummaryResponse {

    private boolean success;
    private String message;
    private int productsGenerated;
    private int customersGenerated;
    private int transactionsGenerated;
    private int inventorySnapshotsGenerated;
    private int durationMonths;

    public GeneratorSummaryResponse() {
    }

    public GeneratorSummaryResponse(boolean success, String message, int productsGenerated, int customersGenerated,
                                    int transactionsGenerated, int inventorySnapshotsGenerated, int durationMonths) {
        this.success = success;
        this.message = message;
        this.productsGenerated = productsGenerated;
        this.customersGenerated = customersGenerated;
        this.transactionsGenerated = transactionsGenerated;
        this.inventorySnapshotsGenerated = inventorySnapshotsGenerated;
        this.durationMonths = durationMonths;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getProductsGenerated() {
        return productsGenerated;
    }

    public void setProductsGenerated(int productsGenerated) {
        this.productsGenerated = productsGenerated;
    }

    public int getCustomersGenerated() {
        return customersGenerated;
    }

    public void setCustomersGenerated(int customersGenerated) {
        this.customersGenerated = customersGenerated;
    }

    public int getTransactionsGenerated() {
        return transactionsGenerated;
    }

    public void setTransactionsGenerated(int transactionsGenerated) {
        this.transactionsGenerated = transactionsGenerated;
    }

    public int getInventorySnapshotsGenerated() {
        return inventorySnapshotsGenerated;
    }

    public void setInventorySnapshotsGenerated(int inventorySnapshotsGenerated) {
        this.inventorySnapshotsGenerated = inventorySnapshotsGenerated;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(int durationMonths) {
        this.durationMonths = durationMonths;
    }
}
