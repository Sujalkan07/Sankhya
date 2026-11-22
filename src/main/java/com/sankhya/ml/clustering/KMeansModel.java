package com.sankhya.ml.clustering;

import com.sankhya.ml.features.FeatureScaler;
import java.util.Arrays;
import java.util.List;

public class KMeansModel {

    private final int k;
    private final double[][] centroids;
    private final String[] clusterLabels;
    private final FeatureScaler scaler;
    private final double inertia;
    private final double silhouetteScore;

    public KMeansModel(int k, double[][] centroids, String[] clusterLabels,
                       FeatureScaler scaler, double inertia, double silhouetteScore) {
        this.k = k;
        this.centroids = centroids;
        this.clusterLabels = clusterLabels;
        this.scaler = scaler;
        this.inertia = inertia;
        this.silhouetteScore = silhouetteScore;
    }

    public int predict(double[] unscaledFeatures) {
        double[] scaled = scaler.transform(unscaledFeatures);
        return findNearestCentroid(scaled);
    }

    public double distanceToCentroid(double[] unscaledFeatures, int clusterId) {
        double[] scaled = scaler.transform(unscaledFeatures);
        return euclideanDistance(scaled, centroids[clusterId]);
    }

    public int findNearestCentroid(double[] scaledPoint) {
        int bestCluster = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < k; i++) {
            double dist = euclideanDistance(scaledPoint, centroids[i]);
            if (dist < minDistance) {
                minDistance = dist;
                bestCluster = i;
            }
        }
        return bestCluster;
    }

    public static double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    public int getK() {
        return k;
    }

    public double[][] getCentroids() {
        return centroids;
    }

    public String[] getClusterLabels() {
        return clusterLabels;
    }

    public FeatureScaler getScaler() {
        return scaler;
    }

    public double getInertia() {
        return inertia;
    }

    public double getSilhouetteScore() {
        return silhouetteScore;
    }
}
