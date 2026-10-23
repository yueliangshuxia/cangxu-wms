package com.cangxu.wms.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

public record LocationStatusRequest(@NotBlank(message = "状态不能为空") String status) {
}
