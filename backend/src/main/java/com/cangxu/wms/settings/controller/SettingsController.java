package com.cangxu.wms.settings.controller;

import com.cangxu.wms.common.Result;
import com.cangxu.wms.settings.service.SettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public Result<Map<String, String>> getAll() {
        return Result.ok(settingsService.getAll());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Map<String, String>> save(@RequestBody Map<String, String> values) {
        return Result.ok(settingsService.save(values));
    }
}
