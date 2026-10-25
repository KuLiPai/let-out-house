package com.lethouse.agent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentResponse {

    private Long agentId;
    private String agentName;
    private String description;
    private String systemPrompt;
    private String modelName;
    private Boolean ragEnable;
    private Integer maxToken;
    private BigDecimal temperature;
    private Integer topK;
    private Integer Status;
    private List<Long> knowledgeBaseIds;
    private LocalDateTime createdAt;
}
