package com.sankhya.ml.entity;

import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_forecast_results", indexes = {
        @Index(name = "idx_forecast_workspace", columnList = "workspace_id"),
        @Index(name = "idx_forecast_exec", columnList = "execution_id"),
        @Index(name = "idx_forecast_date", columnList = "forecast_date")
})
public class ForecastResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private MLModelExecution execution;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "predicted_revenue", nullable = false)
    private double predictedRevenue;

    @Column(name = "baseline_revenue")
    private Double baselineRevenue;

    @Column(name = "lower_bound")
    private Double lowerBound;

    @Column(name = "upper_bound")
    private Double upperBound;

    @Column(name = "horizon_day", nullable = false)
    private int horizonDay;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ForecastResultEntity() {}

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

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }

    public double getPredictedRevenue() {
        return predictedRevenue;
    }

    public void setPredictedRevenue(double predictedRevenue) {
        this.predictedRevenue = predictedRevenue;
    }

    public Double getBaselineRevenue() {
        return baselineRevenue;
    }

    public void setBaselineRevenue(Double baselineRevenue) {
        this.baselineRevenue = baselineRevenue;
    }

    public Double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Double upperBound) {
        this.upperBound = upperBound;
    }

    public int getHorizonDay() {
        return horizonDay;
    }

    public void setHorizonDay(int horizonDay) {
        this.horizonDay = horizonDay;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
