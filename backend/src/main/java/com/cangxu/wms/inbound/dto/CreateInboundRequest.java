package com.cangxu.wms.inbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CreateInboundRequest(
        @NotBlank(message = "供应商不能为空") String supplierName,
        @NotBlank(message = "SKU 不能为空") String sku,
        @NotBlank(message = "商品名称不能为空") String productName,
        @Min(value = 1, message = "数量必须大于 0") Integer qty,
        String dock,
        LocalDateTime eta
) {
}
