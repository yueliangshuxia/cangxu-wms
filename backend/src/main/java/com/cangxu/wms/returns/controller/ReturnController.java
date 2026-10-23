package com.cangxu.wms.returns.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.returns.dto.CreateReturnRequest;
import com.cangxu.wms.returns.dto.InspectReturnRequest;
import com.cangxu.wms.returns.dto.ReturnStats;
import com.cangxu.wms.returns.entity.ReturnOrder;
import com.cangxu.wms.returns.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @GetMapping
    public Result<PageResult<ReturnOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(returnService.page(page, size, status, keyword));
    }

    @GetMapping("/summary")
    public Result<ReturnStats> summary() {
        return Result.ok(returnService.summary());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEIVER')")
    public Result<ReturnOrder> create(@Valid @RequestBody CreateReturnRequest request) {
        return Result.ok(returnService.create(request));
    }

    @PostMapping("/{id}/inspect")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','INSPECTOR')")
    public Result<ReturnOrder> inspect(@PathVariable Long id,
                                       @Valid @RequestBody InspectReturnRequest request) {
        return Result.ok(returnService.inspect(id, request));
    }
}
