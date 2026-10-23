package com.cangxu.wms.inbound.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InboundOrderView(
        Long id,
        String no,
        String supplierName,
        String dock,
        LocalDateTime eta,
        String status,
        String operator,
        LocalDateTime createdAt,
        List<InboundItemView> items,
        String locationCode
) {
}
