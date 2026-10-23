package com.cangxu.wms.masterdata.dto;

import java.time.LocalDateTime;

public record InventoryRow(
        Long id,
        String sku,
        String name,
        String category,
        String locationCode,
        Integer qty,
        Integer lockedQty,
        Integer available,
        Integer safetyStock,
        String status,
        String imageUrl,
        LocalDateTime updatedAt
) {
}
