package com.cangxu.wms.stocktake.controller;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.common.Result;
import com.cangxu.wms.stocktake.dto.CreateStocktakeRequest;
import com.cangxu.wms.stocktake.dto.ScanStocktakeRequest;
import com.cangxu.wms.stocktake.entity.StocktakeItem;
import com.cangxu.wms.stocktake.entity.StocktakeTask;
import com.cangxu.wms.stocktake.service.StocktakeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stocktakes")
public class StocktakeController {

    private final StocktakeService stocktakeService;

    public StocktakeController(StocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    @GetMapping
    public Result<PageResult<StocktakeTask>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(stocktakeService.page(page, size, status, keyword));
    }

    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        return Result.ok(stocktakeService.summary());
    }

    @GetMapping("/diffs")
    public Result<List<StocktakeItem>> diffs() {
        return Result.ok(stocktakeService.diffs());
    }

    @GetMapping("/{id}/items")
    public Result<List<StocktakeItem>> items(@PathVariable Long id) {
        return Result.ok(stocktakeService.items(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<StocktakeTask> create(@Valid @RequestBody CreateStocktakeRequest request) {
        return Result.ok(stocktakeService.create(request));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','INSPECTOR')")
    public Result<StocktakeTask> start(@PathVariable Long id) {
        return Result.ok(stocktakeService.start(id));
    }

    @PostMapping("/{id}/scan")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','INSPECTOR')")
    public Result<StocktakeTask> scan(@PathVariable Long id,
                                      @RequestBody(required = false) ScanStocktakeRequest request) {
        return Result.ok(stocktakeService.scan(id, request == null ? null : request.code()));
    }
}
