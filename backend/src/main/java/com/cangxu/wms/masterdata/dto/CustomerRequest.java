package com.cangxu.wms.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank(message = "客户编码不能为空") String code,
        @NotBlank(message = "客户名称不能为空") String name,
        String channel,
        String contact,
        String phone,
        String status
) {
    public CustomerRequest {
        status = (status == null || status.isBlank()) ? "ACTIVE" : status;
    }
}
