package com.cangxu.wms.returns.dto;

import jakarta.validation.constraints.NotBlank;

public record InspectReturnRequest(
        @NotBlank(message = "处置结论不能为空") String disposition,
        String note
) {
}
