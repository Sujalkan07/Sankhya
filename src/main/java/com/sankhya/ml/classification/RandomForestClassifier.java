package com.sankhya.ml.classification;

import com.sankhya.ml.enums.DemandClass;

import java.util.*;

public class RandomForestClassifier {

    private final int numTrees;
    private final int maxDepth;
    private final int minSamplesSplit;
    private final List<DecisionTree> trees = new ArrayList<>();
    private final double[] featureImportances;
    private final Random random = new Random(42);

    public static class PredictionResult {
        public final DemandClass predictedClass;
        public final double confidence;
        public final Map<DemandClass, Double> classProbabilities;

        public PredictionResult(DemandClass predictedClass, double confidence, Map<DemandClass, Double> classProbabilities) {
            this.predictedClass = predictedClass;
            this.confidence = confidence;
            this.classProbabilities = classProbabilities;
        }
    }

    public static class EvaluationMetrics {
        public double accuracy;
        public double precision;
        public double recall;
        public double f1Score;
        public Map<String, Double> featureImportanceMap;
    }

    public RandomForestClassifier(int numTrees, int maxDepth, int minSamplesSplit, int numFeatures) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
        this.featureImportances = new double[numFeatures];
    }

    public void fit(double[][] features, DemandClass[] labels) {
        int n = features.length;
        trees.clear();
        Arrays.fill(featureImportances, 0.0);

        for (int t = 0; t < numTrees; t++) {
            // Bootstrap sample with replacement
            double[][] bootFeatures = new double[n][];
            DemandClass[] bootLabels = new DemandClass[n];
            for (int i = 0; i < n; i++) {
                int rIdx = random.nextInt(n);
                bootFeatures[i] = features[rIdx];
                bootLabels[i] = labels[rIdx];
            }

            DecisionTree tree = new DecisionTree(maxDepth, minSamplesSplit, random);
            tree.train(bootFeatures, bootLabels, featureImportances);
            trees.add(tree);
        }

        // Normalize feature importances
        double sum = Arrays.stream(featureImportances).sum();
        if (sum > 0) {
            for (int i = 0; i < featureImportances.length; i++) {
                featureImportances[i] /= sum;
            }
        }
    }

    public PredictionResult predict(double[] sample) {
        Map<DemandClass, Integer> votes = new EnumMap<>(DemandClass.class);
        for (DecisionTree tree : trees) {
            DemandClass c = tree.predict(sample);
            votes.merge(c, 1, Integer::sum);
        }

        DemandClass bestClass = DemandClass.MEDIUM;
        int maxVotes = 0;
        for (Map.Entry<DemandClass, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                bestClass = entry.getKey();
            }
        }

        double confidence = (double) maxVotes / (trees.isEmpty() ? 1 : trees.size());
        Map<DemandClass, Double> probs = new EnumMap<>(DemandClass.class);
        for (DemandClass dc : DemandClass.values()) {
            probs.put(dc, (double) votes.getOrDefault(dc, 0) / (trees.isEmpty() ? 1 : trees.size()));
        }

        return new PredictionResult(bestClass, confidence, probs);
    }

    public EvaluationMetrics evaluate(double[][] testFeatures, DemandClass[] testLabels, String[] featureNames) {
        EvaluationMetrics metrics = new EvaluationMetrics();
        if (testFeatures.length == 0) return metrics;

        int correct = 0;
        int total = testFeatures.length;

        Map<DemandClass, Integer> truePositives = new EnumMap<>(DemandClass.class);
        Map<DemandClass, Integer> falsePositives = new EnumMap<>(DemandClass.class);
        Map<DemandClass, Integer> falseNegatives = new EnumMap<>(DemandClass.class);

        for (int i = 0; i < total; i++) {
            PredictionResult res = predict(testFeatures[i]);
            DemandClass actual = testLabels[i];
            if (res.predictedClass == actual) {
                correct++;
                truePositives.merge(actual, 1, Integer::sum);
            } else {
                falsePositives.merge(res.predictedClass, 1, Integer::sum);
                falseNegatives.merge(actual, 1, Integer::sum);
            }
        }

        metrics.accuracy = (double) correct / total;

        double sumPrecision = 0.0;
        double sumRecall = 0.0;
        double sumF1 = 0.0;
        int activeClasses = 0;

        for (DemandClass dc : DemandClass.values()) {
            int tp = truePositives.getOrDefault(dc, 0);
            int fp = falsePositives.getOrDefault(dc, 0);
            int fn = falseNegatives.getOrDefault(dc, 0);

            if (tp + fp > 0 || tp + fn > 0) {
                activeClasses++;
                double prec = (tp + fp > 0) ? ((double) tp / (tp + fp)) : 0.0;
                double rec = (tp + fn > 0) ? ((double) tp / (tp + fn)) : 0.0;
                double f1 = (prec + rec > 0) ? (2 * prec * rec / (prec + rec)) : 0.0;

                sumPrecision += prec;
                sumRecall += rec;
                sumF1 += f1;
            }
        }

        metrics.precision = activeClasses > 0 ? (sumPrecision / activeClasses) : 1.0;
        metrics.recall = activeClasses > 0 ? (sumRecall / activeClasses) : 1.0;
        metrics.f1Score = activeClasses > 0 ? (sumF1 / activeClasses) : 1.0;

        metrics.featureImportanceMap = new LinkedHashMap<>();
        for (int i = 0; i < featureNames.length && i < featureImportances.length; i++) {
            metrics.featureImportanceMap.put(featureNames[i], featureImportances[i]);
        }

        return metrics;
    }

    public double[] getFeatureImportances() {
        return featureImportances;
    }
}
