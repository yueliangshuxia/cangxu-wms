package com.cangxu.wms.masterdata.controller;

import com.cangxu.wms.common.Result;
import com.cangxu.wms.masterdata.dto.LocationRow;
import com.cangxu.wms.masterdata.dto.LocationStatusRequest;
import com.cangxu.wms.masterdata.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public Result<List<LocationRow>> list() {
        return Result.ok(locationService.list());
    }

    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        return Result.ok(locationService.summary());
    }

    @PatchMapping("/{code}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<LocationRow> updateStatus(@PathVariable String code,
                                            @Valid @RequestBody LocationStatusRequest request) {
        return Result.ok(locationService.updateStatus(code, request.status()));
    }
}
