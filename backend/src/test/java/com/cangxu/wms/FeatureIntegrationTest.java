package com.cangxu.wms;

import com.cangxu.wms.analytics.dto.AnalyticsOverview;
import com.cangxu.wms.analytics.service.AnalyticsService;
import com.cangxu.wms.auth.dto.UserRequest;
import com.cangxu.wms.auth.dto.UserView;
import com.cangxu.wms.auth.service.UserService;
import com.cangxu.wms.dashboard.dto.DashboardOverview;
import com.cangxu.wms.dashboard.service.DashboardService;
import com.cangxu.wms.export.ExportService;
import com.cangxu.wms.outbound.dto.OutboundRoute;
import com.cangxu.wms.outbound.service.OutboundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class FeatureIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private OutboundService outboundService;

    @Autowired
    private UserService userService;

    @Test
    void dashboardAndAnalyticsUseDatabaseAggregates() {
        DashboardOverview dashboard = dashboardService.overview();
        assertEquals(7, dashboard.trend().size());
        assertEquals(5, dashboard.zones().size());
        assertTrue(dashboard.inventoryQty() > 0);

        AnalyticsOverview analytics = analyticsService.overview(30);
        assertEquals(30, analytics.trend().size());
        assertFalse(analytics.categories().isEmpty());
        assertTrue(analytics.totalQty() > 0);
        assertEquals(3, analytics.experiment().strategies().size());
        assertTrue(analytics.experiment().orders() > 0);
    }

    @Test
    void outboundRouteIsBuiltFromOrderItems() {
        OutboundRoute route = outboundService.route("W-07");
        assertEquals("W-07", route.waveNo());
        assertFalse(route.stops().isEmpty());
        assertTrue(route.totalQty() > 0);
        assertEquals(1, route.stops().get(0).seq());
    }

    @Test
    void csvExportsContainBomAndHeaders() {
        String inventory = new String(exportService.inventory(null, null, null), StandardCharsets.UTF_8);
        assertTrue(inventory.startsWith("﻿"));
        assertTrue(inventory.contains("SKU,商品,分类,库位"));

        String outbound = new String(exportService.outbound(null, null), StandardCharsets.UTF_8);
        assertTrue(outbound.contains("出库单号,客户"));

        String analytics = new String(exportService.analytics(30), StandardCharsets.UTF_8);
        assertTrue(analytics.contains("AHP-TOPSIS"));
    }

    @Test
    @WithMockUser(username = "chenyu", roles = "MANAGER")
    void userCrudWorksForManager() {
        UserView created = userService.create(new UserRequest(
                "test-user", "secret123", "测试用户", "RECEIVER", "ACTIVE"));
        assertNotNull(created.id());
        assertEquals("test-user", created.username());

        UserView updated = userService.update(created.id(), new UserRequest(
                "test-user", null, "测试用户改名", "INSPECTOR", "DISABLED"));
        assertEquals("INSPECTOR", updated.roleCode());
        assertEquals("DISABLED", updated.status());

        userService.delete(created.id());
        assertTrue(userService.page(1, 10, "test-user", null, null).records().isEmpty());
    }
}
