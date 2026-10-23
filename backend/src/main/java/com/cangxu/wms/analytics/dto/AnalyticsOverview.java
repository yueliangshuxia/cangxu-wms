package com.cangxu.wms.analytics.dto;

import java.util.List;

public record AnalyticsOverview(
        int days,
        List<TrendPoint> trend,
        List<CategoryShare> categories,
        long totalQty,
        List<String> zones,
        List<String> aisles,
        List<HeatCell> heatmap,
        KpiSet kpis,
        ExperimentSummary experiment
) {

    public record TrendPoint(String date, long inbound, long outbound) {
    }

    public record CategoryShare(String name, double value, long qty) {
    }

    public record HeatCell(String zone, String aisle, int value) {
    }

    public record KpiSet(
            long inboundQty,
            long outboundQty,
            double turnover,
            double utilization
    ) {
    }

    public record ExperimentSummary(
            List<StrategyResult> strategies,
            double distanceImprovement,
            double timeImprovement,
            double utilizationImprovement,
            double consistencyRatio,
            long orders,
            int skus,
            String generatedAt,
            String source
    ) {
    }

    public record StrategyResult(
            String method,
            double distance,
            double time,
            double utilization
    ) {
    }
}
