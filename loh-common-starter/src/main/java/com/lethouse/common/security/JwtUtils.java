package com.lethouse.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private final SecretKey key;          // 构造时算一次，别在方法里反复算
    private final long expireMillis;

    public JwtUtils(@Value("${loh.jwt.secret:}") String secret,
                    @Value("${loh.jwt.expire-minutes:120}") long expireMinutes) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length<32) {
            throw  new IllegalStateException("loh.jwt.secret未配置或不足32字节（当前"+keyBytes.length+"），HS256要求至少256位");
        }
        this.key=Keys.hmacShaKeyFor(keyBytes);
        this.expireMillis=expireMinutes*60_000L;
    }

    public String generateToken(Long userId,Long tenantId,String tenantCode,String username){
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenantId",tenantId)
                .claim("tenantCode",tenantCode)
                .claim("username",username)
                .issuedAt(now)
                .expiration(new Date(now.getTime()+expireMillis))
                .signWith(key,Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseToken(String token){
        if (token==null||token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)//验证签名+校验过期
                    .getPayload();
        }catch (ExpiredJwtException e){
            //Token过期，正常业务日志，不要打error日志
            return null;
        }
        catch (JwtException|IllegalArgumentException e){
            //签名错误，格式非法，内容被篡改
            return null;
        }
    }

    public Long getUserId(Claims claims){
        return Long.valueOf(claims.getSubject());
    }

    public Long getTenantId(Claims claims){
        return claims.get("tenantId",Number.class).longValue();
    }

    public boolean isValid(String token){
        return parseToken(token)!=null;
    }
}
