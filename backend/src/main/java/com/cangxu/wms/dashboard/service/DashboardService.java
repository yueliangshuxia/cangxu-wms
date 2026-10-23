package com.cangxu.wms.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.auth.mapper.SysUserMapper;
import com.cangxu.wms.dashboard.dto.DashboardOverview;
import com.cangxu.wms.inbound.entity.InboundItem;
import com.cangxu.wms.inbound.entity.InboundOrder;
import com.cangxu.wms.inbound.mapper.InboundItemMapper;
import com.cangxu.wms.inbound.mapper.InboundOrderMapper;
import com.cangxu.wms.masterdata.entity.Inventory;
import com.cangxu.wms.masterdata.entity.Location;
import com.cangxu.wms.masterdata.entity.Zone;
import com.cangxu.wms.masterdata.mapper.CustomerMapper;
import com.cangxu.wms.masterdata.mapper.InventoryMapper;
import com.cangxu.wms.masterdata.mapper.LocationMapper;
import com.cangxu.wms.masterdata.mapper.ProductMapper;
import com.cangxu.wms.masterdata.mapper.SupplierMapper;
import com.cangxu.wms.masterdata.mapper.ZoneMapper;
import com.cangxu.wms.outbound.entity.OutboundOrder;
import com.cangxu.wms.outbound.mapper.OutboundOrderMapper;
import com.cangxu.wms.stocktake.entity.StocktakeTask;
import com.cangxu.wms.stocktake.mapper.StocktakeTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProductMapper productMapper;
    private final SupplierMapper supplierMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper userMapper;
    private final InventoryMapper inventoryMapper;
    private final LocationMapper locationMapper;
    private final ZoneMapper zoneMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundItemMapper inboundItemMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final StocktakeTaskMapper stocktakeTaskMapper;
    private final AuditService auditService;

    public DashboardService(ProductMapper productMapper,
                            SupplierMapper supplierMapper,
                            CustomerMapper customerMapper,
                            SysUserMapper userMapper,
                            InventoryMapper inventoryMapper,
                            LocationMapper locationMapper,
                            ZoneMapper zoneMapper,
                            InboundOrderMapper inboundOrderMapper,
                            InboundItemMapper inboundItemMapper,
                            OutboundOrderMapper outboundOrderMapper,
                            StocktakeTaskMapper stocktakeTaskMapper,
                            AuditService auditService) {
        this.productMapper = productMapper;
        this.supplierMapper = supplierMapper;
        this.customerMapper = customerMapper;
        this.userMapper = userMapper;
        this.inventoryMapper = inventoryMapper;
        this.locationMapper = locationMapper;
        this.zoneMapper = zoneMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundItemMapper = inboundItemMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.stocktakeTaskMapper = stocktakeTaskMapper;
        this.auditService = auditService;
    }

    public DashboardOverview overview() {
        List<Inventory> inventory = inventoryMapper.selectList(new LambdaQueryWrapper<>());
        long inventoryQty = inventory.stream().mapToLong(i -> i.getQty() == null ? 0 : i.getQty()).sum();
        long lockedQty = inventory.stream().mapToLong(i -> i.getLockedQty() == null ? 0 : i.getLockedQty()).sum();

        List<DashboardOverview.TrendPoint> trend = buildTrend(7);
        List<DashboardOverview.ZoneOccupancy> zones = buildZoneOccupancy();

        long outbound30 = outboundOrderMapper.selectList(new LambdaQueryWrapper<OutboundOrder>()
                        .ge(OutboundOrder::getCreatedAt, LocalDateTime.now().minusDays(30)))
                .stream().mapToLong(o -> o.getTotalQty() == null ? 0 : o.getTotalQty()).sum();
        double turnoverDays = outbound30 == 0 ? 0
                : Math.round(30.0 * inventoryQty / outbound30 * 10.0) / 10.0;

        List<StocktakeTask> tasks = stocktakeTaskMapper.selectList(null);
        long taskTotal = tasks.stream().mapToLong(t -> t.getTotal() == null ? 0 : t.getTotal()).sum();
        long diffTotal = tasks.stream().mapToLong(t -> t.getDiffCount() == null ? 0 : t.getDiffCount()).sum();
        double stockAccuracy = taskTotal == 0 ? 0
                : Math.round((taskTotal - diffTotal) * 1000.0 / taskTotal) / 10.0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<DashboardOverview.ActivityItem> activities = auditService.recent(6).stream()
                .map(log -> new DashboardOverview.ActivityItem(
                        log.getCreatedAt() == null ? "" : log.getCreatedAt().format(formatter),
                        log.getModule(), log.getTarget(), log.getUsername(),
                        "SUCCESS".equals(log.getResult()) ? "已完成" : log.getResult(), 0))
                .toList();

        return new DashboardOverview(
                productMapper.selectCount(new LambdaQueryWrapper<>()),
                supplierMapper.selectCount(new LambdaQueryWrapper<>()),
                customerMapper.selectCount(new LambdaQueryWrapper<>()),
                userMapper.selectCount(new LambdaQueryWrapper<>()),
                inventoryQty,
                lockedQty,
                stockAccuracy,
                turnoverDays,
                trend,
                zones,
                activities);
    }

    /**
     * Real inbound/outbound totals for the last {@code days} calendar days.
     * Inbound quantities come from inbound_item, outbound quantities from outbound_order.total_qty.
     */
    public List<DashboardOverview.TrendPoint> buildTrend(int days) {
        int span = Math.max(1, Math.min(90, days));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(span - 1L);
        LocalDateTime startTime = start.atStartOfDay();

        Map<LocalDate, Long> inboundByDay = new HashMap<>();
        List<InboundOrder> inboundOrders = inboundOrderMapper.selectList(
                new LambdaQueryWrapper<InboundOrder>().ge(InboundOrder::getCreatedAt, startTime));
        if (!inboundOrders.isEmpty()) {
            Map<Long, LocalDate> dateByOrder = inboundOrders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .collect(Collectors.toMap(InboundOrder::getId, o -> o.getCreatedAt().toLocalDate()));
            List<InboundItem> items = inboundItemMapper.selectList(
                    new LambdaQueryWrapper<InboundItem>().in(InboundItem::getOrderId, dateByOrder.keySet()));
            for (InboundItem item : items) {
                LocalDate date = dateByOrder.get(item.getOrderId());
                if (date != null) {
                    inboundByDay.merge(date, (long) (item.getQty() == null ? 0 : item.getQty()), Long::sum);
                }
            }
        }

        Map<LocalDate, Long> outboundByDay = new HashMap<>();
        List<OutboundOrder> outboundOrders = outboundOrderMapper.selectList(
                new LambdaQueryWrapper<OutboundOrder>().ge(OutboundOrder::getCreatedAt, startTime));
        for (OutboundOrder order : outboundOrders) {
            if (order.getCreatedAt() != null) {
                outboundByDay.merge(order.getCreatedAt().toLocalDate(),
                        (long) (order.getTotalQty() == null ? 0 : order.getTotalQty()), Long::sum);
            }
        }

        DateTimeFormatter label = DateTimeFormatter.ofPattern("MM/dd");
        List<DashboardOverview.TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < span; i++) {
            LocalDate date = start.plusDays(i);
            trend.add(new DashboardOverview.TrendPoint(
                    date.format(label),
                    inboundByDay.getOrDefault(date, 0L),
                    outboundByDay.getOrDefault(date, 0L)));
        }
        return trend;
    }

    /**
     * Occupancy is computed from location.status, grouped by zone. A location is occupied
     * when it is not FREE (OCCUPIED / LOCKED / CHECK all consume physical space).
     */
    public List<DashboardOverview.ZoneOccupancy> buildZoneOccupancy() {
        Map<Long, Zone> zones = zoneMapper.selectList(null).stream()
                .collect(Collectors.toMap(Zone::getId, Function.identity()));
        Map<Long, List<Location>> byZone = locationMapper.selectList(null).stream()
                .filter(l -> l.getZoneId() != null)
                .collect(Collectors.groupingBy(Location::getZoneId));

        return zones.values().stream()
                .sorted(Comparator.comparing(Zone::getCode))
                .map(zone -> {
                    List<Location> locations = byZone.getOrDefault(zone.getId(), List.of());
                    long occupied = locations.stream().filter(l -> !"FREE".equals(l.getStatus())).count();
                    int percent = locations.isEmpty() ? 0 : (int) Math.round(occupied * 100.0 / locations.size());
                    return new DashboardOverview.ZoneOccupancy(
                            zone.getCode() + " 区 " + zone.getName(), percent);
                })
                .toList();
    }
}
