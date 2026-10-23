package com.cangxu.wms.auth.dto;

import com.cangxu.wms.auth.entity.SysUser;

import java.time.LocalDateTime;

public record UserView(
        Long id,
        String username,
        String realName,
        String roleCode,
        String status,
        LocalDateTime createdAt
) {

    public static UserView from(SysUser user) {
        return new UserView(user.getId(), user.getUsername(), user.getRealName(),
                user.getRoleCode(), user.getStatus(), user.getCreatedAt());
    }
}
