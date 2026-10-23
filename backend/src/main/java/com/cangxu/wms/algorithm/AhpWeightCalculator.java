package com.cangxu.wms.algorithm;

/**
 * Analytic Hierarchy Process (AHP) weights using the geometric-mean method.
 * The pairwise matrix must be positive and square.
 */
public final class AhpWeightCalculator {

    private static final double[] RI = {
            0.0, 0.0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49
    };

    private AhpWeightCalculator() {
    }

    public record AhpResult(double[] weights, double lambdaMax, double consistencyIndex, double consistencyRatio) {
    }

    public static AhpResult compute(double[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("判断矩阵不能为空");
        }
        int n = matrix.length;
        for (double[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("判断矩阵必须为方阵");
            }
            for (double value : row) {
                if (value <= 0) {
                    throw new IllegalArgumentException("判断矩阵元素必须为正数");
                }
            }
        }

        double[] geometricMeans = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double logSum = 0;
            for (int j = 0; j < n; j++) {
                logSum += Math.log(matrix[i][j]);
            }
            geometricMeans[i] = Math.exp(logSum / n);
            sum += geometricMeans[i];
        }

        double[] weights = new double[n];
        for (int i = 0; i < n; i++) {
            weights[i] = geometricMeans[i] / sum;
        }

        double lambdaMax = 0;
        for (int i = 0; i < n; i++) {
            double aw = 0;
            for (int j = 0; j < n; j++) {
                aw += matrix[i][j] * weights[j];
            }
            lambdaMax += aw / weights[i];
        }
        lambdaMax /= n;

        double ci = n <= 2 ? 0 : (lambdaMax - n) / (n - 1);
        double ri = n < RI.length ? RI[n] : 1.49;
        double cr = ri == 0 ? 0 : ci / ri;
        return new AhpResult(weights, lambdaMax, ci, cr);
    }
}
