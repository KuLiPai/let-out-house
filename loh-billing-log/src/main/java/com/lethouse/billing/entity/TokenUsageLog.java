package com.lethouse.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("token_usage_log")
public class TokenUsageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long agentId;
    private Long conversationId;
    private Long messageId;
    private String modelName;
    private Integer inputToken;
    private Integer outputToken;
    private BigDecimal totalCost;
    private BigDecimal inputCost;
    private BigDecimal outputCost;
    private String requestId;
    private LocalDateTime createdAt;
}
