package com.cangxu.wms.device.controller;

import com.cangxu.wms.common.Result;
import com.cangxu.wms.device.dto.DeviceSummary;
import com.cangxu.wms.device.dto.DeviceUpdateRequest;
import com.cangxu.wms.device.dto.TelemetryPoint;
import com.cangxu.wms.device.entity.Device;
import com.cangxu.wms.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public Result<List<Device>> list() {
        return Result.ok(deviceService.list());
    }

    @GetMapping("/summary")
    public Result<DeviceSummary> summary() {
        return Result.ok(deviceService.summary());
    }

    @GetMapping("/{code}/telemetry")
    public Result<List<TelemetryPoint>> telemetry(
            @PathVariable String code,
            @RequestParam(defaultValue = "temp") String metric,
            @RequestParam(defaultValue = "24") int hours) {
        return Result.ok(deviceService.telemetry(code, metric, hours));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Result<Device> update(@PathVariable String code,
                                 @Valid @RequestBody DeviceUpdateRequest request) {
        return Result.ok(deviceService.update(code, request));
    }
}
