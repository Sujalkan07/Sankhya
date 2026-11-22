package com.sankhya.sales.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_transactions",
        indexes = {
                @Index(name = "idx_sales_workspace", columnList = "workspace_id"),
                @Index(name = "idx_sales_tx_id", columnList = "transaction_id"),
                @Index(name = "idx_sales_date", columnList = "transaction_date"),
                @Index(name = "idx_sales_product", columnList = "product_id"),
                @Index(name = "idx_sales_customer", columnList = "customer_id"),
                @Index(name = "idx_sales_payment", columnList = "payment_method")
        })
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "customer_id", length = 50)
    private String customerId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal revenue;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "store_id", length = 100)
    private String storeId;

    @Column(name = "cashier_name", length = 100)
    private String cashierName;

    @Column(name = "status", length = 50)
    private String status = "COMPLETED";

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_import_id")
    private DatasetImport datasetImport;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SalesTransaction() {}

    public SalesTransaction(Workspace workspace, Dataset dataset, String transactionId,
                            LocalDateTime transactionDate, String productId, Integer quantity,
                            BigDecimal unitPrice, BigDecimal revenue, String paymentMethod) {
        this.workspace = workspace;
        this.dataset = dataset;
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.revenue = revenue;
        this.paymentMethod = paymentMethod;
    }

    public SalesTransaction(Workspace workspace, String transactionId, LocalDateTime transactionDate,
                            String customerId, String productId, Integer quantity, BigDecimal unitPrice,
                            BigDecimal discount, BigDecimal revenue, String paymentMethod,
                            String storeId, DatasetImport datasetImport) {
        this.workspace = workspace;
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.revenue = revenue != null ? revenue : calculateRevenue(quantity, unitPrice, this.discount);
        this.paymentMethod = paymentMethod;
        this.storeId = storeId;
        this.datasetImport = datasetImport;
    }

    public static BigDecimal calculateRevenue(int quantity, BigDecimal unitPrice, BigDecimal discount) {
        if (unitPrice == null) return BigDecimal.ZERO;
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal disc = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal rev = gross.subtract(disc);
        return rev.compareTo(BigDecimal.ZERO) > 0 ? rev : BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.revenue == null && this.quantity != null && this.unitPrice != null) {
            this.revenue = calculateRevenue(this.quantity, this.unitPrice, this.discount);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
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

    public LocalDateTime getTransactionTimestamp() {
        return transactionDate;
    }

    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionDate = transactionTimestamp;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

    public BigDecimal getDiscountAmount() {
        return discount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discount = discountAmount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getTotalAmount() {
        return revenue;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.revenue = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreLocation() {
        return storeId;
    }

    public void setStoreLocation(String storeLocation) {
        this.storeId = storeLocation;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
