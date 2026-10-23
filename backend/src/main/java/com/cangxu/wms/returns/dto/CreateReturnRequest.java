package com.cangxu.wms.returns.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReturnRequest(
        @NotBlank(message = "客户不能为空") String customerName,
        @NotBlank(message = "商品不能为空") String productName,
        String sku,
        @NotNull(message = "退货数量不能为空")
        @Min(value = 1, message = "退货数量必须大于 0") Integer qty,
        String reason,
        String appliedAt
) {
}
