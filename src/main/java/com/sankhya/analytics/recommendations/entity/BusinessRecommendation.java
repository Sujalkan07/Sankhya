package com.sankhya.analytics.recommendations.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sankhya.analytics.recommendations.enums.RecommendationCategory;
import com.sankhya.analytics.recommendations.enums.RecommendationSeverity;
import com.sankhya.analytics.recommendations.enums.RecommendationStatus;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_recommendations",
        indexes = {
                @Index(name = "idx_rec_workspace", columnList = "workspace_id"),
                @Index(name = "idx_rec_category", columnList = "category"),
                @Index(name = "idx_rec_status", columnList = "status"),
                @Index(name = "idx_rec_severity", columnList = "severity")
        })
public class BusinessRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private RecommendationCategory category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private RecommendationSeverity severity = RecommendationSeverity.MEDIUM;

    @Column(name = "priority", nullable = false)
    private int priority = 3;

    @Column(name = "supporting_metric", length = 100)
    private String supportingMetric;

    @Column(name = "observed_value", length = 100)
    private String observedValue;

    @Column(name = "comparison_value", length = 100)
    private String comparisonValue;

    @Column(name = "recommended_action", nullable = false, columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "rule_identifier", nullable = false, length = 100)
    private String ruleIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RecommendationStatus status = RecommendationStatus.NEW;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BusinessRecommendation() {}

    public BusinessRecommendation(Workspace workspace, RecommendationCategory category,
                                  String title, String description, RecommendationSeverity severity,
                                  int priority, String supportingMetric, String observedValue,
                                  String comparisonValue, String recommendedAction, String ruleIdentifier) {
        this.workspace = workspace;
        this.category = category;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.priority = priority;
        this.supportingMetric = supportingMetric;
        this.observedValue = observedValue;
        this.comparisonValue = comparisonValue;
        this.recommendedAction = recommendedAction;
        this.ruleIdentifier = ruleIdentifier;
        this.status = RecommendationStatus.NEW;
        this.generatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public BusinessRecommendation(Workspace workspace, RecommendationCategory category,
                                  RecommendationSeverity severity, int priority, String ruleIdentifier,
                                  String title, String description, String recommendedAction,
                                  String supportingMetric, String observedValue, String comparisonValue) {
        this(workspace, category, title, description, severity, priority, supportingMetric, observedValue, comparisonValue, recommendedAction, ruleIdentifier);
    }

    public String getActionPrompt() {
        return recommendedAction;
    }

    @PrePersist
    protected void onCreate() {
        this.generatedAt = LocalDateTime.now();
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

    public void setId(Long id) {
        this.id = id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public RecommendationCategory getCategory() {
        return category;
    }

    public void setCategory(RecommendationCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecommendationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RecommendationSeverity severity) {
        this.severity = severity;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getSupportingMetric() {
        return supportingMetric;
    }

    public void setSupportingMetric(String supportingMetric) {
        this.supportingMetric = supportingMetric;
    }

    public String getObservedValue() {
        return observedValue;
    }

    public void setObservedValue(String observedValue) {
        this.observedValue = observedValue;
    }

    public String getComparisonValue() {
        return comparisonValue;
    }

    public void setComparisonValue(String comparisonValue) {
        this.comparisonValue = comparisonValue;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getRuleIdentifier() {
        return ruleIdentifier;
    }

    public void setRuleIdentifier(String ruleIdentifier) {
        this.ruleIdentifier = ruleIdentifier;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
