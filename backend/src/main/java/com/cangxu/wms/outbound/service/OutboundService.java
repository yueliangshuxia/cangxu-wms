package com.cangxu.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.outbound.dto.CreateOutboundRequest;
import com.cangxu.wms.outbound.dto.OutboundRoute;
import com.cangxu.wms.outbound.entity.OutboundItem;
import com.cangxu.wms.outbound.entity.OutboundOrder;
import com.cangxu.wms.outbound.mapper.OutboundItemMapper;
import com.cangxu.wms.outbound.mapper.OutboundOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutboundService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundItemMapper outboundItemMapper;
    private final AuditService auditService;

    public OutboundService(OutboundOrderMapper outboundOrderMapper,
                           OutboundItemMapper outboundItemMapper,
                           AuditService auditService) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundItemMapper = outboundItemMapper;
        this.auditService = auditService;
    }

    public PageResult<OutboundOrder> page(long page, long size, String status, String keyword) {
        Page<OutboundOrder> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<OutboundOrder> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(OutboundOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(OutboundOrder::getNo, keyword)
                    .or().like(OutboundOrder::getCustomerName, keyword));
        }
        query.orderByDesc(OutboundOrder::getCreatedAt);
        Page<OutboundOrder> result = outboundOrderMapper.selectPage(pageRequest, query);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public OutboundOrder create(CreateOutboundRequest request) {
        String no = "SO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%03d", outboundOrderMapper.selectCount(null) + 1);
        OutboundOrder order = OutboundOrder.builder()
                .no(no)
                .customerName(request.customerName())
                .priority(StringUtils.hasText(request.priority()) ? request.priority() : "NORMAL")
                .status("PENDING")
                .itemCount(request.itemCount() == null ? 1 : request.itemCount())
                .totalQty(request.totalQty() == null ? 1 : request.totalQty())
                .promisedAt(request.promisedAt())
                .createdAt(LocalDateTime.now())
                .build();
        outboundOrderMapper.insert(order);
        auditService.logCurrent("OUTBOUND", "创建出库单", "outbound:" + no, "SUCCESS", order.getCustomerName());
        return order;
    }

    @Transactional
    public Map<String, Object> generateWave() {
        List<OutboundOrder> pending = outboundOrderMapper.selectList(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getStatus, "PENDING"));
        if (pending.isEmpty()) {
            throw new BusinessException("没有待分配订单");
        }
        String waveNo = "W-" + String.format("%02d", (outboundOrderMapper.selectCount(null) % 90) + 8);
        for (OutboundOrder order : pending) {
            order.setWaveNo(waveNo);
            order.setStatus("PICKING");
            outboundOrderMapper.updateById(order);
        }
        auditService.logCurrent("OUTBOUND", "生成波次", "wave:" + waveNo, "SUCCESS", pending.size() + " 个订单");
        return Map.of("waveNo", waveNo, "count", pending.size());
    }

    public OutboundOrder pick(Long id) {
        return changeStatus(id, "REVIEW", "拣货完成");
    }

    public OutboundOrder ship(Long id) {
        return changeStatus(id, "SHIPPED", "发运确认");
    }

    /**
     * Real picking route for a wave. Items are aggregated by location code and sorted
     * zone → row → column, which is how a warehouse operator would walk the aisles.
     */
    public OutboundRoute route(String waveNo) {
        String targetWave = waveNo;
        if (!StringUtils.hasText(targetWave)) {
            List<OutboundOrder> picking = outboundOrderMapper.selectList(
                    new LambdaQueryWrapper<OutboundOrder>()
                            .isNotNull(OutboundOrder::getWaveNo)
                            .eq(OutboundOrder::getStatus, "PICKING")
                            .orderByAsc(OutboundOrder::getId)
                            .last("LIMIT 1"));
            List<OutboundOrder> anyWave = outboundOrderMapper.selectList(
                    new LambdaQueryWrapper<OutboundOrder>()
                            .isNotNull(OutboundOrder::getWaveNo)
                            .orderByAsc(OutboundOrder::getId)
                            .last("LIMIT 1"));
            targetWave = !picking.isEmpty() ? picking.get(0).getWaveNo()
                    : (!anyWave.isEmpty() ? anyWave.get(0).getWaveNo() : null);
        }
        if (!StringUtils.hasText(targetWave)) {
            return new OutboundRoute("", 0, 0, 0, List.of());
        }

        List<OutboundOrder> orders = outboundOrderMapper.selectList(
                new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getWaveNo, targetWave));
        if (orders.isEmpty()) {
            return new OutboundRoute(targetWave, 0, 0, 0, List.of());
        }
        List<Long> orderIds = orders.stream().map(OutboundOrder::getId).toList();
        List<OutboundItem> items = outboundItemMapper.selectList(
                new LambdaQueryWrapper<OutboundItem>().in(OutboundItem::getOrderId, orderIds));

        Map<String, StopAggregate> aggregated = new LinkedHashMap<>();
        for (OutboundItem item : items) {
            if (!StringUtils.hasText(item.getLocationCode())) {
                continue;
            }
            int qty = item.getQty() == null ? 0 : item.getQty();
            StopAggregate aggregate = aggregated.get(item.getLocationCode());
            if (aggregate == null) {
                aggregated.put(item.getLocationCode(),
                        new StopAggregate(item.getLocationCode(), item.getProductName(), qty));
            } else {
                aggregate.qty += qty;
                if (item.getProductName() != null && !item.getProductName().equals(aggregate.name)) {
                    aggregate.name = aggregate.name.replace(" 等", "") + " 等";
                }
            }
        }

        List<OutboundRoute.RouteStop> stops = new ArrayList<>();
        int seq = 1;
        List<StopAggregate> ordered = aggregated.values().stream()
                .sorted(Comparator.comparing((StopAggregate s) -> s.zone)
                        .thenComparingInt(s -> s.row)
                        .thenComparingInt(s -> s.col)
                        .thenComparing(s -> s.code))
                .toList();
        for (StopAggregate stop : ordered) {
            stops.add(new OutboundRoute.RouteStop(seq++, stop.code, stop.name, stop.qty,
                    stop.zone, stop.row, stop.col));
        }
        long totalQty = items.stream().mapToLong(i -> i.getQty() == null ? 0 : i.getQty()).sum();
        return new OutboundRoute(targetWave, orders.size(), items.size(), totalQty, stops);
    }

    private static final class StopAggregate {
        private final String code;
        private String name;
        private int qty;
        private final String zone;
        private final int row;
        private final int col;

        private StopAggregate(String code, String name, int qty) {
            this.code = code;
            this.name = name == null ? "" : name;
            this.qty = qty;
            String[] parts = code.split("-");
            this.zone = parts.length > 0 ? parts[0] : "?";
            this.row = parts.length > 1 ? parseInt(parts[1]) : 0;
            this.col = parts.length > 2 ? parseInt(parts[2]) : 0;
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    public Map<String, Long> summary() {
        return Map.of(
                "total", outboundOrderMapper.selectCount(null),
                "pending", outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, "PENDING")),
                "picking", outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, "PICKING")),
                "review", outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, "REVIEW")),
                "shipped", outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, "SHIPPED"))
        );
    }

    private OutboundOrder changeStatus(Long id, String status, String action) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "出库单不存在");
        }
        order.setStatus(status);
        outboundOrderMapper.updateById(order);
        auditService.logCurrent("OUTBOUND", action, "outbound:" + order.getNo(), "SUCCESS", status);
        return order;
    }
}
