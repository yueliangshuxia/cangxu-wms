package com.cangxu.wms.inbound.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.inbound.dto.CreateInboundRequest;
import com.cangxu.wms.inbound.dto.InboundOrderView;
import com.cangxu.wms.inbound.dto.PutawayRequest;
import com.cangxu.wms.inbound.dto.SlotRecommendation;
import com.cangxu.wms.inbound.service.InboundService;
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
@RequestMapping("/api/v1/inbound")
public class InboundController {

    private final InboundService inboundService;

    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @GetMapping("/orders")
    public Result<PageResult<InboundOrderView>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(inboundService.page(page, size, status, keyword));
    }

    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        return Result.ok(inboundService.summary());
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<InboundOrderView> create(@Valid @RequestBody CreateInboundRequest request) {
        return Result.ok(inboundService.create(request));
    }

    @PostMapping("/orders/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<InboundOrderView> receive(@PathVariable Long id) {
        return Result.ok(inboundService.receive(id));
    }

    @PostMapping("/orders/{id}/inspect")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','INSPECTOR')")
    public Result<InboundOrderView> inspect(@PathVariable Long id) {
        return Result.ok(inboundService.inspect(id));
    }

    @GetMapping("/orders/{id}/slot-recommendation")
    public Result<SlotRecommendation> recommendation(@PathVariable Long id) {
        return Result.ok(inboundService.recommend(id));
    }

    @PostMapping("/orders/{id}/putaway")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<InboundOrderView> putaway(@PathVariable Long id,
                                            @Valid @RequestBody PutawayRequest request) {
        return Result.ok(inboundService.putaway(id, request.locationCode()));
    }
}
