package com.lethouse.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConversationCreateRequest {
    @NotNull(message = "Agent不能为空")
    private Long agentId;
    @Size(max = 200,message = "标题不能超过200字")
    private String title;
}
