package com.lethouse.agent.controller;

import com.lethouse.agent.dto.AgentResponse;
import com.lethouse.agent.dto.AgentSaveRequest;
import com.lethouse.agent.service.AgentService;
import com.lethouse.common.result.PageResult;
import com.lethouse.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public R<Long> create(@RequestBody @Valid AgentSaveRequest req){
        Long Id = agentService.create(req);
        return R.ok(Id);
    }

    @GetMapping("/list")
    public R<PageResult<AgentResponse>> list(@RequestParam(defaultValue = "1")int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String keyword){
        PageResult<AgentResponse> list = agentService.list(pageNum, pageSize, keyword);
        return R.ok(list);
    }

    @GetMapping("/{agentId}")
    public R<AgentResponse> detail(@PathVariable Long agentId){
        AgentResponse detail=agentService.detail(agentId);
        return R.ok(detail);
    }

    @PutMapping("/{agentId}")
    public R<Void> update(@PathVariable Long agentId,@RequestBody @Valid AgentSaveRequest request){
        agentService.update(agentId,request);
        return R.ok();
    }

    @DeleteMapping("/{agentId}")
    public R<Void> delete(@PathVariable Long agentId){
        agentService.delete(agentId);
        return R.ok();
    }
}
