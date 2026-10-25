package com.lethouse.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long agentId;
    private String title;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private Integer status;
    private LocalDateTime createdAt;
    private Long deleted;
}
