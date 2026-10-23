package com.cangxu.wms.masterdata.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.masterdata.dto.InventoryRow;
import com.cangxu.wms.masterdata.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public Result<PageResult<InventoryRow>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return Result.ok(inventoryService.page(page, size, keyword, category, status));
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(inventoryService.summary());
    }
}
