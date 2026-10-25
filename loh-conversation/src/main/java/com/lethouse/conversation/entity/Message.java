package com.lethouse.conversation.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private String content;
    private String role;
    private Integer tokenInput;
    private Integer tokenOutput;
    private Integer msgStatus;
    private String traceId;
    private LocalDateTime createdAt;
}
