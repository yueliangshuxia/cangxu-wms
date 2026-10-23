package com.cangxu.wms.dashboard.controller;

import com.cangxu.wms.common.Result;
import com.cangxu.wms.dashboard.dto.DashboardOverview;
import com.cangxu.wms.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public Result<DashboardOverview> overview() {
        return Result.ok(dashboardService.overview());
    }
}
