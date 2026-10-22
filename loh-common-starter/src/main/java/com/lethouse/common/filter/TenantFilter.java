package com.lethouse.common.filter;

import com.lethouse.common.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;
import java.io.IOException;
import java.util.UUID;

public class TenantFilter extends OncePerRequestFilter {

    private static final String HEADER_TENANT_ID="X-Tenant-Id";
    private static final String HEADER_USER_ID="X-User-Id";
    private static final String HEADER_REQUEST_ID="X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            Long tenantId = parseLong(request.getHeader(HEADER_TENANT_ID));
            Long userId = parseLong(request.getHeader(HEADER_USER_ID));
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            String traceId=request.getHeader("X-Trace-Id");
            if(traceId==null||traceId.isBlank()){
                traceId=UUID.randomUUID().toString().replace("-", "");
            }
            MDC.put("traceId",traceId);
            TenantContext.set(tenantId,userId,requestId);
            filterChain.doFilter(request,response);
        }finally {
            //用户
            MDC.remove("traceId");
            TenantContext.clear();
        }
    }

    private Long parseLong(String val){
        if(val==null||val.isBlank()){
            return null;
        }
        try {
            return Long.parseLong(val);
        }catch (NumberFormatException e){
            return null;
        }
    }
}
