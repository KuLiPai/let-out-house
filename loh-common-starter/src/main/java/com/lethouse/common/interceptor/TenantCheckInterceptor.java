package com.lethouse.common.interceptor;

import com.lethouse.common.context.TenantContext;
import com.lethouse.common.exception.TenantMissingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class TenantCheckInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId==null) {
            throw new TenantMissingException("tenantId不能为空，拒绝访问");
        }
        return true;
    }
}
