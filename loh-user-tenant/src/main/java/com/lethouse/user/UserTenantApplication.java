package com.lethouse.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 租户服务启动类：租户、用户、角色、权限（RBAC）、认证与令牌签发。
 *
 * scanBasePackages 必须同时写业务包和公共包：
 * 只写 @SpringBootApplication 时，默认只扫本类所在包及子包，
 * com.lethouse.common 下的租户拦截器、全局异常处理器会全部扫不到。
 */
@SpringBootApplication(scanBasePackages = {"com.lethouse.user", "com.lethouse.common"})
@MapperScan("com.lethouse.user.mapper")
public class UserTenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserTenantApplication.class, args);
    }
}
