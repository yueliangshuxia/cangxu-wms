package com.cangxu.wms.masterdata.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.masterdata.dto.CustomerRequest;
import com.cangxu.wms.masterdata.entity.Customer;
import com.cangxu.wms.masterdata.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Result<PageResult<Customer>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(customerService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Customer> get(@PathVariable Long id) {
        return Result.ok(customerService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Customer> create(@Valid @RequestBody CustomerRequest request) {
        return Result.ok(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return Result.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }
}
