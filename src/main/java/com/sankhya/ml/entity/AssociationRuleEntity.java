package com.sankhya.ml.entity;

import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_association_rules", indexes = {
        @Index(name = "idx_rule_workspace", columnList = "workspace_id"),
        @Index(name = "idx_rule_exec", columnList = "execution_id"),
        @Index(name = "idx_rule_lift", columnList = "lift")
})
public class AssociationRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private MLModelExecution execution;

    @Column(name = "antecedent", nullable = false, length = 500)
    private String antecedent;

    @Column(name = "consequent", nullable = false, length = 500)
    private String consequent;

    @Column(name = "antecedent_product_id")
    private Long antecedentProductId;

    @Column(name = "consequent_product_id")
    private Long consequentProductId;

    @Column(name = "support", nullable = false)
    private double support;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "lift", nullable = false)
    private double lift;

    @Column(name = "conviction")
    private Double conviction;

    @Column(name = "rule_type", length = 50)
    private String ruleType = "CROSS_SELL";

    @Column(name = "actionable_recommendation", columnDefinition = "TEXT")
    private String actionableRecommendation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AssociationRuleEntity() {}

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

    public String getAntecedent() {
        return antecedent;
    }

    public void setAntecedent(String antecedent) {
        this.antecedent = antecedent;
    }

    public String getConsequent() {
        return consequent;
    }

    public void setConsequent(String consequent) {
        this.consequent = consequent;
    }

    public Long getAntecedentProductId() {
        return antecedentProductId;
    }

    public void setAntecedentProductId(Long antecedentProductId) {
        this.antecedentProductId = antecedentProductId;
    }

    public Long getConsequentProductId() {
        return consequentProductId;
    }

    public void setConsequentProductId(Long consequentProductId) {
        this.consequentProductId = consequentProductId;
    }

    public double getSupport() {
        return support;
    }

    public void setSupport(double support) {
        this.support = support;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public double getLift() {
        return lift;
    }

    public void setLift(double lift) {
        this.lift = lift;
    }

    public Double getConviction() {
        return conviction;
    }

    public void setConviction(Double conviction) {
        this.conviction = conviction;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getActionableRecommendation() {
        return actionableRecommendation;
    }

    public void setActionableRecommendation(String actionableRecommendation) {
        this.actionableRecommendation = actionableRecommendation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
