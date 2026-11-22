package com.sankhya.ml.entity;

import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_customer_clusters", indexes = {
        @Index(name = "idx_cluster_workspace", columnList = "workspace_id"),
        @Index(name = "idx_cluster_exec", columnList = "execution_id"),
        @Index(name = "idx_cluster_num", columnList = "cluster_id")
})
public class CustomerClusterAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private MLModelExecution execution;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_code", length = 100)
    private String customerCode;

    @Column(name = "cluster_id", nullable = false)
    private int clusterId;

    @Column(name = "cluster_label", nullable = false, length = 100)
    private String clusterLabel;

    @Column(name = "recency_days", nullable = false)
    private double recencyDays;

    @Column(name = "frequency", nullable = false)
    private double frequency;

    @Column(name = "monetary", nullable = false)
    private double monetary;

    @Column(name = "average_transaction_value", nullable = false)
    private double averageTransactionValue;

    @Column(name = "distance_to_centroid")
    private Double distanceToCentroid;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    public CustomerClusterAssignment() {}

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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterLabel() {
        return clusterLabel;
    }

    public void setClusterLabel(String clusterLabel) {
        this.clusterLabel = clusterLabel;
    }

    public double getRecencyDays() {
        return recencyDays;
    }

    public void setRecencyDays(double recencyDays) {
        this.recencyDays = recencyDays;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public double getMonetary() {
        return monetary;
    }

    public void setMonetary(double monetary) {
        this.monetary = monetary;
    }

    public double getAverageTransactionValue() {
        return averageTransactionValue;
    }

    public void setAverageTransactionValue(double averageTransactionValue) {
        this.averageTransactionValue = averageTransactionValue;
    }

    public Double getDistanceToCentroid() {
        return distanceToCentroid;
    }

    public void setDistanceToCentroid(Double distanceToCentroid) {
        this.distanceToCentroid = distanceToCentroid;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
