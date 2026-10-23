package com.cangxu.wms;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.inbound.dto.SlotCandidate;
import com.cangxu.wms.inbound.dto.SlotRecommendation;
import com.cangxu.wms.inbound.service.InboundService;
import com.cangxu.wms.masterdata.dto.InventoryRow;
import com.cangxu.wms.masterdata.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InboundRecommendationTest {

    @Autowired
    private InboundService inboundService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void recommendsFreeLocationsWithValidWeights() {
        SlotRecommendation recommendation = inboundService.recommend(4L);

        assertNotNull(recommendation);
        assertFalse(recommendation.candidates().isEmpty());
        assertEquals(4, recommendation.weights().length);

        double sum = 0;
        for (double weight : recommendation.weights()) {
            sum += weight;
        }
        assertEquals(1.0, sum, 1e-6);
        assertTrue(recommendation.consistencyRatio() < 0.1);
        assertTrue(recommendation.candidates().get(0).score()
                >= recommendation.candidates().get(recommendation.candidates().size() - 1).score());

        List<SlotCandidate> candidates = recommendation.candidates();
        assertTrue(candidates.size() <= 5);
        assertTrue(candidates.stream().allMatch(c -> c.score() >= 0 && c.score() <= 1));
    }

    @Test
    void inventoryQueryReturnsSeedRows() {
        PageResult<InventoryRow> page = inventoryService.page(1, 20, null, null, null);
        assertTrue(page.total() >= 12);
        assertFalse(page.records().isEmpty());
        assertTrue(page.records().stream().anyMatch(row -> "WH-1001".equals(row.sku())));
    }
}
