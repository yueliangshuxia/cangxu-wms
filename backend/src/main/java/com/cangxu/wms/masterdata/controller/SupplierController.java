package com.cangxu.wms.masterdata.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.masterdata.dto.SupplierRequest;
import com.cangxu.wms.masterdata.entity.Supplier;
import com.cangxu.wms.masterdata.service.SupplierService;
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

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public Result<PageResult<Supplier>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(supplierService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Supplier> get(@PathVariable Long id) {
        return Result.ok(supplierService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Supplier> create(@Valid @RequestBody SupplierRequest request) {
        return Result.ok(supplierService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Supplier> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return Result.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.ok();
    }
}
