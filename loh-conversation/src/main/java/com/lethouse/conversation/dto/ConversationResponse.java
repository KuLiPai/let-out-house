package com.lethouse.conversation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationResponse {
    private Long conversationId;
    private Long agentId;
    private String title;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
