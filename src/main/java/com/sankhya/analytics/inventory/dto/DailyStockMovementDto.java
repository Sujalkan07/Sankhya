package com.sankhya.analytics.inventory.dto;

public class DailyStockMovementDto {

    private String date;
    private long openingStock;
    private long receivedStock;
    private long soldQuantity;
    private long closingStock;

    public DailyStockMovementDto() {}

    public DailyStockMovementDto(String date, long openingStock, long receivedStock, long soldQuantity, long closingStock) {
        this.date = date;
        this.openingStock = openingStock;
        this.receivedStock = receivedStock;
        this.soldQuantity = soldQuantity;
        this.closingStock = closingStock;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getOpeningStock() {
        return openingStock;
    }

    public void setOpeningStock(long openingStock) {
        this.openingStock = openingStock;
    }

    public long getReceivedStock() {
        return receivedStock;
    }

    public void setReceivedStock(long receivedStock) {
        this.receivedStock = receivedStock;
    }

    public long getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(long soldQuantity) {
        this.soldQuantity = soldQuantity;
    }

    public long getClosingStock() {
        return closingStock;
    }

    public void setClosingStock(long closingStock) {
        this.closingStock = closingStock;
    }
}
