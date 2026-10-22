package com.lethouse.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关启动类。
 *
 * 注意：本模块是 WebFlux 技术栈，
 * 1. 不要加 @MapperScan（网关不碰数据库）
 * 2. 不要在 pom 里引入 spring-boot-starter-web，否则 MVC 会顶掉 WebFlux，路由全部 404
 */
@SpringBootApplication(scanBasePackages = {"com.lethouse.gateway", "com.lethouse.common"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
