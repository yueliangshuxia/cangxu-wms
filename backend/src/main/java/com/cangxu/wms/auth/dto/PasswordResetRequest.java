package com.cangxu.wms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需为 6–64 位")
        String password
) {
}
