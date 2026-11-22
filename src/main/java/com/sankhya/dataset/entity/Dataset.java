package com.sankhya.dataset.entity;

import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "datasets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workspace_dataset_type", columnNames = {"workspace_id", "dataset_type"})
        },
        indexes = {
                @Index(name = "idx_dataset_workspace", columnList = "workspace_id"),
                @Index(name = "idx_dataset_type", columnList = "dataset_type")
        })
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "dataset_type", nullable = false, length = 50)
    private DatasetType datasetType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "record_count", nullable = false)
    private Long recordCount = 0L;

    @Column(name = "last_imported_at")
    private LocalDateTime lastImportedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Dataset() {}

    public Dataset(Workspace workspace, DatasetType datasetType, String name, String description) {
        this.workspace = workspace;
        this.datasetType = datasetType;
        this.name = name;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public DatasetType getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(DatasetType datasetType) {
        this.datasetType = datasetType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getRecordCount() {
        return recordCount != null ? recordCount : 0L;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = (long) recordCount;
    }

    public LocalDateTime getLastImportedAt() {
        return lastImportedAt;
    }

    public void setLastImportedAt(LocalDateTime lastImportedAt) {
        this.lastImportedAt = lastImportedAt;
    }

    public LocalDateTime getLastUpdated() {
        return updatedAt != null ? updatedAt : lastImportedAt;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.updatedAt = lastUpdated;
        this.lastImportedAt = lastUpdated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
