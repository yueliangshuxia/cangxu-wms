package com.cangxu.wms.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.auth.dto.PasswordResetRequest;
import com.cangxu.wms.auth.dto.UserRequest;
import com.cangxu.wms.auth.dto.UserView;
import com.cangxu.wms.auth.entity.SysUser;
import com.cangxu.wms.auth.mapper.SysUserMapper;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ROLES = Set.of("ADMIN", "MANAGER", "RECEIVER", "INSPECTOR");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public PageResult<UserView> page(long page, long size, String keyword, String role, String status) {
        Page<SysUser> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        if (StringUtils.hasText(role)) {
            query.eq(SysUser::getRoleCode, role);
        }
        if (StringUtils.hasText(status)) {
            query.eq(SysUser::getStatus, status);
        }
        query.orderByAsc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(pageRequest, query);
        return new PageResult<>(result.getRecords().stream().map(UserView::from).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<Map<String, String>> roles() {
        return List.of(
                role("ADMIN", "系统管理员"),
                role("MANAGER", "仓储主管"),
                role("RECEIVER", "收货员"),
                role("INSPECTOR", "质检员")
        );
    }

    @Transactional
    public UserView create(UserRequest request) {
        String username = request.username().trim();
        String role = normalizeRole(request.roleCode());
        if (!StringUtils.hasText(request.password())) {
            throw new BusinessException("新增用户必须设置密码");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessException("账号已存在：" + username);
        }
        ensureCanManageRole(role);
        SysUser user = SysUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .realName(request.realName().trim())
                .roleCode(role)
                .status(normalizeStatus(request.status()))
                .createdAt(LocalDateTime.now())
                .build();
        userMapper.insert(user);
        auditService.logCurrent("SYSTEM", "新增用户", "sys_user:" + username, "SUCCESS", role);
        return UserView.from(user);
    }

    @Transactional
    public UserView update(Long id, UserRequest request) {
        SysUser user = requireUser(id);
        String role = normalizeRole(request.roleCode());
        String status = normalizeStatus(request.status());
        ensureCanManageRole(role);
        ensureCanManageRole(user.getRoleCode());
        ensureLastAdminSafe(user, role, status);

        user.setRealName(request.realName().trim());
        user.setRoleCode(role);
        user.setStatus(status);
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        userMapper.updateById(user);
        auditService.logCurrent("SYSTEM", "编辑用户", "sys_user:" + user.getUsername(), "SUCCESS",
                role + " / " + status);
        return UserView.from(user);
    }

    @Transactional
    public UserView resetPassword(Long id, PasswordResetRequest request) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userMapper.updateById(user);
        auditService.logCurrent("SYSTEM", "重置用户密码", "sys_user:" + user.getUsername(), "SUCCESS", null);
        return UserView.from(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = requireUser(id);
        if (user.getUsername().equals(currentUsername())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        ensureCanManageRole(user.getRoleCode());
        ensureLastAdminSafe(user, null, "DISABLED");
        userMapper.deleteById(id);
        auditService.logCurrent("SYSTEM", "删除用户", "sys_user:" + user.getUsername(), "SUCCESS", null);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (!ROLES.contains(normalized)) {
            throw new BusinessException("不支持的角色：" + role);
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase() : "ACTIVE";
        if (!STATUSES.contains(normalized)) {
            throw new BusinessException("不支持的账号状态：" + status);
        }
        return normalized;
    }

    private void ensureCanManageRole(String role) {
        if ("ADMIN".equals(role) && !isCurrentAdmin()) {
            throw new BusinessException(403, "只有系统管理员可以管理管理员账号");
        }
    }

    private void ensureLastAdminSafe(SysUser target, String newRole, String newStatus) {
        if (!"ADMIN".equals(target.getRoleCode()) || !"ACTIVE".equals(target.getStatus())) {
            return;
        }
        boolean staysAdmin = "ADMIN".equals(newRole) && "ACTIVE".equals(newStatus);
        if (staysAdmin) {
            return;
        }
        long others = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, "ADMIN")
                .eq(SysUser::getStatus, "ACTIVE")
                .ne(SysUser::getId, target.getId()));
        if (others == 0) {
            throw new BusinessException("至少需要保留一个启用的管理员账号");
        }
    }

    private boolean isCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private Map<String, String> role(String code, String label) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        return item;
    }
}
