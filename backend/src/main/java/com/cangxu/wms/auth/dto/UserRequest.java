package com.cangxu.wms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "账号不能为空") String username,
        @Size(min = 6, max = 64, message = "密码长度需为 6–64 位") String password,
        @NotBlank(message = "姓名不能为空") String realName,
        @NotBlank(message = "角色不能为空") String roleCode,
        String status
) {
}
