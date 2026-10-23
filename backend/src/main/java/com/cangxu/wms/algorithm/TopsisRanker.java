package com.cangxu.wms.algorithm;

/**
 * TOPSIS ranking. The decision matrix is m alternatives x n criteria.
 * benefit[i] = true means a higher value is better; false means a lower value is better.
 */
public final class TopsisRanker {

    private TopsisRanker() {
    }

    public static double[] rank(double[][] matrix, double[] weights, boolean[] benefit) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("决策矩阵不能为空");
        }
        int m = matrix.length;
        int n = matrix[0].length;
        if (weights == null || weights.length != n) {
            throw new IllegalArgumentException("权重数量必须与指标数量一致");
        }
        if (benefit == null || benefit.length != n) {
            throw new IllegalArgumentException("指标方向数量必须与指标数量一致");
        }
        for (double[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("决策矩阵每行长度必须一致");
            }
        }

        double[][] normalized = new double[m][n];
        for (int j = 0; j < n; j++) {
            double norm = 0;
            for (int i = 0; i < m; i++) {
                norm += matrix[i][j] * matrix[i][j];
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < m; i++) {
                normalized[i][j] = norm == 0 ? 0 : matrix[i][j] / norm;
            }
        }

        double[][] weighted = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                weighted[i][j] = normalized[i][j] * weights[j];
            }
        }

        double[] best = new double[n];
        double[] worst = new double[n];
        for (int j = 0; j < n; j++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < m; i++) {
                min = Math.min(min, weighted[i][j]);
                max = Math.max(max, weighted[i][j]);
            }
            best[j] = benefit[j] ? max : min;
            worst[j] = benefit[j] ? min : max;
        }

        double[] closeness = new double[m];
        for (int i = 0; i < m; i++) {
            double dBest = 0;
            double dWorst = 0;
            for (int j = 0; j < n; j++) {
                dBest += Math.pow(weighted[i][j] - best[j], 2);
                dWorst += Math.pow(weighted[i][j] - worst[j], 2);
            }
            dBest = Math.sqrt(dBest);
            dWorst = Math.sqrt(dWorst);
            double denominator = dBest + dWorst;
            closeness[i] = denominator == 0 ? 0 : dWorst / denominator;
        }
        return closeness;
    }
}
