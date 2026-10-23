package com.cangxu.wms.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
                "status", "UP",
                "service", "cangxu-wms",
                "time", LocalDateTime.now().toString()
        ));
    }
}
