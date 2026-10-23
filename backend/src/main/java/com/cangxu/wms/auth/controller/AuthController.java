package com.cangxu.wms.auth.controller;

import com.cangxu.wms.auth.dto.LoginRequest;
import com.cangxu.wms.auth.dto.LoginResponse;
import com.cangxu.wms.auth.service.AuthService;
import com.cangxu.wms.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/me")
    public Result<LoginResponse.UserInfo> me(Authentication authentication) {
        return Result.ok(authService.me(authentication.getName()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(Authentication authentication, HttpServletRequest httpRequest) {
        authService.logout(authentication.getName(), httpRequest.getRemoteAddr());
        return Result.ok();
    }
}
