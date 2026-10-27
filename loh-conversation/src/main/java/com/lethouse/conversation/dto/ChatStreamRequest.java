package com.lethouse.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatStreamRequest {
    @NotBlank(message = "问题不能为空")
    @Size(max = 4000,message = "问题不能超过4000字")
    private String query;
    private Boolean stream;
}
