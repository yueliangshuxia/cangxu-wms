package com.cangxu.wms.algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopsisRankerTest {

    @Test
    void ranksBetterAlternativeFirst() {
        double[][] matrix = {
                {10, 10},
                {1, 1},
                {5, 5}
        };
        double[] weights = {0.5, 0.5};
        boolean[] benefit = {true, true};

        double[] closeness = TopsisRanker.rank(matrix, weights, benefit);

        assertEquals(3, closeness.length);
        assertEquals(1.0, closeness[0], 1e-9);
        assertEquals(0.0, closeness[1], 1e-9);
        assertTrue(closeness[0] > closeness[2]);
        assertTrue(closeness[2] > closeness[1]);
    }

    @Test
    void respectsCostCriteria() {
        double[][] matrix = {
                {10, 1},
                {1, 10},
                {5, 5}
        };
        double[] weights = {0.5, 0.5};
        boolean[] benefit = {true, false};

        double[] closeness = TopsisRanker.rank(matrix, weights, benefit);

        assertTrue(closeness[0] > closeness[1]);
        assertEquals(1.0, closeness[0], 1e-9);
        assertEquals(0.0, closeness[1], 1e-9);
    }
}
