package com.cangxu.wms.device.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DeviceUpdateRequest(
        String location,
        String status,
        @Min(value = 0, message = "电量不能小于 0")
        @Max(value = 100, message = "电量不能大于 100")
        Integer battery,
        String signalStrength
) {
}
