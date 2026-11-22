package com.sankhya.ml.association;

import java.util.Set;

public class AssociationRuleDto {

    private final Set<String> antecedent;
    private final Set<String> consequent;
    private final double support;
    private final double confidence;
    private final double lift;
    private final double conviction;
    private final String recommendation;

    public AssociationRuleDto(Set<String> antecedent, Set<String> consequent,
                              double support, double confidence, double lift, double conviction,
                              String recommendation) {
        this.antecedent = antecedent;
        this.consequent = consequent;
        this.support = support;
        this.confidence = confidence;
        this.lift = lift;
        this.conviction = conviction;
        this.recommendation = recommendation;
    }

    public Set<String> getAntecedent() {
        return antecedent;
    }

    public Set<String> getConsequent() {
        return consequent;
    }

    public double getSupport() {
        return support;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getLift() {
        return lift;
    }

    public double getConviction() {
        return conviction;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
