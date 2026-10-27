package com.lethouse.conversation.controller;

import com.lethouse.conversation.dto.ChatStreamRequest;
import com.lethouse.conversation.service.ChatStreamService;
import com.lethouse.conversation.service.impl.ChatStreamServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversation")
public class ChatStreamController {

    private final ChatStreamService chatStreamService;

    @PostMapping(value = "/{conversationId}/stream")
    public void stream(@PathVariable Long conversationId, @RequestBody @Valid ChatStreamRequest request, HttpServletResponse response){
        chatStreamService.stream(conversationId,request,response);
    }
}
