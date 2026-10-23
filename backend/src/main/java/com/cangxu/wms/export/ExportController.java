package com.cangxu.wms.export;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<byte[]> inventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return csv("inventory", exportService.inventory(keyword, category, status));
    }

    @GetMapping("/locations")
    public ResponseEntity<byte[]> locations() {
        return csv("locations", exportService.locations());
    }

    @GetMapping("/inbound")
    public ResponseEntity<byte[]> inbound(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return csv("inbound", exportService.inbound(status, keyword));
    }

    @GetMapping("/outbound")
    public ResponseEntity<byte[]> outbound(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return csv("outbound", exportService.outbound(status, keyword));
    }

    @GetMapping("/stocktakes")
    public ResponseEntity<byte[]> stocktakes(@RequestParam(required = false) String status) {
        return csv("stocktakes", exportService.stocktakes(status));
    }

    @GetMapping("/returns")
    public ResponseEntity<byte[]> returns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return csv("returns", exportService.returns(status, keyword));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> auditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String keyword) {
        return csv("audit-logs", exportService.auditLogs(module, result, keyword));
    }

    @GetMapping("/masterdata")
    public ResponseEntity<byte[]> masterdata() {
        return csv("masterdata", exportService.masterdata());
    }

    @GetMapping("/analytics")
    public ResponseEntity<byte[]> analytics(@RequestParam(defaultValue = "30") int days) {
        return csv("analytics", exportService.analytics(days));
    }

    private ResponseEntity<byte[]> csv(String name, byte[] body) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "-" + date + ".csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(body);
    }
}
