package com.lethouse.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("agent_config")
public class AgentConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String agentName;
    private String description;
    private String systemPrompt;
    private String modelName;
    private Integer ragEnable;
    private Integer maxToken;
    private Integer topK;
    private Integer status;
    private BigDecimal temperature;
    private Long deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
}
