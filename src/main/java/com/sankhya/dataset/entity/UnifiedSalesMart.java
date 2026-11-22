package com.sankhya.dataset.entity;

import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "unified_sales_mart",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mart_workspace_store_row", columnNames = {"workspace_id", "store_id", "dataset_import_id", "row_number"})
        },
        indexes = {
                @Index(name = "idx_mart_workspace_date", columnList = "workspace_id, transaction_date"),
                @Index(name = "idx_mart_workspace_tx", columnList = "workspace_id, transaction_id"),
                @Index(name = "idx_mart_workspace_cust", columnList = "workspace_id, customer_id"),
                @Index(name = "idx_mart_workspace_prod", columnList = "workspace_id, product_id"),
                @Index(name = "idx_mart_workspace_cat", columnList = "workspace_id, category"),
                @Index(name = "idx_mart_workspace_import", columnList = "workspace_id, dataset_import_id"),
                @Index(name = "idx_mart_store_date", columnList = "store_id, transaction_date")
        })
public class UnifiedSalesMart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false, length = 50)
    private String storeId = "STORE-001";

    @Column(name = "store_name", nullable = false, length = 150)
    private String storeName = "Naveen Store";

    @Column(name = "store_owner", nullable = false, length = 150)
    private String storeOwner = "Sujal Kansal";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_import_id")
    private DatasetImport datasetImport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    // Receipt context
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "cashier_name", length = 100)
    private String cashierName;

    // Customer context
    @Column(name = "customer_id", length = 50)
    private String customerId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "customer_segment", length = 50)
    private String customerSegment;

    // Product context
    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 255)
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

    // Line item metrics
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal revenue;

    @Column(name = "gross_profit", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossProfit;

    // Latest inventory context
    @Column(name = "latest_inventory_snapshot_date")
    private LocalDate latestInventorySnapshotDate;

    @Column(name = "current_closing_stock")
    private Integer currentClosingStock;

    @Column(name = "current_reorder_level")
    private Integer currentReorderLevel;

    @Column(name = "current_inventory_status", length = 50)
    private String currentInventoryStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UnifiedSalesMart() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Canonical calculations
    public static BigDecimal calculateRevenue(int quantity, BigDecimal unitPrice, BigDecimal discount) {
        if (quantity <= 0 || unitPrice == null) return BigDecimal.ZERO;
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal disc = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal net = gross.subtract(disc);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    public static BigDecimal calculateGrossProfit(BigDecimal revenue, int quantity, BigDecimal unitCost) {
        if (revenue == null) revenue = BigDecimal.ZERO;
        if (quantity <= 0 || unitCost == null) return revenue;
        BigDecimal cogs = unitCost.multiply(BigDecimal.valueOf(quantity));
        return revenue.subtract(cogs);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreOwner() {
        return storeOwner;
    }

    public void setStoreOwner(String storeOwner) {
        this.storeOwner = storeOwner;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public DatasetImport getDatasetImport() {
        return datasetImport;
    }

    public void setDatasetImport(DatasetImport datasetImport) {
        this.datasetImport = datasetImport;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public LocalDate getLatestInventorySnapshotDate() {
        return latestInventorySnapshotDate;
    }

    public void setLatestInventorySnapshotDate(LocalDate latestInventorySnapshotDate) {
        this.latestInventorySnapshotDate = latestInventorySnapshotDate;
    }

    public Integer getCurrentClosingStock() {
        return currentClosingStock;
    }

    public void setCurrentClosingStock(Integer currentClosingStock) {
        this.currentClosingStock = currentClosingStock;
    }

    public Integer getCurrentReorderLevel() {
        return currentReorderLevel;
    }

    public void setCurrentReorderLevel(Integer currentReorderLevel) {
        this.currentReorderLevel = currentReorderLevel;
    }

    public String getCurrentInventoryStatus() {
        return currentInventoryStatus;
    }

    public void setCurrentInventoryStatus(String currentInventoryStatus) {
        this.currentInventoryStatus = currentInventoryStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
