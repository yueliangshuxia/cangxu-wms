package com.cangxu.wms.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the Vue SPA when the frontend build is copied into the jar by the
 * {@code portable} Maven profile. API and H2 routes are never matched here.
 */
@Controller
public class SpaController {

    @GetMapping({
            "/", "/login", "/dashboard", "/masterdata", "/inventory", "/locations",
            "/inbound", "/outbound", "/devices", "/audit", "/stocktake", "/returns",
            "/analytics", "/settings"
    })
    public String index() {
        return "forward:/index.html";
    }
}
