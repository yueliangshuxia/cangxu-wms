package com.cangxu.wms.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.device.dto.DeviceSummary;
import com.cangxu.wms.device.dto.DeviceUpdateRequest;
import com.cangxu.wms.device.dto.TelemetryPoint;
import com.cangxu.wms.device.entity.Device;
import com.cangxu.wms.device.entity.DeviceTelemetry;
import com.cangxu.wms.device.mapper.DeviceMapper;
import com.cangxu.wms.device.mapper.DeviceTelemetryMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class DeviceService {

    private static final Set<String> STATUSES = Set.of("ONLINE", "OFFLINE", "WORKING", "CHARGING");

    private final DeviceMapper deviceMapper;
    private final DeviceTelemetryMapper telemetryMapper;
    private final AuditService auditService;

    public DeviceService(DeviceMapper deviceMapper,
                         DeviceTelemetryMapper telemetryMapper,
                         AuditService auditService) {
        this.deviceMapper = deviceMapper;
        this.telemetryMapper = telemetryMapper;
        this.auditService = auditService;
    }

    public List<Device> list() {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>().orderByAsc(Device::getCode));
    }

    public DeviceSummary summary() {
        List<Device> devices = deviceMapper.selectList(null);
        long total = devices.size();
        long online = devices.stream().filter(d -> "ONLINE".equals(d.getStatus())).count();
        long offline = devices.stream().filter(d -> "OFFLINE".equals(d.getStatus())).count();
        long working = devices.stream().filter(d -> "WORKING".equals(d.getStatus())).count();
        long charging = devices.stream().filter(d -> "CHARGING".equals(d.getStatus())).count();
        double temp = latestValue("TH-A01", "temp");
        double humidity = latestValue("TH-A01", "humidity");
        return new DeviceSummary(total, online, offline, working, charging, temp, humidity);
    }

    public List<TelemetryPoint> telemetry(String deviceCode, String metric, int hours) {
        String normalized = "humidity".equalsIgnoreCase(metric) ? "humidity" : "temp";
        LocalDateTime since = LocalDateTime.now().minusHours(Math.max(1, Math.min(168, hours)));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return telemetryMapper.selectList(new LambdaQueryWrapper<DeviceTelemetry>()
                        .eq(DeviceTelemetry::getDeviceCode, deviceCode)
                        .eq(DeviceTelemetry::getMetric, normalized)
                        .ge(DeviceTelemetry::getCollectedAt, since)
                        .orderByAsc(DeviceTelemetry::getCollectedAt))
                .stream()
                .map(row -> new TelemetryPoint(row.getCollectedAt().format(formatter), row.getMetricValue().doubleValue()))
                .toList();
    }

    public Device update(String code, DeviceUpdateRequest request) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getCode, code));
        if (device == null) {
            throw new BusinessException(404, "设备不存在：" + code);
        }
        if (StringUtils.hasText(request.location())) {
            device.setLocation(request.location().trim());
        }
        if (StringUtils.hasText(request.status())) {
            String status = request.status().trim().toUpperCase();
            if (!STATUSES.contains(status)) {
                throw new BusinessException("不支持的设备状态：" + request.status());
            }
            device.setStatus(status);
        }
        if (request.battery() != null) {
            device.setBattery(request.battery());
        }
        if (StringUtils.hasText(request.signalStrength())) {
            device.setSignalStrength(request.signalStrength().trim());
        }
        deviceMapper.updateById(device);
        auditService.logCurrent("DEVICE", "更新设备配置", "device:" + code, "SUCCESS",
                device.getStatus() + " / " + device.getLocation());
        return device;
    }

    public void ingest(String deviceCode, String metric, double value, String unit) {
        telemetryMapper.insert(DeviceTelemetry.builder()
                .deviceCode(deviceCode)
                .metric(metric)
                .metricValue(BigDecimal.valueOf(Math.round(value * 100.0) / 100.0))
                .unit(unit)
                .collectedAt(LocalDateTime.now())
                .build());
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getCode, deviceCode));
        if (device != null) {
            device.setLastHeartbeat(LocalDateTime.now());
            if ("OFFLINE".equals(device.getStatus())) {
                device.setStatus("ONLINE");
            }
            if ("battery".equals(metric)) {
                device.setBattery((int) Math.round(value));
            }
            deviceMapper.updateById(device);
        }
    }

    public void updateAgvState(String deviceCode, String state, int battery) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getCode, deviceCode));
        if (device != null) {
            device.setStatus(state);
            device.setBattery(battery);
            device.setLastHeartbeat(LocalDateTime.now());
            deviceMapper.updateById(device);
        }
    }

    private double latestValue(String deviceCode, String metric) {
        List<DeviceTelemetry> latest = telemetryMapper.selectList(new LambdaQueryWrapper<DeviceTelemetry>()
                .eq(DeviceTelemetry::getDeviceCode, deviceCode)
                .eq(DeviceTelemetry::getMetric, metric)
                .orderByDesc(DeviceTelemetry::getCollectedAt)
                .last("LIMIT 1"));
        return latest.isEmpty() ? 0 : latest.get(0).getMetricValue().doubleValue();
    }
}
