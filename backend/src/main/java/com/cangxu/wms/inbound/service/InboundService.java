package com.cangxu.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.algorithm.AhpWeightCalculator;
import com.cangxu.wms.algorithm.AprioriAnalyzer;
import com.cangxu.wms.algorithm.TopsisRanker;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.inbound.dto.CreateInboundRequest;
import com.cangxu.wms.inbound.dto.InboundItemView;
import com.cangxu.wms.inbound.dto.InboundOrderView;
import com.cangxu.wms.inbound.dto.SlotCandidate;
import com.cangxu.wms.inbound.dto.SlotRecommendation;
import com.cangxu.wms.inbound.entity.InboundItem;
import com.cangxu.wms.inbound.entity.InboundOrder;
import com.cangxu.wms.inbound.mapper.InboundItemMapper;
import com.cangxu.wms.inbound.mapper.InboundOrderMapper;
import com.cangxu.wms.masterdata.entity.Inventory;
import com.cangxu.wms.masterdata.entity.Location;
import com.cangxu.wms.masterdata.entity.Product;
import com.cangxu.wms.masterdata.mapper.InventoryMapper;
import com.cangxu.wms.masterdata.mapper.LocationMapper;
import com.cangxu.wms.masterdata.mapper.ProductMapper;
import com.cangxu.wms.outbound.entity.OutboundItem;
import com.cangxu.wms.outbound.mapper.OutboundItemMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InboundService {

    private static final double[][] AHP_MATRIX = {
            {1.00, 1.26, 1.55, 2.00},
            {1.00 / 1.26, 1.00, 1.23, 1.59},
            {1.00 / 1.55, 1.00 / 1.23, 1.00, 1.29},
            {1.00 / 2.00, 1.00 / 1.59, 1.00 / 1.29, 1.00}
    };

    private final InboundOrderMapper orderMapper;
    private final InboundItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;
    private final OutboundItemMapper outboundItemMapper;
    private final AuditService auditService;

    public InboundService(InboundOrderMapper orderMapper,
                          InboundItemMapper itemMapper,
                          ProductMapper productMapper,
                          LocationMapper locationMapper,
                          InventoryMapper inventoryMapper,
                          OutboundItemMapper outboundItemMapper,
                          AuditService auditService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
        this.locationMapper = locationMapper;
        this.inventoryMapper = inventoryMapper;
        this.outboundItemMapper = outboundItemMapper;
        this.auditService = auditService;
    }

    public PageResult<InboundOrderView> page(long page, long size, String status, String keyword) {
        Page<InboundOrder> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<InboundOrder> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(InboundOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(InboundOrder::getNo, keyword)
                    .or().like(InboundOrder::getSupplierName, keyword));
        }
        query.orderByDesc(InboundOrder::getCreatedAt);
        Page<InboundOrder> result = orderMapper.selectPage(pageRequest, query);
        List<InboundOrderView> views = result.getRecords().stream().map(this::toView).toList();
        return new PageResult<>(views, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public InboundOrderView create(CreateInboundRequest request) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getSku, request.sku()));
        String no = "ASN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%03d", orderMapper.selectCount(null) + 1);
        LocalDateTime now = LocalDateTime.now();
        InboundOrder order = InboundOrder.builder()
                .no(no)
                .supplierName(request.supplierName())
                .dock(StringUtils.hasText(request.dock()) ? request.dock() : "1 号月台")
                .eta(request.eta() == null ? now.plusHours(2) : request.eta())
                .status("PENDING")
                .operator(currentUsername())
                .createdAt(now)
                .build();
        orderMapper.insert(order);

        InboundItem item = InboundItem.builder()
                .orderId(order.getId())
                .productId(product == null ? null : product.getId())
                .sku(request.sku())
                .productName(product == null ? request.productName() : product.getName())
                .qty(request.qty())
                .build();
        itemMapper.insert(item);

        auditService.logCurrent("INBOUND", "创建入库单", "inbound:" + no, "SUCCESS",
                request.sku() + " × " + request.qty());
        return toView(order);
    }

    public InboundOrderView receive(Long id) {
        return changeStatus(id, "RECEIVED", "收货确认");
    }

    public InboundOrderView inspect(Long id) {
        return changeStatus(id, "READY", "质检完成");
    }

    public SlotRecommendation recommend(Long id) {
        InboundOrder order = getOrder(id);
        InboundItem item = firstItem(id);
        List<Location> free = locationMapper.selectList(new LambdaQueryWrapper<Location>()
                .eq(Location::getStatus, "FREE")
                .orderByAsc(Location::getCode));
        if (free.isEmpty()) {
            throw new BusinessException("当前没有空闲库位可供推荐");
        }

        AhpWeightCalculator.AhpResult ahp = AhpWeightCalculator.compute(AHP_MATRIX);
        List<Location> candidates = free.stream().limit(8).toList();

        // Turnover comes from real outbound item quantities; correlation comes from
        // Apriori rules mined from outbound orders and the SKUs already stored nearby.
        List<OutboundItem> outboundItems = outboundItemMapper.selectList(null);
        Map<String, Double> turnoverBySku = new HashMap<>();
        Map<Long, Set<String>> transactions = new HashMap<>();
        for (OutboundItem row : outboundItems) {
            if (!StringUtils.hasText(row.getSku())) {
                continue;
            }
            turnoverBySku.merge(row.getSku(), (double) (row.getQty() == null ? 0 : row.getQty()),
                    Double::sum);
            transactions.computeIfAbsent(row.getOrderId(), orderId -> new HashSet<>()).add(row.getSku());
        }
        List<AprioriAnalyzer.Rule> rules = AprioriAnalyzer.minePairs(
                new ArrayList<>(transactions.values()), 0.01, 0.1);
        Map<Long, Set<String>> zoneSkus = new HashMap<>();
        for (Location location : locationMapper.selectList(
                new LambdaQueryWrapper<Location>().isNotNull(Location::getCurrentSku))) {
            zoneSkus.computeIfAbsent(location.getZoneId(), zoneId -> new HashSet<>())
                    .add(location.getCurrentSku());
        }

        double[][] matrix = new double[candidates.size()][4];
        for (int i = 0; i < candidates.size(); i++) {
            Location location = candidates.get(i);
            matrix[i][0] = turnoverBySku.getOrDefault(item.getSku(), 1.0);
            matrix[i][1] = correlationScore(item.getSku(), location.getZoneId(), zoneSkus, rules);
            matrix[i][2] = distanceScore(location.getCode());
            matrix[i][3] = fitScore(location.getCapacity(), item.getQty());
        }

        boolean[] benefit = {true, true, false, true};
        double[] closeness = TopsisRanker.rank(matrix, ahp.weights(), benefit);

        List<SlotCandidate> ranked = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Location location = candidates.get(i);
            ranked.add(new SlotCandidate(
                    location.getCode(),
                    round(closeness[i]),
                    round(matrix[i][0]),
                    round(matrix[i][1]),
                    round(matrix[i][2]),
                    round(matrix[i][3]),
                    location.getCapacity() == null ? 0 : location.getCapacity(),
                    location.getCurrentSku()
            ));
        }
        ranked.sort(Comparator.comparingDouble(SlotCandidate::score).reversed());

        return new SlotRecommendation(order.getId(), item.getSku(), item.getProductName(),
                item.getQty(), ahp.weights(), round(ahp.consistencyRatio()),
                ranked.stream().limit(5).toList());
    }

    @Transactional
    public InboundOrderView putaway(Long id, String locationCode) {
        InboundOrder order = getOrder(id);
        InboundItem item = firstItem(id);
        Location location = locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                .eq(Location::getCode, locationCode));
        if (location == null) {
            throw new BusinessException(404, "库位不存在：" + locationCode);
        }
        if ("LOCKED".equals(location.getStatus())) {
            throw new BusinessException(409, "库位已锁定，不能上架：" + locationCode);
        }

        item.setLocationId(location.getId());
        item.setLocationCode(location.getCode());
        itemMapper.updateById(item);

        location.setStatus("OCCUPIED");
        location.setCurrentSku(item.getSku());
        locationMapper.updateById(location);

        if (item.getProductId() != null) {
            Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .eq(Inventory::getLocationId, location.getId()));
            if (inventory == null) {
                inventoryMapper.insert(Inventory.builder()
                        .productId(item.getProductId())
                        .locationId(location.getId())
                        .qty(item.getQty())
                        .lockedQty(0)
                        .updatedAt(LocalDateTime.now())
                        .build());
            } else {
                inventory.setQty(inventory.getQty() + item.getQty());
                inventory.setUpdatedAt(LocalDateTime.now());
                inventoryMapper.updateById(inventory);
            }
        }

        order.setStatus("DONE");
        orderMapper.updateById(order);
        auditService.logCurrent("INBOUND", "上架确认", "inbound:" + order.getNo(), "SUCCESS",
                item.getSku() + " → " + locationCode);
        return toView(order);
    }

    public Map<String, Long> summary() {
        return Map.of(
                "total", orderMapper.selectCount(null),
                "pending", orderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getStatus, "PENDING")),
                "ready", orderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getStatus, "READY")),
                "done", orderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getStatus, "DONE"))
        );
    }

    private InboundOrderView changeStatus(Long id, String status, String action) {
        InboundOrder order = getOrder(id);
        order.setStatus(status);
        orderMapper.updateById(order);
        auditService.logCurrent("INBOUND", action, "inbound:" + order.getNo(), "SUCCESS", status);
        return toView(order);
    }

    private InboundOrder getOrder(Long id) {
        InboundOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "入库单不存在");
        }
        return order;
    }

    private InboundItem firstItem(Long orderId) {
        List<InboundItem> items = itemMapper.selectList(new LambdaQueryWrapper<InboundItem>()
                .eq(InboundItem::getOrderId, orderId)
                .orderByAsc(InboundItem::getId));
        if (items.isEmpty()) {
            throw new BusinessException(409, "入库单没有商品明细");
        }
        return items.get(0);
    }

    private InboundOrderView toView(InboundOrder order) {
        List<InboundItem> items = itemMapper.selectList(new LambdaQueryWrapper<InboundItem>()
                .eq(InboundItem::getOrderId, order.getId())
                .orderByAsc(InboundItem::getId));
        List<InboundItemView> itemViews = items.stream()
                .map(item -> new InboundItemView(item.getId(), item.getSku(),
                        item.getProductName(), item.getQty(), item.getLocationCode()))
                .toList();
        String locationCode = items.isEmpty() ? null : items.get(0).getLocationCode();
        return new InboundOrderView(order.getId(), order.getNo(), order.getSupplierName(),
                order.getDock(), order.getEta(), order.getStatus(), order.getOperator(),
                order.getCreatedAt(), itemViews, locationCode);
    }

    private double correlationScore(String sku, Long zoneId,
                                    Map<Long, Set<String>> zoneSkus,
                                    List<AprioriAnalyzer.Rule> rules) {
        Set<String> nearbySkus = zoneSkus.getOrDefault(zoneId, Set.of());
        double score = 0;
        for (AprioriAnalyzer.Rule rule : rules) {
            if (sku.equals(rule.antecedent()) && nearbySkus.contains(rule.consequent())) {
                score += rule.lift();
            }
        }
        return score;
    }

    private double distanceScore(String code) {
        int zone = code.isEmpty() ? 0 : Math.max(0, code.charAt(0) - 'A');
        int row = code.length() >= 4 ? Integer.parseInt(code.substring(2, 4)) : 1;
        int col = code.length() >= 7 ? Integer.parseInt(code.substring(5, 7)) : 1;
        return 20 + zone * 12 + row * 4 + col;
    }

    private double fitScore(Integer capacity, Integer qty) {
        if (capacity == null || qty == null || qty <= 0) {
            return 50;
        }
        return Math.min(100, 40 + 60.0 * Math.min(1.0, (double) capacity / qty));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
