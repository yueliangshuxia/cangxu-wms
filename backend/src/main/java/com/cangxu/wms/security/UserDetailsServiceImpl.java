package com.cangxu.wms.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cangxu.wms.auth.entity.SysUser;
import com.cangxu.wms.auth.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;

    public UserDetailsServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRoleCode())
                .disabled(!"ACTIVE".equals(user.getStatus()))
                .build();
    }
}
