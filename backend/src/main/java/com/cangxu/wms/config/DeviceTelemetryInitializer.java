package com.cangxu.wms.config;

import com.cangxu.wms.device.entity.DeviceTelemetry;
import com.cangxu.wms.device.mapper.DeviceTelemetryMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Order(20)
public class DeviceTelemetryInitializer implements CommandLineRunner {

    private final DeviceTelemetryMapper telemetryMapper;

    public DeviceTelemetryInitializer(DeviceTelemetryMapper telemetryMapper) {
        this.telemetryMapper = telemetryMapper;
    }

    @Override
    public void run(String... args) {
        if (telemetryMapper.selectCount(null) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime time = now.minusHours(i);
            double phase = (23 - i) / 24.0 * Math.PI * 2;
            double temp = 22.1 + 2.2 * Math.sin(phase - 1.0) + (i % 3) * 0.1;
            double humidity = 48 + 4.5 * Math.sin(phase + 0.8) + (i % 2) * 0.3;
            insert("TH-A01", "temp", temp, "°C", time);
            insert("TH-A01", "humidity", humidity, "%", time);
        }
        insert("TH-B01", "temp", 22.4, "°C", now);
        insert("TH-B01", "humidity", 51, "%", now);
        insert("TH-D01", "temp", 21.8, "°C", now);
        insert("TH-D01", "humidity", 46, "%", now);
    }

    private void insert(String code, String metric, double value, String unit, LocalDateTime time) {
        telemetryMapper.insert(DeviceTelemetry.builder()
                .deviceCode(code)
                .metric(metric)
                .metricValue(BigDecimal.valueOf(Math.round(value * 100.0) / 100.0))
                .unit(unit)
                .collectedAt(time)
                .build());
    }
}
