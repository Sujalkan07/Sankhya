package com.sankhya.ml.entity;

import com.sankhya.ml.enums.ExecutionStatus;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_model_executions", indexes = {
        @Index(name = "idx_exec_workspace", columnList = "workspace_id"),
        @Index(name = "idx_exec_model", columnList = "model_id"),
        @Index(name = "idx_exec_status", columnList = "status")
})
public class MLModelExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private MLModel model;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "train_record_count")
    private Integer trainRecordCount = 0;

    @Column(name = "test_record_count")
    private Integer testRecordCount = 0;

    @Column(name = "hyperparameters_json", columnDefinition = "TEXT")
    private String hyperparametersJson;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "artifact_path")
    private String artifactPath;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    public MLModelExecution() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MLModel getModel() {
        return model;
    }

    public void setModel(MLModel model) {
        this.model = model;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getTrainRecordCount() {
        return trainRecordCount;
    }

    public void setTrainRecordCount(Integer trainRecordCount) {
        this.trainRecordCount = trainRecordCount;
    }

    public Integer getTestRecordCount() {
        return testRecordCount;
    }

    public void setTestRecordCount(Integer testRecordCount) {
        this.testRecordCount = testRecordCount;
    }

    public String getHyperparametersJson() {
        return hyperparametersJson;
    }

    public void setHyperparametersJson(String hyperparametersJson) {
        this.hyperparametersJson = hyperparametersJson;
    }

    public String getMetricsJson() {
        return metricsJson;
    }

    public void setMetricsJson(String metricsJson) {
        this.metricsJson = metricsJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }
}
