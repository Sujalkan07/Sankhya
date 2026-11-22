package com.sankhya.ml.classification;

import com.sankhya.ml.enums.DemandClass;

import java.util.*;

public class DecisionTree {

    public static class Node {
        public boolean isLeaf;
        public DemandClass predictedClass;
        public int featureIndex;
        public double splitThreshold;
        public Node left;
        public Node right;

        public Node(DemandClass predictedClass) {
            this.isLeaf = true;
            this.predictedClass = predictedClass;
        }

        public Node(int featureIndex, double splitThreshold, Node left, Node right) {
            this.isLeaf = false;
            this.featureIndex = featureIndex;
            this.splitThreshold = splitThreshold;
            this.left = left;
            this.right = right;
        }
    }

    private final int maxDepth;
    private final int minSamplesSplit;
    private final Random random;
    private Node root;

    public DecisionTree(int maxDepth, int minSamplesSplit, Random random) {
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
        this.random = random;
    }

    public void train(double[][] features, DemandClass[] labels, double[] featureImportanceAccumulator) {
        List<Integer> sampleIndices = new ArrayList<>();
        for (int i = 0; i < features.length; i++) sampleIndices.add(i);
        this.root = buildTree(features, labels, sampleIndices, 0, featureImportanceAccumulator);
    }

    private Node buildTree(double[][] features, DemandClass[] labels, List<Integer> indices, int depth, double[] featureImportances) {
        if (indices.isEmpty()) {
            return new Node(DemandClass.MEDIUM);
        }

        DemandClass majorityClass = getMajorityClass(labels, indices);
        if (depth >= maxDepth || indices.size() < minSamplesSplit || isPure(labels, indices)) {
            return new Node(majorityClass);
        }

        int numFeatures = features[0].length;
        int featuresToTry = Math.max(1, (int) Math.sqrt(numFeatures));

        List<Integer> featureIndices = new ArrayList<>();
        for (int i = 0; i < numFeatures; i++) featureIndices.add(i);
        Collections.shuffle(featureIndices, random);
        List<Integer> selectedFeatures = featureIndices.subList(0, featuresToTry);

        double currentGini = calculateGini(labels, indices);
        double bestGiniGain = -1.0;
        int bestFeature = -1;
        double bestThreshold = 0.0;
        List<Integer> bestLeft = null;
        List<Integer> bestRight = null;

        for (int featIdx : selectedFeatures) {
            // Test candidate split thresholds
            for (int idx : indices) {
                double threshold = features[idx][featIdx];
                List<Integer> left = new ArrayList<>();
                List<Integer> right = new ArrayList<>();

                for (int i : indices) {
                    if (features[i][featIdx] <= threshold) {
                        left.add(i);
                    } else {
                        right.add(i);
                    }
                }

                if (left.isEmpty() || right.isEmpty()) continue;

                double leftGini = calculateGini(labels, left);
                double rightGini = calculateGini(labels, right);
                double weightedGini = ((double) left.size() / indices.size()) * leftGini
                        + ((double) right.size() / indices.size()) * rightGini;

                double gain = currentGini - weightedGini;
                if (gain > bestGiniGain) {
                    bestGiniGain = gain;
                    bestFeature = featIdx;
                    bestThreshold = threshold;
                    bestLeft = left;
                    bestRight = right;
                }
            }
        }

        if (bestGiniGain <= 0.0 || bestLeft == null || bestRight == null) {
            return new Node(majorityClass);
        }

        if (featureImportances != null && bestFeature >= 0 && bestFeature < featureImportances.length) {
            featureImportances[bestFeature] += bestGiniGain * indices.size();
        }

        Node leftNode = buildTree(features, labels, bestLeft, depth + 1, featureImportances);
        Node rightNode = buildTree(features, labels, bestRight, depth + 1, featureImportances);

        return new Node(bestFeature, bestThreshold, leftNode, rightNode);
    }

    public DemandClass predict(double[] sample) {
        Node current = root;
        while (current != null && !current.isLeaf) {
            if (sample[current.featureIndex] <= current.splitThreshold) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return current != null ? current.predictedClass : DemandClass.MEDIUM;
    }

    private double calculateGini(DemandClass[] labels, List<Integer> indices) {
        if (indices.isEmpty()) return 0.0;
        Map<DemandClass, Integer> counts = new EnumMap<>(DemandClass.class);
        for (int idx : indices) {
            counts.merge(labels[idx], 1, Integer::sum);
        }
        double sumSq = 0.0;
        double n = indices.size();
        for (int count : counts.values()) {
            double p = count / n;
            sumSq += p * p;
        }
        return 1.0 - sumSq;
    }

    private boolean isPure(DemandClass[] labels, List<Integer> indices) {
        DemandClass first = labels[indices.get(0)];
        for (int idx : indices) {
            if (labels[idx] != first) return false;
        }
        return true;
    }

    private DemandClass getMajorityClass(DemandClass[] labels, List<Integer> indices) {
        Map<DemandClass, Integer> counts = new EnumMap<>(DemandClass.class);
        for (int idx : indices) {
            counts.merge(labels[idx], 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DemandClass.MEDIUM);
    }
}
