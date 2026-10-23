package com.cangxu.wms.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.dto.AuditStats;
import com.cangxu.wms.audit.entity.AuditLog;
import com.cangxu.wms.audit.mapper.AuditLogMapper;
import com.cangxu.wms.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void log(String username, String roleCode, String module, String action,
                    String target, String ip, String result, String detail) {
        AuditLog log = AuditLog.builder()
                .username(username)
                .roleCode(roleCode)
                .module(module)
                .action(action)
                .target(target)
                .ip(ip)
                .result(result)
                .detail(detail)
                .build();
        auditLogMapper.insert(log);
    }

    public void logCurrent(String module, String action, String target, String result, String detail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth == null ? "system" : auth.getName();
        String role = auth == null ? null : auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
        String ip = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            ip = request.getRemoteAddr();
        }
        log(username, role, module, action, target, ip, result, detail);
    }

    public List<AuditLog> recent(int limit) {
        Page<AuditLog> pageRequest = new Page<>(1, Math.max(1, limit));
        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<>();
        query.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(pageRequest, query).getRecords();
    }

    public AuditStats stats() {
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getCreatedAt));
        long loginCount = logs.stream()
                .filter(log -> "AUTH".equals(log.getModule())
                        && ("登录系统".equals(log.getAction()) || "登录失败".equals(log.getAction())))
                .count();
        long loginSuccess = logs.stream()
                .filter(log -> "AUTH".equals(log.getModule()) && "SUCCESS".equals(log.getResult()))
                .count();
        long loginFail = logs.stream()
                .filter(log -> "AUTH".equals(log.getModule()) && "FAIL".equals(log.getResult()))
                .count();
        long failureCount = logs.stream().filter(log -> "FAIL".equals(log.getResult())).count();
        List<AuditLog> sensitiveLogs = logs.stream().filter(this::isSensitive).toList();
        DateTimeFormatter time = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        List<AuditStats.SensitiveItem> recent = sensitiveLogs.stream()
                .limit(4)
                .map(log -> new AuditStats.SensitiveItem(
                        log.getAction(),
                        (log.getUsername() == null ? "—" : log.getUsername()) + " · "
                                + (log.getDetail() == null ? (log.getTarget() == null ? "—" : log.getTarget()) : log.getDetail()),
                        log.getCreatedAt() == null ? "—" : log.getCreatedAt().format(time)))
                .toList();
        return new AuditStats(logs.size(), loginCount, loginSuccess, loginFail,
                sensitiveLogs.size(), failureCount, recent);
    }

    private boolean isSensitive(AuditLog log) {
        String action = log.getAction() == null ? "" : log.getAction();
        return action.contains("导出") || action.contains("删除") || action.contains("重置")
                || action.contains("权限") || action.contains("用户") || action.contains("库位")
                || action.contains("库存调整");
    }

    public PageResult<AuditLog> page(long page, long size, String module, String result, String keyword) {
        Page<AuditLog> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
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
        Page<AuditLog> resultPage = auditLogMapper.selectPage(pageRequest, query);
        return new PageResult<>(resultPage.getRecords(), resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }
}
