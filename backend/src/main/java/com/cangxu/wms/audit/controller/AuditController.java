package com.cangxu.wms.audit.controller;

import com.cangxu.wms.audit.dto.AuditStats;
import com.cangxu.wms.audit.entity.AuditLog;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<AuditStats> stats() {
        return Result.ok(auditService.stats());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<PageResult<AuditLog>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String keyword) {
        return Result.ok(auditService.page(page, size, module, result, keyword));
    }
}
