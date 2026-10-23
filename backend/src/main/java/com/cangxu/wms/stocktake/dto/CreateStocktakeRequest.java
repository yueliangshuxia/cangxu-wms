package com.cangxu.wms.stocktake.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStocktakeRequest(
        @NotBlank(message = "盘点范围不能为空") String scope,
        String type,
        String owner,
        String plannedAt
) {
}
