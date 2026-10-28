package com.lethouse.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("agent_config")
public class AgentRef {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer status;
    private Long deleted;
    private String systemPrompt;
    private String modelName;
    private Boolean ragEnable;
    private Integer maxToken;
    private BigDecimal temperature;
    private Integer topK;
}
