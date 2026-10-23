package com.cangxu.wms.masterdata.service;

import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.masterdata.dto.InventoryRow;
import com.cangxu.wms.masterdata.entity.Inventory;
import com.cangxu.wms.masterdata.entity.Location;
import com.cangxu.wms.masterdata.entity.Product;
import com.cangxu.wms.masterdata.mapper.InventoryMapper;
import com.cangxu.wms.masterdata.mapper.LocationMapper;
import com.cangxu.wms.masterdata.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final LocationMapper locationMapper;

    public InventoryService(InventoryMapper inventoryMapper,
                            ProductMapper productMapper,
                            LocationMapper locationMapper) {
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.locationMapper = locationMapper;
    }

    public PageResult<InventoryRow> page(long page, long size, String keyword, String category, String status) {
        Map<Long, Product> products = productMapper.selectList(null).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Location> locations = locationMapper.selectList(null).stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));

        List<Inventory> allInventory = inventoryMapper.selectList(null);
        Map<Long, Integer> availableByProduct = new HashMap<>();
        for (Inventory inventory : allInventory) {
            int available = (inventory.getQty() == null ? 0 : inventory.getQty())
                    - (inventory.getLockedQty() == null ? 0 : inventory.getLockedQty());
            availableByProduct.merge(inventory.getProductId(), available, Integer::sum);
        }

        List<InventoryRow> rows = allInventory.stream()
                .map(inventory -> toRow(inventory, products, locations, availableByProduct))
                .filter(Objects::nonNull)
                .filter(row -> !StringUtils.hasText(keyword)
                        || (row.sku() + " " + row.name() + " " + row.locationCode()).toLowerCase()
                        .contains(keyword.toLowerCase()))
                .filter(row -> !StringUtils.hasText(category) || category.equals(row.category()))
                .filter(row -> !StringUtils.hasText(status) || status.equals(row.status()))
                .sorted(Comparator.comparing(InventoryRow::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int from = (int) Math.max(0, (page - 1) * size);
        int to = (int) Math.min(rows.size(), from + size);
        List<InventoryRow> pageRows = from >= rows.size() ? List.of() : rows.subList(from, to);
        return new PageResult<>(pageRows, rows.size(), page, size);
    }

    public Map<String, Object> summary() {
        List<Inventory> all = inventoryMapper.selectList(null);
        Map<Long, Product> products = productMapper.selectList(null).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Integer> availableByProduct = new HashMap<>();
        long qty = 0;
        long locked = 0;
        for (Inventory inventory : all) {
            int rowQty = inventory.getQty() == null ? 0 : inventory.getQty();
            int rowLocked = inventory.getLockedQty() == null ? 0 : inventory.getLockedQty();
            qty += rowQty;
            locked += rowLocked;
            availableByProduct.merge(inventory.getProductId(), rowQty - rowLocked, Integer::sum);
        }
        long low = products.values().stream().filter(product -> {
            Integer available = availableByProduct.get(product.getId());
            return available != null && available < (product.getSafetyStock() == null ? 0 : product.getSafetyStock());
        }).count();
        return Map.of(
                "totalQty", qty,
                "lockedQty", locked,
                "availableQty", qty - locked,
                "lowCount", low,
                "skuCount", availableByProduct.size()
        );
    }

    private InventoryRow toRow(Inventory inventory, Map<Long, Product> products,
                               Map<Long, Location> locations, Map<Long, Integer> availableByProduct) {
        Product product = products.get(inventory.getProductId());
        if (product == null) {
            return null;
        }
        Location location = locations.get(inventory.getLocationId());
        int qty = inventory.getQty() == null ? 0 : inventory.getQty();
        int locked = inventory.getLockedQty() == null ? 0 : inventory.getLockedQty();
        int available = qty - locked;
        int safety = product.getSafetyStock() == null ? 0 : product.getSafetyStock();
        int skuAvailable = availableByProduct.getOrDefault(product.getId(), available);
        String status = skuAvailable <= 0 ? "OUT" : skuAvailable < safety ? "LOW" : "NORMAL";
        return new InventoryRow(
                inventory.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                location == null ? "未分配" : location.getCode(),
                qty,
                locked,
                available,
                safety,
                status,
                product.getImageUrl(),
                inventory.getUpdatedAt()
        );
    }
}
