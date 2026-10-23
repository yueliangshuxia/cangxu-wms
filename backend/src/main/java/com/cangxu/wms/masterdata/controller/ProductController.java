package com.cangxu.wms.masterdata.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.masterdata.dto.ProductRequest;
import com.cangxu.wms.masterdata.entity.Product;
import com.cangxu.wms.masterdata.service.ProductService;
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
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<PageResult<Product>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return Result.ok(productService.page(page, size, keyword, category, status));
    }

    @GetMapping("/{id}")
    public Result<Product> get(@PathVariable Long id) {
        return Result.ok(productService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Product> create(@Valid @RequestBody ProductRequest request) {
        return Result.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return Result.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
