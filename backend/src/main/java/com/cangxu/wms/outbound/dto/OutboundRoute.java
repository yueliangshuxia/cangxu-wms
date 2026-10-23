package com.cangxu.wms.outbound.dto;

import java.util.List;

public record OutboundRoute(
        String waveNo,
        long orders,
        long items,
        long totalQty,
        List<RouteStop> stops
) {

    public record RouteStop(
            int seq,
            String code,
            String productName,
            int qty,
            String zone,
            int row,
            int col
    ) {
    }
}
