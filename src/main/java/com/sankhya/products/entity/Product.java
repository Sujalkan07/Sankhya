package com.sankhya.products.entity;

import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workspace_product_id", columnNames = {"workspace_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_product_workspace", columnList = "workspace_id"),
                @Index(name = "idx_product_sku", columnList = "product_id"),
                @Index(name = "idx_product_category", columnList = "category")
        })
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "subcategory", length = 100)
    private String subcategory;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "supplier", length = 150)
    private String supplier;

    @Column(name = "expiry_days")
    private Integer expiryDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_import_id")
    private DatasetImport datasetImport;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(Workspace workspace, String productId, String productName) {
        this.workspace = workspace;
        this.productId = productId;
        this.productName = productName;
        this.category = "General";
        this.unitCost = BigDecimal.ZERO;
        this.sellingPrice = BigDecimal.ZERO;
    }

    public Product(Workspace workspace, Dataset dataset, String productId, String productName) {
        this.workspace = workspace;
        this.dataset = dataset;
        this.productId = productId;
        this.productName = productName;
        this.category = "General";
        this.unitCost = BigDecimal.ZERO;
        this.sellingPrice = BigDecimal.ZERO;
    }

    public Product(Workspace workspace, String productId, String productName, String category,
                   String subcategory, String brand, BigDecimal unitCost, BigDecimal sellingPrice,
                   String supplier, Integer expiryDays, DatasetImport datasetImport) {
        this.workspace = workspace;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.subcategory = subcategory;
        this.brand = brand;
        this.unitCost = unitCost;
        this.sellingPrice = sellingPrice;
        this.supplier = supplier;
        this.expiryDays = expiryDays;
        this.datasetImport = datasetImport;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public Integer getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(Integer expiryDays) {
        this.expiryDays = expiryDays;
    }

    public Integer getShelfLifeDays() {
        return expiryDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.expiryDays = shelfLifeDays;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
