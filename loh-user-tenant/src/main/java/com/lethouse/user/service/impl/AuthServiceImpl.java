package com.lethouse.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lethouse.common.context.TenantContext;
import com.lethouse.common.exception.BusinessException;
import com.lethouse.common.security.JwtUtils;
import com.lethouse.user.dto.LoginRequest;
import com.lethouse.user.dto.LoginResponse;
import com.lethouse.user.entity.SysUser;
import com.lethouse.user.entity.Tenant;
import com.lethouse.user.mapper.SysUserMapper;
import com.lethouse.user.mapper.TenantMapper;
import com.lethouse.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(LoginRequest req) {
        // ① 查租户（tenant 表在 IGNORE_TABLES，不拼 tenant_id)
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantCode, req.getTenantCode()));
        // ② 租户校验：不存在 / 停用 / 过期
        if (tenant==null||tenant.getStatus()==null||tenant.getStatus()!=1) {
            throw new BusinessException(401,"租户不存在或已停用");
        }
        // ③ 塞租户上下文 —— 下一步查 sys_user
        if (tenant.getExpireAt()!=null&&tenant.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401,"租户已过期，请联系管理员");
        }
        TenantContext.set(tenant.getId(),null,TenantContext.getRequestId());
        // ④ 查用户（拦截器自动拼 tenant_id）
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername())
        );
        if (user==null||!passwordEncoder.matches(req.getPassword(),user.getPasswordHash())) {
            throw new BusinessException(401,"用户名或密码错误");
        }

        if (user.getStatus()==null||user.getStatus()!=1) {
            throw new BusinessException(401,"账号已被停用");
        }
        SysUser patch = new SysUser();
        patch.setId(user.getId());
        patch.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(patch);

        String token = jwtUtils.generateToken(user.getId(), tenant.getId(), tenant.getTenantCode(), user.getUsername());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setTenantId(tenant.getId());
        response.setTenantCode(tenant.getTenantCode());
        response.setUsername(user.getUsername());
        response.setNickname(user.getRealName());
        response.setRoleList(List.of());
        return response;
    }
}
