package com.cangxu.wms.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.masterdata.dto.LocationRow;
import com.cangxu.wms.masterdata.entity.Location;
import com.cangxu.wms.masterdata.entity.Zone;
import com.cangxu.wms.masterdata.mapper.LocationMapper;
import com.cangxu.wms.masterdata.mapper.ZoneMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final Set<String> ALLOWED_STATUS = Set.of("FREE", "OCCUPIED", "LOCKED", "CHECK");

    private final LocationMapper locationMapper;
    private final ZoneMapper zoneMapper;
    private final AuditService auditService;

    public LocationService(LocationMapper locationMapper, ZoneMapper zoneMapper, AuditService auditService) {
        this.locationMapper = locationMapper;
        this.zoneMapper = zoneMapper;
        this.auditService = auditService;
    }

    public List<LocationRow> list() {
        Map<Long, Zone> zones = zoneMapper.selectList(null).stream()
                .collect(Collectors.toMap(Zone::getId, Function.identity()));
        return locationMapper.selectList(new LambdaQueryWrapper<Location>().orderByAsc(Location::getCode))
                .stream()
                .map(location -> {
                    Zone zone = zones.get(location.getZoneId());
                    return new LocationRow(
                            location.getId(),
                            location.getCode(),
                            zone == null ? "—" : zone.getCode(),
                            zone == null ? "—" : zone.getName(),
                            zone == null ? "NORMAL" : zone.getTempType(),
                            location.getRowNo(),
                            location.getColNo(),
                            location.getCapacity(),
                            location.getStatus(),
                            location.getCurrentSku()
                    );
                })
                .toList();
    }

    public Map<String, Long> summary() {
        List<Location> locations = locationMapper.selectList(null);
        long total = locations.size();
        long occupied = locations.stream().filter(l -> "OCCUPIED".equals(l.getStatus())).count();
        long free = locations.stream().filter(l -> "FREE".equals(l.getStatus())).count();
        long locked = locations.stream().filter(l -> "LOCKED".equals(l.getStatus())).count();
        long check = locations.stream().filter(l -> "CHECK".equals(l.getStatus())).count();
        return Map.of("total", total, "occupied", occupied, "free", free, "locked", locked, "check", check);
    }

    public LocationRow updateStatus(String code, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BusinessException("不支持的库位状态：" + status);
        }
        Location location = locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                .eq(Location::getCode, code));
        if (location == null) {
            throw new BusinessException(404, "库位不存在：" + code);
        }
        location.setStatus(normalized);
        if ("FREE".equals(normalized)) {
            location.setCurrentSku(null);
        }
        locationMapper.updateById(location);
        auditService.logCurrent("LOCATION", "更新库位状态", "location:" + code, "SUCCESS", normalized);
        return list().stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow();
    }
}
