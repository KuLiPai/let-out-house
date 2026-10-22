package com.lethouse.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 配置服务启动类：Agent 配置、知识库配置、模型参数、工具注册。
 */
@SpringBootApplication(scanBasePackages = {"com.lethouse.agent", "com.lethouse.common"})
public class AgentConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentConfigApplication.class, args);
    }
}
