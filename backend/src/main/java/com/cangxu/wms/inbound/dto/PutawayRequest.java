package com.cangxu.wms.inbound.dto;

import jakarta.validation.constraints.NotBlank;

public record PutawayRequest(@NotBlank(message = "库位不能为空") String locationCode) {
}
