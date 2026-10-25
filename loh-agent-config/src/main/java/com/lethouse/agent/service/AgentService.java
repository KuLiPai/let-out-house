package com.lethouse.agent.service;

import com.lethouse.agent.dto.AgentResponse;
import com.lethouse.agent.dto.AgentSaveRequest;
import com.lethouse.common.result.PageResult;

public interface AgentService {

    public Long create(AgentSaveRequest request);

    void update(Long agentId,AgentSaveRequest request);

    void delete(Long agentId);

    PageResult<AgentResponse> list(int pageNum,int pageSize,String keyword);


    AgentResponse detail(Long agentId);
}
