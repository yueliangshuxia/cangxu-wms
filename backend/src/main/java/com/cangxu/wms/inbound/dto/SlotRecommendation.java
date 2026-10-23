package com.cangxu.wms.inbound.dto;

import java.util.List;

public record SlotRecommendation(
        Long orderId,
        String sku,
        String productName,
        int qty,
        double[] weights,
        double consistencyRatio,
        List<SlotCandidate> candidates
) {
}
