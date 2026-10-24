package com.lethouse.user.controller;

import com.lethouse.common.context.TenantContext;
import com.lethouse.common.exception.BusinessException;
import com.lethouse.common.result.R;
import com.lethouse.user.dto.UserInfoResponse;
import com.lethouse.user.entity.SysUser;
import com.lethouse.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final SysUserMapper sysUserMapper;

    @GetMapping("/info")
    public R<UserInfoResponse> info(){
        Long userId = TenantContext.getUserId();
        if (userId==null) {
            throw new BusinessException(401,"用户身份缺失");
        }
        Long tenantId = TenantContext.getTenantId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user==null) {
            throw new BusinessException(401,"用户不存在或已被禁用");
        }
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setUserId(userId);
        userInfoResponse.setTenantId(tenantId);
        userInfoResponse.setUsername(user.getUsername());
        userInfoResponse.setNickname(user.getRealName());
        return R.ok(userInfoResponse);
    }
}
