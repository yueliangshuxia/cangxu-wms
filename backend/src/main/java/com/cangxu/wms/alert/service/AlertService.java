package com.cangxu.wms.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.alert.dto.AlertItem;
import com.cangxu.wms.device.dto.DeviceSummary;
import com.cangxu.wms.device.entity.Device;
import com.cangxu.wms.device.mapper.DeviceMapper;
import com.cangxu.wms.device.service.DeviceService;
import com.cangxu.wms.masterdata.entity.Inventory;
import com.cangxu.wms.masterdata.mapper.InventoryMapper;
import com.cangxu.wms.masterdata.mapper.ProductMapper;
import com.cangxu.wms.returns.entity.ReturnOrder;
import com.cangxu.wms.returns.mapper.ReturnOrderMapper;
import com.cangxu.wms.stocktake.entity.StocktakeTask;
import com.cangxu.wms.stocktake.mapper.StocktakeTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceService deviceService;
    private final ReturnOrderMapper returnOrderMapper;
    private final StocktakeTaskMapper stocktakeTaskMapper;

    public AlertService(ProductMapper productMapper,
                        InventoryMapper inventoryMapper,
                        DeviceMapper deviceMapper,
                        DeviceService deviceService,
                        ReturnOrderMapper returnOrderMapper,
                        StocktakeTaskMapper stocktakeTaskMapper) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.deviceMapper = deviceMapper;
        this.deviceService = deviceService;
        this.returnOrderMapper = returnOrderMapper;
        this.stocktakeTaskMapper = stocktakeTaskMapper;
    }

    public List<AlertItem> list() {
        List<AlertItem> alerts = new ArrayList<>();

        Map<Long, Integer> availableByProduct = new HashMap<>();
        for (Inventory row : inventoryMapper.selectList(null)) {
            int available = (row.getQty() == null ? 0 : row.getQty())
                    - (row.getLockedQty() == null ? 0 : row.getLockedQty());
            availableByProduct.merge(row.getProductId(), available, Integer::sum);
        }
        productMapper.selectList(null).stream()
                .filter(product -> {
                    Integer available = availableByProduct.get(product.getId());
                    int safety = product.getSafetyStock() == null ? 0 : product.getSafetyStock();
                    return available != null && available < safety;
                })
                .limit(3)
                .forEach(product -> {
                    int available = availableByProduct.getOrDefault(product.getId(), 0);
                    alerts.add(new AlertItem(
                            "stock-" + product.getSku(), "warn", "package-search",
                            "库存预警：" + product.getName(),
                            "可用 " + available + " 件，低于安全库存 " + product.getSafetyStock() + " 件",
                            "/inventory"));
                });

        List<Device> offline = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getStatus, "OFFLINE"));
        for (Device device : offline) {
            alerts.add(new AlertItem("device-" + device.getCode(), "danger", "bot",
                    "设备离线：" + device.getCode(),
                    device.getLocation() + " · 请检查网关与供电", "/devices"));
        }

        DeviceSummary summary = deviceService.summary();
        if (summary.temperature() > 26) {
            alerts.add(new AlertItem("temp-a01", "danger", "thermometer",
                    "A 区温度超过阈值",
                    "当前 " + String.format("%.1f", summary.temperature()) + "°C · 阈值 26°C",
                    "/devices"));
        }
        if (summary.humidity() > 65) {
            alerts.add(new AlertItem("humidity-a01", "warn", "droplets",
                    "A 区湿度超过阈值",
                    "当前 " + String.format("%.0f", summary.humidity()) + "% · 阈值 65%",
                    "/devices"));
        }

        long pendingReturns = returnOrderMapper.selectCount(new LambdaQueryWrapper<ReturnOrder>()
                .eq(ReturnOrder::getStatus, "PENDING_INSPECTION"));
        if (pendingReturns > 0) {
            alerts.add(new AlertItem("returns-pending", "warn", "rotate-ccw",
                    pendingReturns + " 单退货待质检",
                    "请在退货管理中完成质检判定", "/returns"));
        }

        long pendingStocktakes = stocktakeTaskMapper.selectCount(new LambdaQueryWrapper<StocktakeTask>()
                .in(StocktakeTask::getStatus, "PENDING", "IN_PROGRESS"));
        if (pendingStocktakes > 0) {
            alerts.add(new AlertItem("stocktake-pending", "info", "clipboard-list",
                    pendingStocktakes + " 个盘点任务进行中",
                    "查看盘点差异与处理进度", "/stocktake"));
        }

        return alerts.stream().limit(10).toList();
    }
}
