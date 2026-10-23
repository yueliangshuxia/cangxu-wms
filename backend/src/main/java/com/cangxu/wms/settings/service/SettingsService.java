package com.cangxu.wms.settings.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.settings.entity.SystemSetting;
import com.cangxu.wms.settings.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettingsService {

    private final SystemSettingMapper settingMapper;
    private final AuditService auditService;

    public SettingsService(SystemSettingMapper settingMapper, AuditService auditService) {
        this.settingMapper = settingMapper;
        this.auditService = auditService;
    }

    public Map<String, String> getAll() {
        Map<String, String> result = new LinkedHashMap<>();
        settingMapper.selectList(new LambdaQueryWrapper<SystemSetting>()
                        .orderByAsc(SystemSetting::getGroupName)
                        .orderByAsc(SystemSetting::getSettingKey))
                .forEach(item -> result.put(item.getSettingKey(), item.getSettingValue()));
        return result;
    }

    @Transactional
    public Map<String, String> save(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return getAll();
        }
        values.forEach((key, value) -> {
            SystemSetting existing = settingMapper.selectById(key);
            if (existing == null) {
                settingMapper.insert(SystemSetting.builder()
                        .settingKey(key)
                        .settingValue(value)
                        .groupName(groupOf(key))
                        .build());
            } else {
                existing.setSettingValue(value);
                settingMapper.updateById(existing);
            }
        });
        auditService.logCurrent("SYSTEM", "保存系统设置", "settings", "SUCCESS",
                values.size() + " 项");
        return getAll();
    }

    private String groupOf(String key) {
        int dot = key.indexOf('.');
        return dot > 0 ? key.substring(0, dot) : "general";
    }
}
