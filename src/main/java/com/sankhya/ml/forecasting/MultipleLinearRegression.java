package com.sankhya.ml.forecasting;

import java.util.Arrays;

public class MultipleLinearRegression {

    private double[] weights;
    private double intercept;
    private double rmse;
    private double rSquared;
    private double mae;
    private double mape;

    public void fit(double[][] X, double[] y) {
        int n = X.length;
        if (n == 0 || X[0].length == 0) {
            this.intercept = 0.0;
            this.weights = new double[0];
            return;
        }

        int p = X[0].length;
        // Augment X with leading 1 for intercept: matrix [n x (p+1)]
        double[][] Xaug = new double[n][p + 1];
        for (int i = 0; i < n; i++) {
            Xaug[i][0] = 1.0;
            System.arraycopy(X[i], 0, Xaug[i], 1, p);
        }

        // Compute (X^T * X + lambda * I)
        int m = p + 1;
        double[][] XtX = new double[m][m];
        double[] Xty = new double[m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                Xty[j] += Xaug[i][j] * y[i];
                for (int k = 0; k < m; k++) {
                    XtX[j][k] += Xaug[i][j] * Xaug[i][k];
                }
            }
        }

        // Ridge regularization: add lambda to diagonal (except intercept)
        double lambda = 1e-4;
        for (int j = 1; j < m; j++) {
            XtX[j][j] += lambda;
        }

        // Solve linear system XtX * beta = Xty using Gaussian elimination with partial pivoting
        double[] beta = solveLinearSystem(XtX, Xty);

        this.intercept = beta[0];
        this.weights = Arrays.copyOfRange(beta, 1, m);

        // Evaluate fit metrics on training set
        evaluate(X, y);
    }

    public double predict(double[] x) {
        double pred = intercept;
        for (int i = 0; i < x.length && i < weights.length; i++) {
            pred += weights[i] * x[i];
        }
        return Math.max(0.0, pred);
    }

    private void evaluate(double[][] X, double[] y) {
        int n = X.length;
        double sumY = 0.0;
        for (double val : y) sumY += val;
        double meanY = sumY / n;

        double ssTot = 0.0;
        double ssRes = 0.0;
        double sumAbsErr = 0.0;
        double sumPercErr = 0.0;
        int nonZeroCount = 0;

        for (int i = 0; i < n; i++) {
            double pred = predict(X[i]);
            double err = y[i] - pred;
            ssRes += err * err;
            ssTot += (y[i] - meanY) * (y[i] - meanY);
            sumAbsErr += Math.abs(err);
            if (y[i] > 1e-6) {
                sumPercErr += Math.abs(err) / y[i];
                nonZeroCount++;
            }
        }

        this.rmse = Math.sqrt(ssRes / n);
        this.mae = sumAbsErr / n;
        this.mape = nonZeroCount > 0 ? (sumPercErr / nonZeroCount) * 100.0 : 0.0;
        this.rSquared = (ssTot > 1e-6) ? Math.max(0.0, 1.0 - (ssRes / ssTot)) : 1.0;
    }

    private static double[] solveLinearSystem(double[][] A, double[] b) {
        int n = b.length;
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
        }
        double[] x = Arrays.copyOf(b, n);

        for (int p = 0; p < n; p++) {
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(M[i][p]) > Math.abs(M[max][p])) {
                    max = i;
                }
            }

            double[] tempM = M[p];
            M[p] = M[max];
            M[max] = tempM;

            double tempX = x[p];
            x[p] = x[max];
            x[max] = tempX;

            if (Math.abs(M[p][p]) <= 1e-12) {
                M[p][p] = 1e-6; // prevent zero division
            }

            for (int i = p + 1; i < n; i++) {
                double alpha = M[i][p] / M[p][p];
                x[i] -= alpha * x[p];
                for (int j = p; j < n; j++) {
                    M[i][j] -= alpha * M[p][j];
                }
            }
        }

        // Back substitution
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += M[i][j] * x[j];
            }
            x[i] = (x[i] - sum) / M[i][i];
        }

        return x;
    }

    public double[] getWeights() {
        return weights;
    }

    public double getIntercept() {
        return intercept;
    }

    public double getRmse() {
        return rmse;
    }

    public double getrSquared() {
        return rSquared;
    }

    public double getMae() {
        return mae;
    }

    public double getMape() {
        return mape;
    }
}
