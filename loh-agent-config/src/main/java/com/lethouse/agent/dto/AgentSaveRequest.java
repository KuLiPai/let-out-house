package com.lethouse.agent.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AgentSaveRequest {
    @NotBlank(message = "Agent名称不能为空")
    @Size(max = 128)
    private String agentName;

    @Size(max=512)
    private String description;

    @NotBlank(message = "系统提示词不能为空")
    private String systemPrompt;

    @NotBlank(message = "模型不能为空")
    private String modelName;

    private Boolean ragEnable;

    @Min(1)@Max(32000)
    private Integer maxToken;

    @DecimalMin("0.00") @DecimalMax("2.00")
    private BigDecimal temperature;

    @Min(1)@Max(20)
    private Integer topK;

    private List<Long> knowledgeBaseIds;
}
