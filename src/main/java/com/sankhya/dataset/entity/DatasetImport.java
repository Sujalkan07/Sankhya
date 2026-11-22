package com.sankhya.dataset.entity;

import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ImportStatus;
import com.sankhya.user.entity.User;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_imports",
        indexes = {
                @Index(name = "idx_import_workspace", columnList = "workspace_id"),
                @Index(name = "idx_import_dataset", columnList = "dataset_id"),
                @Index(name = "idx_import_status", columnList = "status"),
                @Index(name = "idx_import_created", columnList = "created_at")
        })
public class DatasetImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "dataset_type", nullable = false, length = 50)
    private DatasetType datasetType;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ImportStatus status = ImportStatus.PENDING;

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "valid_rows", nullable = false)
    private int validRows = 0;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows = 0;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows = 0;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "completeness_score")
    private Double completenessScore;

    @Column(name = "validity_score")
    private Double validityScore;

    @Column(name = "uniqueness_score")
    private Double uniquenessScore;

    @Column(name = "consistency_score")
    private Double consistencyScore;

    @Column(name = "referential_score")
    private Double referentialScore;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "column_mapping_json", columnDefinition = "TEXT")
    private String columnMappingJson;

    @Column(name = "is_synthetic", nullable = false)
    private boolean synthetic = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public DatasetImport() {}

    public DatasetImport(Workspace workspace, DatasetType datasetType, String originalFilename,
                         String storedFilename, Long fileSize, User uploadedBy) {
        this.workspace = workspace;
        this.datasetType = datasetType;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.status = ImportStatus.PENDING;
    }

    public DatasetImport(Workspace workspace, Dataset dataset, DatasetType datasetType,
                         String originalFilename, String storedFilename, Long fileSize) {
        this.workspace = workspace;
        this.dataset = dataset;
        this.datasetType = datasetType;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.status = ImportStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
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

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getSourceFileName() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getStoredFilePath() {
        return storedFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public void setStatus(ImportStatus status) {
        this.status = status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(int invalidRows) {
        this.invalidRows = invalidRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public void setDuplicateRows(int duplicateRows) {
        this.duplicateRows = duplicateRows;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Double getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(Double completenessScore) {
        this.completenessScore = completenessScore;
    }

    public Double getValidityScore() {
        return validityScore;
    }

    public void setValidityScore(Double validityScore) {
        this.validityScore = validityScore;
    }

    public Double getUniquenessScore() {
        return uniquenessScore;
    }

    public void setUniquenessScore(Double uniquenessScore) {
        this.uniquenessScore = uniquenessScore;
    }

    public Double getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(Double consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public Double getReferentialScore() {
        return referentialScore;
    }

    public void setReferentialScore(Double referentialScore) {
        this.referentialScore = referentialScore;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getColumnMappingJson() {
        return columnMappingJson;
    }

    public void setColumnMappingJson(String columnMappingJson) {
        this.columnMappingJson = columnMappingJson;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public void setSynthetic(boolean synthetic) {
        this.synthetic = synthetic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
