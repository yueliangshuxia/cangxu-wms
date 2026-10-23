package com.cangxu.wms.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "供应商编码不能为空") String code,
        @NotBlank(message = "供应商名称不能为空") String name,
        String contact,
        String phone,
        String category,
        String status
) {
    public SupplierRequest {
        status = (status == null || status.isBlank()) ? "ACTIVE" : status;
    }
}
