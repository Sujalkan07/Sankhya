package com.sankhya.inventory.entity;

import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_snapshots",
        indexes = {
                @Index(name = "idx_inventory_workspace", columnList = "workspace_id"),
                @Index(name = "idx_inventory_product", columnList = "product_id"),
                @Index(name = "idx_inventory_date", columnList = "snapshot_date"),
                @Index(name = "idx_inventory_code", columnList = "inventory_id")
        })
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "inventory_id", nullable = false, length = 100)
    private String inventoryId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "opening_stock", nullable = false)
    private Integer openingStock;

    @Column(name = "received_stock", nullable = false)
    private Integer receivedStock = 0;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Column(name = "closing_stock", nullable = false)
    private Integer closingStock;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "reorder_level")
    private Integer reorderLevel = 0;

    @Column(name = "warehouse_or_store", length = 100)
    private String warehouseOrStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_import_id")
    private DatasetImport datasetImport;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InventorySnapshot() {}

    public InventorySnapshot(Workspace workspace, Dataset dataset, String inventoryId,
                             LocalDate snapshotDate, String productId, Integer closingStock) {
        this.workspace = workspace;
        this.dataset = dataset;
        this.inventoryId = inventoryId;
        this.snapshotDate = snapshotDate;
        this.productId = productId;
        this.closingStock = closingStock;
        this.openingStock = closingStock;
    }

    public InventorySnapshot(Workspace workspace, String inventoryId, String productId,
                             LocalDate snapshotDate, Integer openingStock, Integer receivedStock,
                             Integer soldQuantity, Integer closingStock, Integer reorderLevel,
                             String warehouseOrStore, DatasetImport datasetImport) {
        this.workspace = workspace;
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.snapshotDate = snapshotDate;
        this.openingStock = openingStock;
        this.receivedStock = receivedStock != null ? receivedStock : 0;
        this.soldQuantity = soldQuantity != null ? soldQuantity : 0;
        this.closingStock = closingStock;
        this.reorderLevel = reorderLevel != null ? reorderLevel : 0;
        this.warehouseOrStore = warehouseOrStore;
        this.datasetImport = datasetImport;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.closingStock == null && this.openingStock != null) {
            int rec = this.receivedStock != null ? this.receivedStock : 0;
            int sold = this.soldQuantity != null ? this.soldQuantity : 0;
            this.closingStock = Math.max(0, this.openingStock + rec - sold);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public Integer getOpeningStock() {
        return openingStock;
    }

    public void setOpeningStock(Integer openingStock) {
        this.openingStock = openingStock;
    }

    public Integer getReceivedStock() {
        return receivedStock;
    }

    public void setReceivedStock(Integer receivedStock) {
        this.receivedStock = receivedStock;
    }

    public Integer getStockReceived() {
        return receivedStock;
    }

    public void setStockReceived(Integer stockReceived) {
        this.receivedStock = stockReceived;
    }

    public Integer getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = soldQuantity;
    }

    public Integer getStockSold() {
        return soldQuantity;
    }

    public void setStockSold(Integer stockSold) {
        this.soldQuantity = stockSold;
    }

    public Integer getClosingStock() {
        return closingStock;
    }

    public void setClosingStock(Integer closingStock) {
        this.closingStock = closingStock;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public String getWarehouseOrStore() {
        return warehouseOrStore;
    }

    public void setWarehouseOrStore(String warehouseOrStore) {
        this.warehouseOrStore = warehouseOrStore;
    }

    public String getWarehouseLocation() {
        return warehouseOrStore;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseOrStore = warehouseLocation;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetImport getDatasetImport() {
        return datasetImport;
    }

    public void setDatasetImport(DatasetImport datasetImport) {
        this.datasetImport = datasetImport;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
