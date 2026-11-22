package com.sankhya.ml.forecasting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MultipleLinearRegression Unit Tests")
class MultipleLinearRegressionTest {

    @Test
    @DisplayName("Linear Regression solves coefficients, calculates metrics, and forecasts forward")
    void testMultipleLinearRegression() {
        // True function: y = 50 + 2 * x1 + 5 * x2
        double[][] X = new double[][]{
                {1.0, 2.0},
                {2.0, 3.0},
                {3.0, 4.0},
                {4.0, 5.0},
                {5.0, 6.0},
                {6.0, 7.0},
                {7.0, 8.0}
        };

        double[] y = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            y[i] = 50.0 + 2.0 * X[i][0] + 5.0 * X[i][1];
        }

        MultipleLinearRegression lr = new MultipleLinearRegression();
        lr.fit(X, y);

        assertTrue(lr.getrSquared() > 0.95, "R² should be close to 1.0 for linear data");
        assertTrue(lr.getRmse() < 1.0, "RMSE should be very low");
        assertTrue(lr.getMae() < 1.0);

        // Predict on next point: x1=8, x2=9 -> expected 50 + 16 + 45 = 111
        double pred = lr.predict(new double[]{8.0, 9.0});
        assertEquals(111.0, pred, 1.0);
    }
}
