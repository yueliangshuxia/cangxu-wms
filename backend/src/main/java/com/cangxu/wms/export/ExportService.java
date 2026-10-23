package com.cangxu.wms.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.analytics.dto.AnalyticsOverview;
import com.cangxu.wms.analytics.service.AnalyticsService;
import com.cangxu.wms.audit.entity.AuditLog;
import com.cangxu.wms.audit.mapper.AuditLogMapper;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.inbound.entity.InboundItem;
import com.cangxu.wms.inbound.entity.InboundOrder;
import com.cangxu.wms.inbound.mapper.InboundItemMapper;
import com.cangxu.wms.inbound.mapper.InboundOrderMapper;
import com.cangxu.wms.masterdata.dto.InventoryRow;
import com.cangxu.wms.masterdata.dto.LocationRow;
import com.cangxu.wms.masterdata.entity.Customer;
import com.cangxu.wms.masterdata.entity.Product;
import com.cangxu.wms.masterdata.entity.Supplier;
import com.cangxu.wms.masterdata.mapper.CustomerMapper;
import com.cangxu.wms.masterdata.mapper.ProductMapper;
import com.cangxu.wms.masterdata.mapper.SupplierMapper;
import com.cangxu.wms.masterdata.service.InventoryService;
import com.cangxu.wms.masterdata.service.LocationService;
import com.cangxu.wms.outbound.entity.OutboundOrder;
import com.cangxu.wms.outbound.mapper.OutboundOrderMapper;
import com.cangxu.wms.returns.entity.ReturnOrder;
import com.cangxu.wms.returns.mapper.ReturnOrderMapper;
import com.cangxu.wms.stocktake.entity.StocktakeTask;
import com.cangxu.wms.stocktake.mapper.StocktakeTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InventoryService inventoryService;
    private final LocationService locationService;
    private final ProductMapper productMapper;
    private final SupplierMapper supplierMapper;
    private final CustomerMapper customerMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundItemMapper inboundItemMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final StocktakeTaskMapper stocktakeTaskMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final AuditLogMapper auditLogMapper;
    private final AuditService auditService;
    private final AnalyticsService analyticsService;

    public ExportService(InventoryService inventoryService,
                         LocationService locationService,
                         ProductMapper productMapper,
                         SupplierMapper supplierMapper,
                         CustomerMapper customerMapper,
                         InboundOrderMapper inboundOrderMapper,
                         InboundItemMapper inboundItemMapper,
                         OutboundOrderMapper outboundOrderMapper,
                         StocktakeTaskMapper stocktakeTaskMapper,
                         ReturnOrderMapper returnOrderMapper,
                         AuditLogMapper auditLogMapper,
                         AuditService auditService,
                         AnalyticsService analyticsService) {
        this.inventoryService = inventoryService;
        this.locationService = locationService;
        this.productMapper = productMapper;
        this.supplierMapper = supplierMapper;
        this.customerMapper = customerMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundItemMapper = inboundItemMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.stocktakeTaskMapper = stocktakeTaskMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.auditLogMapper = auditLogMapper;
        this.auditService = auditService;
        this.analyticsService = analyticsService;
    }

    public byte[] inventory(String keyword, String category, String status) {
        List<InventoryRow> rows = inventoryService.page(1, 100_000, keyword, category, status).records();
        List<List<?>> data = new ArrayList<>();
        for (InventoryRow row : rows) {
            data.add(row(
                    row.sku(), row.name(), row.category(), row.locationCode(),
                    row.qty(), row.lockedQty(), row.available(), row.safetyStock(),
                    row.status(), format(row.updatedAt())));
        }
        auditService.logCurrent("INVENTORY", "导出库存", "inventory", "SUCCESS", rows.size() + " 行");
        return CsvUtils.write(List.of("SKU", "商品", "分类", "库位", "库存", "锁定", "可用", "安全库存", "状态", "更新时间"), data);
    }

    public byte[] locations() {
        List<LocationRow> rows = locationService.list();
        List<List<?>> data = new ArrayList<>();
        for (LocationRow row : rows) {
            data.add(row(row.code(), row.zoneCode(), row.zoneName(), row.tempType(),
                    row.rowNo(), row.colNo(), row.capacity(), row.status(),
                    row.currentSku() == null ? "" : row.currentSku()));
        }
        auditService.logCurrent("LOCATION", "导出库位", "location", "SUCCESS", rows.size() + " 行");
        return CsvUtils.write(List.of("库位编码", "库区", "库区名称", "温区", "巷道", "列", "容量", "状态", "当前 SKU"), data);
    }

    public byte[] inbound(String status, String keyword) {
        LambdaQueryWrapper<InboundOrder> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(InboundOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(InboundOrder::getNo, keyword)
                    .or().like(InboundOrder::getSupplierName, keyword));
        }
        query.orderByDesc(InboundOrder::getCreatedAt);
        List<InboundOrder> orders = inboundOrderMapper.selectList(query);
        Map<Long, InboundOrder> byId = orders.stream()
                .collect(Collectors.toMap(InboundOrder::getId, Function.identity()));
        List<InboundItem> items = byId.isEmpty() ? List.of()
                : inboundItemMapper.selectList(new LambdaQueryWrapper<InboundItem>()
                .in(InboundItem::getOrderId, byId.keySet()));
        List<List<?>> data = new ArrayList<>();
        for (InboundItem item : items) {
            InboundOrder order = byId.get(item.getOrderId());
            if (order == null) {
                continue;
            }
            data.add(row(order.getNo(), order.getSupplierName(), order.getDock(),
                    order.getStatus(), order.getOperator(), item.getSku(), item.getProductName(),
                    item.getQty(), item.getLocationCode() == null ? "" : item.getLocationCode(),
                    format(order.getCreatedAt())));
        }
        auditService.logCurrent("INBOUND", "导出入库单", "inbound", "SUCCESS", data.size() + " 行");
        return CsvUtils.write(List.of("入库单号", "供应商", "月台", "状态", "操作人", "SKU", "商品", "数量", "库位", "创建时间"), data);
    }

    public byte[] outbound(String status, String keyword) {
        LambdaQueryWrapper<OutboundOrder> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(OutboundOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(OutboundOrder::getNo, keyword)
                    .or().like(OutboundOrder::getCustomerName, keyword));
        }
        query.orderByDesc(OutboundOrder::getCreatedAt);
        List<List<?>> data = new ArrayList<>();
        for (OutboundOrder order : outboundOrderMapper.selectList(query)) {
            data.add(row(
                    order.getNo(), order.getCustomerName(), order.getPriority(), order.getWaveNo(),
                    order.getStatus(), order.getItemCount(), order.getTotalQty(), order.getPromisedAt(),
                    format(order.getCreatedAt())));
        }
        auditService.logCurrent("OUTBOUND", "导出出库单", "outbound", "SUCCESS", data.size() + " 行");
        return CsvUtils.write(List.of("出库单号", "客户", "优先级", "波次", "状态", "商品数", "数量", "承诺时间", "创建时间"), data);
    }

    public byte[] stocktakes(String status) {
        LambdaQueryWrapper<StocktakeTask> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(StocktakeTask::getStatus, status);
        }
        query.orderByDesc(StocktakeTask::getId);
        List<List<?>> data = new ArrayList<>();
        for (StocktakeTask task : stocktakeTaskMapper.selectList(query)) {
            data.add(row(
                    task.getNo(), task.getScope(), task.getType(), task.getOwner(), task.getStatus(),
                    task.getTotal(), task.getCounted(), task.getDiffCount(), task.getStartedAt(),
                    format(task.getCreatedAt())));
        }
        auditService.logCurrent("STOCKTAKE", "导出盘点", "stocktake", "SUCCESS", data.size() + " 行");
        return CsvUtils.write(List.of("盘点单号", "范围", "类型", "负责人", "状态", "总数", "已盘", "差异", "开始时间", "创建时间"), data);
    }

    public byte[] returns(String status, String keyword) {
        LambdaQueryWrapper<ReturnOrder> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(ReturnOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(ReturnOrder::getNo, keyword)
                    .or().like(ReturnOrder::getCustomerName, keyword)
                    .or().like(ReturnOrder::getProductName, keyword));
        }
        query.orderByDesc(ReturnOrder::getId);
        List<List<?>> data = new ArrayList<>();
        for (ReturnOrder order : returnOrderMapper.selectList(query)) {
            data.add(row(
                    order.getNo(), order.getCustomerName(), order.getProductName(), order.getSku(),
                    order.getQty(), order.getReason(), order.getStatus(), order.getDisposition(),
                    order.getAppliedAt(), format(order.getCreatedAt())));
        }
        auditService.logCurrent("RETURNS", "导出退货单", "returns", "SUCCESS", data.size() + " 行");
        return CsvUtils.write(List.of("退货单号", "客户", "商品", "SKU", "数量", "原因", "状态", "处置", "申请时间", "创建时间"), data);
    }

    public byte[] auditLogs(String module, String result, String keyword) {
        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            query.eq(AuditLog::getModule, module);
        }
        if (StringUtils.hasText(result)) {
            query.eq(AuditLog::getResult, result);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(AuditLog::getUsername, keyword)
                    .or().like(AuditLog::getAction, keyword)
                    .or().like(AuditLog::getTarget, keyword));
        }
        query.orderByDesc(AuditLog::getCreatedAt);
        List<List<?>> data = new ArrayList<>();
        for (AuditLog log : auditLogMapper.selectList(query)) {
            data.add(row(
                    format(log.getCreatedAt()), log.getUsername(), log.getRoleCode(), log.getModule(),
                    log.getAction(), log.getTarget(), log.getIp(), log.getResult(), log.getDetail()));
        }
        return CsvUtils.write(List.of("时间", "操作人", "角色", "模块", "操作", "对象", "IP", "结果", "详情"), data);
    }

    public byte[] masterdata() {
        List<List<?>> data = new ArrayList<>();
        for (Product product : productMapper.selectList(null)) {
            data.add(row("商品", product.getSku(), product.getName(), product.getCategory(),
                    "", "", product.getStatus()));
        }
        for (Supplier supplier : supplierMapper.selectList(null)) {
            data.add(row("供应商", supplier.getCode(), supplier.getName(), supplier.getCategory(),
                    supplier.getContact(), supplier.getPhone(), supplier.getStatus()));
        }
        for (Customer customer : customerMapper.selectList(null)) {
            data.add(row("客户", customer.getCode(), customer.getName(), customer.getChannel(),
                    customer.getContact(), customer.getPhone(), customer.getStatus()));
        }
        auditService.logCurrent("MASTERDATA", "导出主数据", "masterdata", "SUCCESS", data.size() + " 行");
        return CsvUtils.write(List.of("类型", "编码", "名称", "分类/渠道", "联系人", "电话", "状态"), data);
    }

    public byte[] analytics(int days) {
        AnalyticsOverview overview = analyticsService.overview(days);
        List<List<?>> data = new ArrayList<>();
        data.add(row("指标", "数值", "单位"));
        data.add(row("统计天数", overview.days(), "天"));
        data.add(row("入库总量", overview.kpis().inboundQty(), "件"));
        data.add(row("出库总量", overview.kpis().outboundQty(), "件"));
        data.add(row("库存周转率", overview.kpis().turnover(), "次/月"));
        data.add(row("库位利用率", overview.kpis().utilization(), "%"));
        data.add(row("实验订单数", overview.experiment().orders(), "条"));
        data.add(row("实验 SKU 数", overview.experiment().skus(), "个"));
        data.add(row("AHP 一致性 CR", overview.experiment().consistencyRatio(), ""));
        data.add(row("", "", ""));
        data.add(row("策略", "平均拣货距离(m/单)", "平均履行时间(min/单)", "库位利用率(%)"));
        for (AnalyticsOverview.StrategyResult strategy : overview.experiment().strategies()) {
            data.add(row(strategy.method(), strategy.distance(), strategy.time(), strategy.utilization()));
        }
        auditService.logCurrent("SYSTEM", "导出分析报告", "analytics", "SUCCESS", days + " 天");
        return CsvUtils.write(List.of("分析报告", "", ""), data);
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME);
    }
}
