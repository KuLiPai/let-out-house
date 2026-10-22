package com.lethouse.conversation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 会话服务启动类：会话、消息、上下文窗口、SSE 会话元数据。
 */
@SpringBootApplication(scanBasePackages = {"com.lethouse.conversation", "com.lethouse.common"})
public class ConversationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConversationApplication.class, args);
    }
}
