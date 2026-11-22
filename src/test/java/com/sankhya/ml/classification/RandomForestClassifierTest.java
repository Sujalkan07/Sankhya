package com.sankhya.ml.classification;

import com.sankhya.ml.enums.DemandClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RandomForestClassifier Unit Tests")
class RandomForestClassifierTest {

    @Test
    @DisplayName("Random Forest trains ensemble and predicts demand classes with high accuracy")
    void testRandomForestClassification() {
        // Simple synthetic dataset:
        // High velocity (> 5.0) -> HIGH
        // Low velocity (< 2.0) -> LOW
        double[][] features = new double[][]{
                {100.0, 6.0, 50.0, 20.0},
                {120.0, 7.5, 45.0, 15.0},
                {95.0, 5.8, 55.0, 10.0},
                {110.0, 8.0, 40.0, 25.0},
                {10.0, 0.5, 150.0, 80.0},
                {15.0, 0.8, 120.0, 90.0},
                {8.0, 0.4, 200.0, 60.0},
                {12.0, 0.6, 180.0, 75.0}
        };

        DemandClass[] labels = new DemandClass[]{
                DemandClass.HIGH,
                DemandClass.HIGH,
                DemandClass.HIGH,
                DemandClass.HIGH,
                DemandClass.LOW,
                DemandClass.LOW,
                DemandClass.LOW,
                DemandClass.LOW
        };

        String[] featureNames = {"totalUnits", "dailyVelocity", "unitPrice", "currentStock"};
        RandomForestClassifier rf = new RandomForestClassifier(20, 4, 2, featureNames.length);
        rf.fit(features, labels);

        // Evaluation
        RandomForestClassifier.EvaluationMetrics eval = rf.evaluate(features, labels, featureNames);
        assertNotNull(eval);
        assertTrue(eval.accuracy >= 0.85, "Accuracy should be high on distinct synthetic data");
        assertTrue(eval.f1Score > 0.0);
        assertNotNull(eval.featureImportanceMap);
        assertTrue(eval.featureImportanceMap.containsKey("dailyVelocity"));

        // Prediction on unseen test sample
        RandomForestClassifier.PredictionResult resHigh = rf.predict(new double[]{105.0, 7.0, 48.0, 18.0});
        assertEquals(DemandClass.HIGH, resHigh.predictedClass);
        assertTrue(resHigh.confidence > 0.5);

        RandomForestClassifier.PredictionResult resLow = rf.predict(new double[]{9.0, 0.5, 170.0, 70.0});
        assertEquals(DemandClass.LOW, resLow.predictedClass);
        assertTrue(resLow.confidence > 0.5);
    }
}
