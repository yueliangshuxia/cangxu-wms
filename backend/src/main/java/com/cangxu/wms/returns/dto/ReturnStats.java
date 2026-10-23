package com.cangxu.wms.returns.dto;

import java.util.List;

public record ReturnStats(
        long total,
        long pending,
        long ready,
        long inStock,
        long scrapped,
        double returnRate,
        double resellRate,
        double repairRate,
        double scrapRate,
        List<ReasonCount> reasons
) {

    public record ReasonCount(String label, long value) {
    }
}
