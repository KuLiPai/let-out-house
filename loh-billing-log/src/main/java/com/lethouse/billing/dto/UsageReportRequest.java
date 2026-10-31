package com.lethouse.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsageReportRequest {
    @NotNull
    private Long messageId;
    private Long agentId;
    private Long conversationId;
    private Long userId;
    @NotBlank
    private String modelName;
    @NotNull @Min(0) private Integer inputTokens;
    @NotNull @Min(0) private Integer outputTokens;
    private String requestId;
}
