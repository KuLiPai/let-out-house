package com.lethouse.conversation.controller;

import com.lethouse.common.result.PageResult;
import com.lethouse.common.result.R;
import com.lethouse.conversation.dto.ConversationCreateRequest;
import com.lethouse.conversation.dto.ConversationResponse;
import com.lethouse.conversation.dto.MessageResponse;
import com.lethouse.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public R<Long> create(@RequestBody @Valid ConversationCreateRequest request){
        return R.ok(conversationService.create(request));
    }

    @GetMapping("/list")
    public R<PageResult<ConversationResponse>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize){
        return R.ok(conversationService.list(pageNum,pageSize));
    }

    @GetMapping("/{conversationId}/history")
    public R<PageResult<MessageResponse>> history(@PathVariable Long conversationId,@RequestParam(defaultValue = "1")int pageNum,@RequestParam(defaultValue = "10") int pageSize){
        return R.ok(conversationService.history(conversationId,pageNum,pageSize));
    }

    @DeleteMapping("/{conversationId}")
    public R<Void> delete(@PathVariable Long conversationId){
        conversationService.delete(conversationId);
        return R.ok();
    }
}
