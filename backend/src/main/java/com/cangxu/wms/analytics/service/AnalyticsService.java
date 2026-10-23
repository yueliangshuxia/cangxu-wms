package com.cangxu.wms.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.analytics.dto.AnalyticsOverview;
import com.cangxu.wms.analytics.dto.AnalyticsOverview.CategoryShare;
import com.cangxu.wms.analytics.dto.AnalyticsOverview.HeatCell;
import com.cangxu.wms.analytics.dto.AnalyticsOverview.KpiSet;
import com.cangxu.wms.analytics.dto.AnalyticsOverview.TrendPoint;
import com.cangxu.wms.dashboard.service.DashboardService;
import com.cangxu.wms.masterdata.entity.Inventory;
import com.cangxu.wms.masterdata.entity.Location;
import com.cangxu.wms.masterdata.entity.Product;
import com.cangxu.wms.masterdata.entity.Zone;
import com.cangxu.wms.masterdata.mapper.InventoryMapper;
import com.cangxu.wms.masterdata.mapper.LocationMapper;
import com.cangxu.wms.masterdata.mapper.ProductMapper;
import com.cangxu.wms.masterdata.mapper.ZoneMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final DashboardService dashboardService;
    private final ExperimentService experimentService;
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final LocationMapper locationMapper;
    private final ZoneMapper zoneMapper;

    public AnalyticsService(DashboardService dashboardService,
                            ExperimentService experimentService,
                            InventoryMapper inventoryMapper,
                            ProductMapper productMapper,
                            LocationMapper locationMapper,
                            ZoneMapper zoneMapper) {
        this.dashboardService = dashboardService;
        this.experimentService = experimentService;
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.locationMapper = locationMapper;
        this.zoneMapper = zoneMapper;
    }

    public AnalyticsOverview overview(int days) {
        int span = Math.max(7, Math.min(90, days));
        List<TrendPoint> trend = dashboardService.buildTrend(span).stream()
                .map(point -> new TrendPoint(point.date(), point.inbound(), point.outbound()))
                .toList();

        List<Inventory> inventory = inventoryMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Product> products = productMapper.selectList(null).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<String, Long> qtyByCategory = new LinkedHashMap<>();
        long totalQty = 0;
        for (Inventory row : inventory) {
            Product product = products.get(row.getProductId());
            if (product == null) {
                continue;
            }
            long qty = row.getQty() == null ? 0 : row.getQty();
            totalQty += qty;
            qtyByCategory.merge(Objects.requireNonNullElse(product.getCategory(), "其他"), qty, Long::sum);
        }
        final long inventoryTotal = totalQty;
        List<CategoryShare> categories = qtyByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new CategoryShare(
                        entry.getKey(),
                        inventoryTotal == 0 ? 0
                                : Math.round(entry.getValue() * 1000.0 / inventoryTotal) / 10.0,
                        entry.getValue()))
                .toList();

        List<Zone> zones = zoneMapper.selectList(null).stream()
                .sorted(Comparator.comparing(Zone::getCode))
                .toList();
        List<Location> locations = locationMapper.selectList(null);
        Map<Long, List<Location>> byZone = locations.stream()
                .filter(l -> l.getZoneId() != null)
                .collect(Collectors.groupingBy(Location::getZoneId));
        TreeSet<Integer> rows = locations.stream()
                .map(Location::getRowNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        List<String> aisles = rows.stream().map(r -> String.format("%02d", r)).toList();
        List<HeatCell> heatmap = new ArrayList<>();
        for (Zone zone : zones) {
            Map<Integer, List<Location>> byRow = byZone.getOrDefault(zone.getId(), List.of()).stream()
                    .filter(l -> l.getRowNo() != null)
                    .collect(Collectors.groupingBy(Location::getRowNo));
            for (Integer row : rows) {
                List<Location> cells = byRow.getOrDefault(row, List.of());
                long occupied = cells.stream().filter(l -> !"FREE".equals(l.getStatus())).count();
                int percent = cells.isEmpty() ? 0 : (int) Math.round(occupied * 100.0 / cells.size());
                heatmap.add(new HeatCell(zone.getCode(), String.format("%02d", row), percent));
            }
        }

        long inboundQty = trend.stream().mapToLong(TrendPoint::inbound).sum();
        long outboundQty = trend.stream().mapToLong(TrendPoint::outbound).sum();
        double turnover = totalQty == 0 ? 0
                : Math.round(outboundQty * 30.0 / span / totalQty * 100.0) / 100.0;
        long occupied = locations.stream().filter(l -> !"FREE".equals(l.getStatus())).count();
        double utilization = locations.isEmpty() ? 0
                : Math.round(occupied * 1000.0 / locations.size()) / 10.0;

        return new AnalyticsOverview(
                span,
                trend,
                categories,
                totalQty,
                zones.stream().map(Zone::getCode).toList(),
                aisles,
                heatmap,
                new KpiSet(inboundQty, outboundQty, turnover, utilization),
                experimentService.experiment());
    }
}
