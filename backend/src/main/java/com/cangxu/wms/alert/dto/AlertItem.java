package com.cangxu.wms.alert.dto;

public record AlertItem(
        String id,
        String level,
        String icon,
        String title,
        String meta,
        String route
) {
}
