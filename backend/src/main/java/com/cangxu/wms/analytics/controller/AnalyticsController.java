package com.cangxu.wms.analytics.controller;

import com.cangxu.wms.analytics.dto.AnalyticsOverview;
import com.cangxu.wms.analytics.service.AnalyticsService;
import com.cangxu.wms.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public Result<AnalyticsOverview> overview(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(analyticsService.overview(days));
    }
}
