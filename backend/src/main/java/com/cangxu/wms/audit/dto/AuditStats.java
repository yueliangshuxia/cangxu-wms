package com.cangxu.wms.audit.dto;

import java.util.List;

public record AuditStats(
        long total,
        long loginCount,
        long loginSuccess,
        long loginFail,
        long sensitiveCount,
        long failureCount,
        List<SensitiveItem> recentSensitive
) {

    public record SensitiveItem(String title, String meta, String time) {
    }
}
