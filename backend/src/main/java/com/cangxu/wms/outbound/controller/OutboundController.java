package com.cangxu.wms.outbound.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.outbound.dto.CreateOutboundRequest;
import com.cangxu.wms.outbound.dto.OutboundRoute;
import com.cangxu.wms.outbound.entity.OutboundOrder;
import com.cangxu.wms.outbound.service.OutboundService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/outbound")
public class OutboundController {

    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @GetMapping("/orders")
    public Result<PageResult<OutboundOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(outboundService.page(page, size, status, keyword));
    }

    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        return Result.ok(outboundService.summary());
    }

    @GetMapping("/route")
    public Result<OutboundRoute> route(@RequestParam(required = false) String waveNo) {
        return Result.ok(outboundService.route(waveNo));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<OutboundOrder> create(@Valid @RequestBody CreateOutboundRequest request) {
        return Result.ok(outboundService.create(request));
    }

    @PostMapping("/waves")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<Map<String, Object>> generateWave() {
        return Result.ok(outboundService.generateWave());
    }

    @PostMapping("/orders/{id}/pick")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<OutboundOrder> pick(@PathVariable Long id) {
        return Result.ok(outboundService.pick(id));
    }

    @PostMapping("/orders/{id}/ship")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<OutboundOrder> ship(@PathVariable Long id) {
        return Result.ok(outboundService.ship(id));
    }
}
