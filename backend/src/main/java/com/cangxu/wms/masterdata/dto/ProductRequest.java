package com.cangxu.wms.masterdata.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "SKU 不能为空") @Size(max = 64) String sku,
        @NotBlank(message = "商品名称不能为空") @Size(max = 128) String name,
        @NotBlank(message = "分类不能为空") String category,
        @Size(max = 128) String spec,
        String unit,
        String tempType,
        @Min(value = 0, message = "安全库存不能为负数") Integer safetyStock,
        String status,
        String imageUrl
) {
    public ProductRequest {
        unit = (unit == null || unit.isBlank()) ? "件" : unit;
        tempType = (tempType == null || tempType.isBlank()) ? "NORMAL" : tempType;
        status = (status == null || status.isBlank()) ? "ON_SALE" : status;
        safetyStock = safetyStock == null ? 0 : safetyStock;
    }
}
