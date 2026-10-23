package com.cangxu.wms.auth.controller;

import com.cangxu.wms.auth.dto.PasswordResetRequest;
import com.cangxu.wms.auth.dto.UserRequest;
import com.cangxu.wms.auth.dto.UserView;
import com.cangxu.wms.auth.service.UserService;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<PageResult<UserView>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return Result.ok(userService.page(page, size, keyword, role, status));
    }

    @GetMapping("/roles")
    public Result<List<Map<String, String>>> roles() {
        return Result.ok(userService.roles());
    }

    @PostMapping
    public Result<UserView> create(@Valid @RequestBody UserRequest request) {
        return Result.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserView> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return Result.ok(userService.update(id, request));
    }

    @PostMapping("/{id}/reset-password")
    public Result<UserView> resetPassword(@PathVariable Long id,
                                          @Valid @RequestBody PasswordResetRequest request) {
        return Result.ok(userService.resetPassword(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
