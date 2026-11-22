package com.sankhya.ml.clustering;

import com.sankhya.ml.features.FeatureScaler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KMeansService Unit Tests")
class KMeansServiceTest {

    private final KMeansService kMeansService = new KMeansService();

    @Test
    @DisplayName("KMeans clusters well-separated 2D synthetic clusters accurately")
    void testKMeansClustering() {
        // Group 1: Centered around (10, 10)
        // Group 2: Centered around (100, 100)
        double[][] data = new double[][]{
                {9.0, 10.5, 50.0, 10.0},
                {10.0, 9.8, 52.0, 10.5},
                {11.2, 10.1, 48.0, 9.5},
                {10.5, 10.2, 51.0, 10.2},
                {98.0, 101.5, 500.0, 100.0},
                {100.2, 99.1, 520.0, 105.0},
                {102.1, 100.5, 490.0, 98.0},
                {99.5, 100.2, 510.0, 102.0}
        };

        KMeansModel model = kMeansService.fit(data, 2, 50);

        assertNotNull(model);
        assertEquals(2, model.getK());
        assertTrue(model.getInertia() >= 0.0);
        assertTrue(model.getSilhouetteScore() > 0.0, "Silhouette score should be positive for well-separated clusters");

        // Points in group 1 should receive the same cluster
        int c0 = model.predict(new double[]{10.0, 10.0, 50.0, 10.0});
        int c1 = model.predict(new double[]{10.5, 9.5, 51.0, 10.1});
        assertEquals(c0, c1);

        // Point in group 2 should receive a different cluster
        int c2 = model.predict(new double[]{100.0, 100.0, 500.0, 100.0});
        assertNotEquals(c0, c2);

        // Check cluster labels
        assertNotNull(model.getClusterLabels());
        assertEquals(2, model.getClusterLabels().length);
    }

    @Test
    @DisplayName("FeatureScaler standardizes and inverts features correctly")
    void testFeatureScaler() {
        double[][] data = new double[][]{
                {10.0, 100.0},
                {20.0, 200.0},
                {30.0, 300.0}
        };

        FeatureScaler scaler = new FeatureScaler(data);
        assertEquals(20.0, scaler.getMeans()[0], 1e-4);
        assertEquals(200.0, scaler.getMeans()[1], 1e-4);

        double[] scaled = scaler.transform(new double[]{20.0, 200.0});
        assertEquals(0.0, scaled[0], 1e-4);
        assertEquals(0.0, scaled[1], 1e-4);

        double[] unscaled = scaler.inverseTransform(scaled);
        assertEquals(20.0, unscaled[0], 1e-4);
        assertEquals(200.0, unscaled[1], 1e-4);
    }
}
