package com.cangxu.wms.masterdata.dto;

public record LocationRow(
        Long id,
        String code,
        String zoneCode,
        String zoneName,
        String tempType,
        Integer rowNo,
        Integer colNo,
        Integer capacity,
        String status,
        String currentSku
) {
}
