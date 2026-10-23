package com.cangxu.wms.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.auth.dto.LoginRequest;
import com.cangxu.wms.auth.dto.LoginResponse;
import com.cangxu.wms.auth.entity.SysUser;
import com.cangxu.wms.auth.mapper.SysUserMapper;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(SysUserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request, String ip) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));

        boolean valid = user != null
                && "ACTIVE".equals(user.getStatus())
                && passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!valid) {
            auditService.log(request.username(), user == null ? null : user.getRoleCode(),
                    "AUTH", "登录失败", "sys_user:" + request.username(), ip,
                    "FAIL", "账号或密码错误");
            throw new BusinessException(401, "账号或密码错误");
        }

        String token = jwtService.generate(user);
        auditService.log(user.getUsername(), user.getRoleCode(),
                "AUTH", "登录系统", "sys_user:" + user.getUsername(), ip,
                "SUCCESS", "JWT 登录");
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMinutes(),
                toUserInfo(user));
    }

    public LoginResponse.UserInfo me(String username) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserInfo(user);
    }

    public void logout(String username, String ip) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        auditService.log(username, user == null ? null : user.getRoleCode(),
                "AUTH", "退出登录", "sys_user:" + username, ip, "SUCCESS", null);
    }

    private LoginResponse.UserInfo toUserInfo(SysUser user) {
        return new LoginResponse.UserInfo(user.getId(), user.getUsername(),
                user.getRealName(), user.getRoleCode());
    }
}
