package com.cangxu.wms.alert.controller;

import com.cangxu.wms.alert.dto.AlertItem;
import com.cangxu.wms.alert.service.AlertService;
import com.cangxu.wms.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public Result<List<AlertItem>> list() {
        return Result.ok(alertService.list());
    }
}
