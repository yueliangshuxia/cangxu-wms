package com.cangxu.wms.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UserInfo user
) {
    public record UserInfo(Long id, String username, String realName, String roleCode) {
    }
}
