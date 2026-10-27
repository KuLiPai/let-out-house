package com.lethouse.conversation.service;

import com.lethouse.conversation.dto.ChatStreamRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ChatStreamService {
    void stream(Long conversationId, ChatStreamRequest request, HttpServletResponse response);
}
