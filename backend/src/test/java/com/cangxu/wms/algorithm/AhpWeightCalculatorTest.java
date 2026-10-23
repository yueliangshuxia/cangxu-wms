package com.cangxu.wms.algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AhpWeightCalculatorTest {

    @Test
    void computesWeightsAndPassesConsistencyCheck() {
        double[][] matrix = {
                {1, 3, 5, 7},
                {1.0 / 3, 1, 3, 5},
                {1.0 / 5, 1.0 / 3, 1, 3},
                {1.0 / 7, 1.0 / 5, 1.0 / 3, 1}
        };

        AhpWeightCalculator.AhpResult result = AhpWeightCalculator.compute(matrix);

        double sum = 0;
        for (double weight : result.weights()) {
            sum += weight;
        }
        assertEquals(1.0, sum, 1e-9);
        assertTrue(result.consistencyRatio() < 0.1, "CR must be below 0.1");
        assertTrue(result.weights()[0] > result.weights()[1]);
        assertTrue(result.weights()[1] > result.weights()[2]);
        assertTrue(result.weights()[2] > result.weights()[3]);
    }
}
