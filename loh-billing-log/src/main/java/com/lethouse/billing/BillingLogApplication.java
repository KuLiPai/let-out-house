package com.lethouse.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 计费审计服务启动类：Token 计费、调用流水、操作审计日志。
 */
@SpringBootApplication(scanBasePackages = {"com.lethouse.billing", "com.lethouse.common"})
public class BillingLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingLogApplication.class, args);
    }
}
