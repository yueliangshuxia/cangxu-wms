package com.cangxu.wms.inbound.dto;

public record SlotCandidate(
        String code,
        double score,
        double turnover,
        double correlation,
        double distance,
        double fit,
        int capacity,
        String currentSku
) {
}
