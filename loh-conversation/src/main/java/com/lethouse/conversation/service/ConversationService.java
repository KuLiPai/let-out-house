package com.lethouse.conversation.service;

import com.lethouse.common.result.PageResult;
import com.lethouse.conversation.dto.ConversationCreateRequest;
import com.lethouse.conversation.dto.ConversationResponse;
import com.lethouse.conversation.dto.MessageResponse;

public interface ConversationService {
    Long create(ConversationCreateRequest request);

    PageResult<ConversationResponse> list(int pageNum,int pageSize);

    PageResult<MessageResponse> history(Long conversationId,int pageNum,int pageSize);

    void delete(Long conversationId);
}
