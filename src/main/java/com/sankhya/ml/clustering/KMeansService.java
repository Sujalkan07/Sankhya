package com.sankhya.ml.clustering;

import com.sankhya.ml.features.FeatureScaler;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KMeansService {

    private final Random random = new Random(42);

    public KMeansModel fit(double[][] rawData, int k, int maxIterations) {
        if (rawData == null || rawData.length < k || k <= 0) {
            throw new IllegalArgumentException("Invalid dataset or k for KMeans clustering");
        }

        int n = rawData.length;
        int d = rawData[0].length;

        // 1. Scale data
        FeatureScaler scaler = new FeatureScaler(rawData);
        double[][] data = scaler.transform(rawData);

        // 2. K-Means++ initialization
        double[][] centroids = initCentroidsKMeansPlusPlus(data, k, d);

        int[] assignments = new int[n];
        boolean converged = false;
        int iteration = 0;

        while (!converged && iteration < maxIterations) {
            iteration++;
            converged = true;

            // Step A: Assignment step
            for (int i = 0; i < n; i++) {
                int nearest = findNearestCentroid(data[i], centroids);
                if (nearest != assignments[i]) {
                    assignments[i] = nearest;
                    converged = false;
                }
            }

            // Step B: Update step
            double[][] newCentroids = new double[k][d];
            int[] counts = new int[k];

            for (int i = 0; i < n; i++) {
                int c = assignments[i];
                counts[c]++;
                for (int j = 0; j < d; j++) {
                    newCentroids[c][j] += data[i][j];
                }
            }

            for (int c = 0; c < k; c++) {
                if (counts[c] > 0) {
                    for (int j = 0; j < d; j++) {
                        centroids[c][j] = newCentroids[c][j] / counts[c];
                    }
                } else {
                    // Empty cluster fallback: pick random point
                    centroids[c] = Arrays.copyOf(data[random.nextInt(n)], d);
                }
            }
        }

        // 3. Calculate Inertia (SSE)
        double inertia = calculateInertia(data, centroids, assignments);

        // 4. Calculate Silhouette Score
        double silhouetteScore = calculateSilhouetteScore(data, assignments, k);

        // 5. Generate descriptive business labels for each cluster based on unscaled centroids
        String[] labels = generateClusterLabels(centroids, scaler, k);

        return new KMeansModel(k, centroids, labels, scaler, inertia, silhouetteScore);
    }

    private double[][] initCentroidsKMeansPlusPlus(double[][] data, int k, int d) {
        int n = data.length;
        double[][] centroids = new double[k][d];

        // First centroid at random
        int firstIdx = random.nextInt(n);
        centroids[0] = Arrays.copyOf(data[firstIdx], d);

        double[] minDistSquared = new double[n];
        Arrays.fill(minDistSquared, Double.MAX_VALUE);

        for (int c = 1; c < k; c++) {
            double totalWeight = 0.0;
            for (int i = 0; i < n; i++) {
                double dist = KMeansModel.euclideanDistance(data[i], centroids[c - 1]);
                double distSq = dist * dist;
                if (distSq < minDistSquared[i]) {
                    minDistSquared[i] = distSq;
                }
                totalWeight += minDistSquared[i];
            }

            double r = random.nextDouble() * totalWeight;
            double cumulative = 0.0;
            int selectedIdx = n - 1;
            for (int i = 0; i < n; i++) {
                cumulative += minDistSquared[i];
                if (cumulative >= r) {
                    selectedIdx = i;
                    break;
                }
            }
            centroids[c] = Arrays.copyOf(data[selectedIdx], d);
        }

        return centroids;
    }

    private int findNearestCentroid(double[] point, double[][] centroids) {
        int best = 0;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < centroids.length; i++) {
            double dist = KMeansModel.euclideanDistance(point, centroids[i]);
            if (dist < min) {
                min = dist;
                best = i;
            }
        }
        return best;
    }

    private double calculateInertia(double[][] data, double[][] centroids, int[] assignments) {
        double sse = 0.0;
        for (int i = 0; i < data.length; i++) {
            double d = KMeansModel.euclideanDistance(data[i], centroids[assignments[i]]);
            sse += d * d;
        }
        return sse;
    }

    public double calculateSilhouetteScore(double[][] data, int[] assignments, int k) {
        if (k <= 1 || data.length <= k) return 0.0;

        int n = data.length;
        // Sample down if n is very large for performance (e.g. max 500 samples)
        int sampleSize = Math.min(n, 500);
        int step = Math.max(1, n / sampleSize);

        double totalScore = 0.0;
        int evaluatedCount = 0;

        for (int i = 0; i < n; i += step) {
            int currentCluster = assignments[i];
            double[] currentPoint = data[i];

            // Mean intra-cluster distance a(i)
            double a = 0.0;
            int sameCount = 0;
            double[] otherClusterDistSum = new double[k];
            int[] otherClusterCounts = new int[k];

            for (int j = 0; j < n; j += step) {
                if (i == j) continue;
                double dist = KMeansModel.euclideanDistance(currentPoint, data[j]);
                int otherCluster = assignments[j];
                if (otherCluster == currentCluster) {
                    a += dist;
                    sameCount++;
                } else {
                    otherClusterDistSum[otherCluster] += dist;
                    otherClusterCounts[otherCluster]++;
                }
            }

            a = (sameCount > 0) ? (a / sameCount) : 0.0;

            // Smallest mean inter-cluster distance b(i)
            double b = Double.MAX_VALUE;
            for (int c = 0; c < k; c++) {
                if (c != currentCluster && otherClusterCounts[c] > 0) {
                    double avgDist = otherClusterDistSum[c] / otherClusterCounts[c];
                    if (avgDist < b) {
                        b = avgDist;
                    }
                }
            }

            if (b == Double.MAX_VALUE) b = 0.0;

            double s = (Math.max(a, b) > 0) ? ((b - a) / Math.max(a, b)) : 0.0;
            totalScore += s;
            evaluatedCount++;
        }

        return evaluatedCount > 0 ? (totalScore / evaluatedCount) : 0.0;
    }

    private String[] generateClusterLabels(double[][] centroids, FeatureScaler scaler, int k) {
        String[] labels = new String[k];
        double[][] unscaled = new double[k][];
        for (int i = 0; i < k; i++) {
            unscaled[i] = scaler.inverseTransform(centroids[i]);
        }

        // Sort clusters by monetary value to assign intuitive business names
        Integer[] order = new Integer[k];
        for (int i = 0; i < k; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> unscaled[i][2]).reversed());

        String[] templates = {
                "High-Value Champions",
                "Loyal Frequent Shoppers",
                "Emerging Regulars",
                "At-Risk Occasionals",
                "Lapsed Budget Buyers",
                "New Explorers"
        };

        for (int rank = 0; rank < k; rank++) {
            int originalIdx = order[rank];
            labels[originalIdx] = (rank < templates.length) ? templates[rank] : ("Customer Cluster #" + (rank + 1));
        }

        return labels;
    }
}
