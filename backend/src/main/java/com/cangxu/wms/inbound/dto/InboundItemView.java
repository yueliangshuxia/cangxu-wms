package com.cangxu.wms.inbound.dto;

public record InboundItemView(
        Long id,
        String sku,
        String productName,
        Integer qty,
        String locationCode
) {
}
