package com.sankhya.ml.features;

import java.util.Arrays;

/**
 * Standard Z-Score Scaler: (x - mean) / stdDev.
 * Pure Java, robust against zero variance and NaN.
 */
public class FeatureScaler {

    private final double[] means;
    private final double[] stdDevs;
    private final int numFeatures;

    public FeatureScaler(double[][] data) {
        if (data == null || data.length == 0 || data[0].length == 0) {
            this.numFeatures = 0;
            this.means = new double[0];
            this.stdDevs = new double[0];
            return;
        }

        int rows = data.length;
        this.numFeatures = data[0].length;
        this.means = new double[numFeatures];
        this.stdDevs = new double[numFeatures];

        // 1. Calculate means
        for (double[] row : data) {
            for (int j = 0; j < numFeatures; j++) {
                means[j] += row[j];
            }
        }
        for (int j = 0; j < numFeatures; j++) {
            means[j] /= rows;
        }

        // 2. Calculate standard deviations
        for (double[] row : data) {
            for (int j = 0; j < numFeatures; j++) {
                double diff = row[j] - means[j];
                stdDevs[j] += diff * diff;
            }
        }
        for (int j = 0; j < numFeatures; j++) {
            double variance = stdDevs[j] / (rows > 1 ? rows - 1 : 1);
            stdDevs[j] = Math.sqrt(variance);
            if (stdDevs[j] < 1e-6) {
                stdDevs[j] = 1.0; // avoid divide by zero for constant features
            }
        }
    }

    public double[] transform(double[] row) {
        if (numFeatures == 0 || row.length != numFeatures) {
            return Arrays.copyOf(row, row.length);
        }
        double[] scaled = new double[numFeatures];
        for (int j = 0; j < numFeatures; j++) {
            scaled[j] = (row[j] - means[j]) / stdDevs[j];
        }
        return scaled;
    }

    public double[][] transform(double[][] data) {
        double[][] scaled = new double[data.length][numFeatures];
        for (int i = 0; i < data.length; i++) {
            scaled[i] = transform(data[i]);
        }
        return scaled;
    }

    public double[] inverseTransform(double[] scaledRow) {
        if (numFeatures == 0 || scaledRow.length != numFeatures) {
            return Arrays.copyOf(scaledRow, scaledRow.length);
        }
        double[] orig = new double[numFeatures];
        for (int j = 0; j < numFeatures; j++) {
            orig[j] = (scaledRow[j] * stdDevs[j]) + means[j];
        }
        return orig;
    }

    public double[] getMeans() {
        return means;
    }

    public double[] getStdDevs() {
        return stdDevs;
    }
}
