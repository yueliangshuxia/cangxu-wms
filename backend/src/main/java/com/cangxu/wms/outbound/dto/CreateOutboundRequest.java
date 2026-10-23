package com.cangxu.wms.outbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOutboundRequest(
        @NotBlank(message = "客户不能为空") String customerName,
        String priority,
        @Min(value = 1, message = "商品数必须大于 0") Integer itemCount,
        @Min(value = 1, message = "数量必须大于 0") Integer totalQty,
        String promisedAt
) {
}
