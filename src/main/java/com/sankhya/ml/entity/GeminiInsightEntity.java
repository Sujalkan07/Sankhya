package com.sankhya.ml.entity;

import com.sankhya.ml.enums.InsightCategory;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_gemini_insights", indexes = {
        @Index(name = "idx_ai_insight_workspace", columnList = "workspace_id"),
        @Index(name = "idx_ai_insight_cat", columnList = "category")
})
public class GeminiInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private InsightCategory category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "full_narrative", columnDefinition = "MEDIUMTEXT")
    private String fullNarrative;

    @Column(name = "key_findings_json", columnDefinition = "TEXT")
    private String keyFindingsJson;

    @Column(name = "action_items_json", columnDefinition = "TEXT")
    private String actionItemsJson;

    @Column(name = "grounded_metrics_json", columnDefinition = "TEXT")
    private String groundedMetricsJson;

    @Column(name = "gemini_model_version", length = 50)
    private String geminiModelVersion;

    @Column(name = "prompt_tokens")
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens")
    private Integer completionTokens = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public GeminiInsightEntity() {}

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

    public InsightCategory getCategory() {
        return category;
    }

    public void setCategory(InsightCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFullNarrative() {
        return fullNarrative;
    }

    public void setFullNarrative(String fullNarrative) {
        this.fullNarrative = fullNarrative;
    }

    public String getKeyFindingsJson() {
        return keyFindingsJson;
    }

    public void setKeyFindingsJson(String keyFindingsJson) {
        this.keyFindingsJson = keyFindingsJson;
    }

    public String getActionItemsJson() {
        return actionItemsJson;
    }

    public void setActionItemsJson(String actionItemsJson) {
        this.actionItemsJson = actionItemsJson;
    }

    public String getGroundedMetricsJson() {
        return groundedMetricsJson;
    }

    public void setGroundedMetricsJson(String groundedMetricsJson) {
        this.groundedMetricsJson = groundedMetricsJson;
    }

    public String getGeminiModelVersion() {
        return geminiModelVersion;
    }

    public void setGeminiModelVersion(String geminiModelVersion) {
        this.geminiModelVersion = geminiModelVersion;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
