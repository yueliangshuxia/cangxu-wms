package com.cangxu.wms.config;

import com.cangxu.wms.masterdata.entity.Location;
import com.cangxu.wms.masterdata.entity.Zone;
import com.cangxu.wms.masterdata.mapper.LocationMapper;
import com.cangxu.wms.masterdata.mapper.ZoneMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates the demo warehouse floor on first start.
 * Data.sql seeds a few known locations; the rest are added here so the map page is meaningful.
 */
@Component
@Order(10)
public class LocationInitializer implements CommandLineRunner {

    private static final String[] ZONE_CODES = {"A", "B", "C", "D", "E"};
    private static final String[] ZONE_NAMES = {"电子配件", "智能家居", "户外与生活", "大件商品", "退货暂存"};
    private static final String[] ZONE_TEMPS = {"NORMAL", "NORMAL", "NORMAL", "CONSTANT", "NORMAL"};
    private static final String[] SKUS = {
            "WH-1001", "WH-1002", "WH-1003", "WH-1004", "WH-1005", "WH-1006",
            "WH-1007", "WH-2001", "WH-2002", "WH-3001", "WH-3002", "WH-2003"
    };
    private static final Map<String, String> KNOWN_SKU = Map.of(
            "A-01-03", "WH-1001",
            "A-02-01", "WH-1003",
            "B-01-02", "WH-2001",
            "C-03-01", "WH-3002"
    );
    private static final Set<String> KNOWN_FREE = Set.of(
            "A-01-10", "B-01-05", "D-02-01", "E-01-01"
    );

    private final ZoneMapper zoneMapper;
    private final LocationMapper locationMapper;

    public LocationInitializer(ZoneMapper zoneMapper, LocationMapper locationMapper) {
        this.zoneMapper = zoneMapper;
        this.locationMapper = locationMapper;
    }

    @Override
    public void run(String... args) {
        if (zoneMapper.selectCount(null) == 0) {
            for (int i = 0; i < ZONE_CODES.length; i++) {
                zoneMapper.insert(Zone.builder()
                        .warehouseId(1L)
                        .code(ZONE_CODES[i])
                        .name(ZONE_NAMES[i])
                        .tempType(ZONE_TEMPS[i])
                        .build());
            }
        }

        List<Zone> zones = zoneMapper.selectList(null);
        Set<String> existing = locationMapper.selectList(null).stream()
                .map(Location::getCode)
                .collect(Collectors.toSet());

        for (int zi = 0; zi < zones.size(); zi++) {
            Zone zone = zones.get(zi);
            int rows = "E".equals(zone.getCode()) ? 2 : 3;
            for (int r = 1; r <= rows; r++) {
                for (int c = 1; c <= 10; c++) {
                    String code = String.format("%s-%02d-%02d", zone.getCode(), r, c);
                    if (existing.contains(code)) {
                        continue;
                    }
                    int h = (zi * 37 + r * 11 + c * 7) % 100;
                    String status;
                    String sku = null;
                    if (KNOWN_SKU.containsKey(code)) {
                        status = "OCCUPIED";
                        sku = KNOWN_SKU.get(code);
                    } else if (KNOWN_FREE.contains(code)) {
                        status = "FREE";
                    } else if (h < 78) {
                        status = "OCCUPIED";
                        sku = SKUS[(zi * 7 + r * 3 + c) % SKUS.length];
                    } else if (h < 90) {
                        status = "FREE";
                    } else if (h < 96) {
                        status = "LOCKED";
                    } else {
                        status = "CHECK";
                    }
                    locationMapper.insert(Location.builder()
                            .zoneId(zone.getId())
                            .code(code)
                            .rowNo(r)
                            .colNo(c)
                            .capacity("D".equals(zone.getCode()) ? 60 : 120)
                            .status(status)
                            .currentSku(sku)
                            .build());
                }
            }
        }
    }
}
