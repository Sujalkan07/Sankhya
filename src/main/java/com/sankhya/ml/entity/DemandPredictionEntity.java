package com.sankhya.ml.entity;

import com.sankhya.ml.enums.DemandClass;
import com.sankhya.ml.enums.StockoutRiskLevel;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_demand_predictions", indexes = {
        @Index(name = "idx_demand_workspace", columnList = "workspace_id"),
        @Index(name = "idx_demand_exec", columnList = "execution_id"),
        @Index(name = "idx_demand_class", columnList = "predicted_demand_class")
})
public class DemandPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private MLModelExecution execution;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_demand_class", nullable = false, length = 50)
    private DemandClass predictedDemandClass;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "recent_daily_velocity")
    private Double recentDailyVelocity;

    @Column(name = "predicted_units_next_7d")
    private Double predictedUnitsNext7d;

    @Enumerated(EnumType.STRING)
    @Column(name = "stockout_risk_level", length = 30)
    private StockoutRiskLevel stockoutRiskLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DemandPredictionEntity() {}

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

    public MLModelExecution getExecution() {
        return execution;
    }

    public void setExecution(MLModelExecution execution) {
        this.execution = execution;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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

    public DemandClass getPredictedDemandClass() {
        return predictedDemandClass;
    }

    public void setPredictedDemandClass(DemandClass predictedDemandClass) {
        this.predictedDemandClass = predictedDemandClass;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Double getRecentDailyVelocity() {
        return recentDailyVelocity;
    }

    public void setRecentDailyVelocity(Double recentDailyVelocity) {
        this.recentDailyVelocity = recentDailyVelocity;
    }

    public Double getPredictedUnitsNext7d() {
        return predictedUnitsNext7d;
    }

    public void setPredictedUnitsNext7d(Double predictedUnitsNext7d) {
        this.predictedUnitsNext7d = predictedUnitsNext7d;
    }

    public StockoutRiskLevel getStockoutRiskLevel() {
        return stockoutRiskLevel;
    }

    public void setStockoutRiskLevel(StockoutRiskLevel stockoutRiskLevel) {
        this.stockoutRiskLevel = stockoutRiskLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
