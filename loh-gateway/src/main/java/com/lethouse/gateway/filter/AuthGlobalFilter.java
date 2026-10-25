package com.lethouse.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lethouse.common.result.R;
import com.lethouse.common.security.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {


    private static final Logger log= LoggerFactory.getLogger(AuthGlobalFilter.class);
    private static final String HEADER_AUTH="Authorization";
    private static final String BEARER="Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // CORS 预检请求（OPTIONS）不带 Authorization，必须提前放行，
        // 否则浏览器/WebView 拿不到 Access-Control-Allow-* 响应头，跨域直接失败
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        //白名单放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }
        //取token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HEADER_AUTH);
        if (authHeader==null||!authHeader.regionMatches(true,0,BEARER,0,BEARER.length())) {
            return unauthorized(exchange,"缺少认证凭证");
        }
        String token = authHeader.substring(BEARER.length());
        //验签
        Claims claims = jwtUtils.parseToken(token);
        if (claims==null) {
            return unauthorized(exchange,"Token 无效或已过期");
        }

        Long userId = jwtUtils.getUserId(claims);
        Long tenantId = jwtUtils.getTenantId(claims);
        //覆盖写header
        ServerHttpRequest mutated = exchange.getRequest().mutate().headers(h -> {
            h.set("X-User-Id", String.valueOf(userId));
            h.set("X-Tenant-Id", String.valueOf(tenantId));
            if (h.getFirst("X-Request-Id") == null) {
                h.set("X-Request-Id", UUID.randomUUID().toString().replace("-", ""));
            }
            if (h.getFirst("X-Trace-Id") == null) {
                h.set("X-Trace-Id", UUID.randomUUID().toString().replace("-", ""));
            }
        }).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhiteList(String path){
        return path.equals("/api/auth/login")||path.startsWith("/actuator");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange,String msg){
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        R<Void> body = R.fail(401, msg);
        if (traceId!=null) {
            body.setTraceId(traceId);
        }
        byte[] bytes;
        try{

            bytes=new ObjectMapper().writeValueAsBytes(body);
        }catch (JsonProcessingException e){
            bytes="{\"code\":401,\"msg\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
