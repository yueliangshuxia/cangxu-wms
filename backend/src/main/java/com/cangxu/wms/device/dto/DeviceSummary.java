package com.cangxu.wms.device.dto;

public record DeviceSummary(
        long total,
        long online,
        long offline,
        long working,
        long charging,
        double temperature,
        double humidity
) {
}
