package com.cangxu.wms.dashboard.dto;

import java.util.List;

public record DashboardOverview(
        long productCount,
        long supplierCount,
        long customerCount,
        long userCount,
        long inventoryQty,
        long lockedQty,
        double stockAccuracy,
        double turnoverDays,
        List<TrendPoint> trend,
        List<ZoneOccupancy> zones,
        List<ActivityItem> activities
) {
    public record TrendPoint(String date, long inbound, long outbound) {
    }

    public record ZoneOccupancy(String zone, int value) {
    }

    public record ActivityItem(String time, String type, String doc, String operator, String status, long qty) {
    }
}
