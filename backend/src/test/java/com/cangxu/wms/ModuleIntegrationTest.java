package com.cangxu.wms;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.returns.dto.InspectReturnRequest;
import com.cangxu.wms.returns.entity.ReturnOrder;
import com.cangxu.wms.returns.service.ReturnService;
import com.cangxu.wms.settings.service.SettingsService;
import com.cangxu.wms.stocktake.entity.StocktakeTask;
import com.cangxu.wms.stocktake.service.StocktakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ModuleIntegrationTest {

    @Autowired
    private StocktakeService stocktakeService;

    @Autowired
    private ReturnService returnService;

    @Autowired
    private SettingsService settingsService;

    @Test
    void stocktakeScanIncrementsCount() {
        PageResult<StocktakeTask> page = stocktakeService.page(1, 20, null, null);
        assertTrue(page.total() >= 8);
        StocktakeTask task = page.records().stream()
                .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
                .findFirst()
                .orElseThrow();
        int before = task.getCounted();
        StocktakeTask updated = stocktakeService.scan(task.getId(), "TEST-LOC");
        assertEquals(before + 1, updated.getCounted());
        assertFalse(stocktakeService.diffs().isEmpty());
    }

    @Test
    void returnInspectionChangesStatus() {
        PageResult<ReturnOrder> page = returnService.page(1, 20, "PENDING_INSPECTION", null);
        assertFalse(page.records().isEmpty());
        ReturnOrder order = page.records().get(0);
        ReturnOrder updated = returnService.inspect(order.getId(),
                new InspectReturnRequest("resell", "test"));
        assertEquals("READY_TO_PUTAWAY", updated.getStatus());
        assertEquals("resell", updated.getDisposition());
    }

    @Test
    void settingsCanBeReadAndSaved() {
        Map<String, String> before = settingsService.getAll();
        assertNotNull(before.get("warehouse.name"));
        Map<String, String> after = settingsService.save(Map.of("warehouse.name", "测试仓库"));
        assertEquals("测试仓库", after.get("warehouse.name"));
    }
}
