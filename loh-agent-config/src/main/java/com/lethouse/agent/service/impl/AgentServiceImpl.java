package com.lethouse.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lethouse.agent.dto.AgentResponse;
import com.lethouse.agent.dto.AgentSaveRequest;
import com.lethouse.agent.entity.AgentConfig;
import com.lethouse.agent.entity.AgentKbRel;
import com.lethouse.agent.entity.KnowledgeBase;
import com.lethouse.agent.mapper.AgentConfigMapper;
import com.lethouse.agent.mapper.AgentKbRelMapper;
import com.lethouse.agent.mapper.KnowledgeBaseMapper;
import com.lethouse.agent.service.AgentService;
import com.lethouse.common.context.TenantContext;
import com.lethouse.common.exception.BusinessException;
import com.lethouse.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {


    private final AgentConfigMapper agentConfigMapper;
    private final AgentKbRelMapper agentKbRelMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgentSaveRequest request) {
        //名称查询（唯一键是（tenantId，agent——Name，deleted）重复会抛出异常DuplicateKeyException
        Long exists = agentConfigMapper.selectCount(new LambdaQueryWrapper<AgentConfig>()
                .eq(AgentConfig::getAgentName, request.getAgentName()));
        if (exists>0) {
            throw new BusinessException(409,"Agent名称已存在");
        }
        //校验知识库归属，必须在插入agent之前，避免脏数据
        List<Long> kbIds = request.getKnowledgeBaseIds() == null ? List.of() : request.getKnowledgeBaseIds();
        validateKbIds(kbIds);
        //插入主表
        AgentConfig entity = toConfig(request);
        entity.setStatus(1);
        entity.setCreatedBy(TenantContext.getUserId());
        //字段赋值
        agentConfigMapper.insert(entity);
        for (Long kbId : kbIds) {
            AgentKbRel rel = new AgentKbRel();
            rel.setAgentId(entity.getId());
            rel.setKbId(kbId);
            agentKbRelMapper.insert(rel);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long agentId, AgentSaveRequest request) {
        AgentConfig exist = agentConfigMapper.selectById(agentId);
        if (exist==null) {
            throw new BusinessException(404,"Agent不存在");
        }
        List<Long> kbIds = request.getKnowledgeBaseIds() == null ? List.of() : request.getKnowledgeBaseIds();
        validateKbIds(kbIds);
        AgentConfig patch = toConfig(request);
        patch.setId(agentId);
        patch.setUpdatedBy(TenantContext.getUserId());
        agentConfigMapper.updateById(patch);
        agentKbRelMapper.delete(new LambdaQueryWrapper<AgentKbRel>()
                .eq(AgentKbRel::getAgentId,agentId));
        for (Long kbId : kbIds) {
            AgentKbRel rel = new AgentKbRel();
            rel.setAgentId(agentId);
            rel.setKbId(kbId);
            agentKbRelMapper.insert(rel);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long agentId) {
        AgentConfig exist = agentConfigMapper.selectById(agentId);
        if (exist==null) {
            throw new BusinessException(404,"Agent不存在");
        }
        agentKbRelMapper.delete(new LambdaQueryWrapper<AgentKbRel>().eq(AgentKbRel::getAgentId,agentId));
        agentConfigMapper.deleteById(agentId);
    }

    @Override
    public PageResult<AgentResponse> list(int pageNum, int pageSize, String keyword) {
        pageSize = Math.min(pageSize, 100);
        Page<AgentConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<AgentConfig>()
                .like(StringUtils.hasText(keyword), AgentConfig::getAgentName, keyword)
                .orderByDesc(AgentConfig::getId);
        Page<AgentConfig> agentConfigPage = agentConfigMapper.selectPage(page, wrapper);
        return PageResult.of(agentConfigPage.convert(this::toResponse));
    }

    @Override
    public AgentResponse detail(Long agentId) {
        AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
        if (agentConfig==null) {
            throw new BusinessException(404,"Agent不存在");
        }
        AgentResponse response = toResponse(agentConfig);
        List<AgentKbRel> kbrel = agentKbRelMapper.selectList(new LambdaQueryWrapper<AgentKbRel>().eq(AgentKbRel::getAgentId, agentId));
        response.setKnowledgeBaseIds(kbrel.stream().map(AgentKbRel::getKbId).toList());
        return response;
    }


    private void validateKbIds(List<Long> kbIds){
        if (kbIds.isEmpty()) {
            return ;
        }
        List<KnowledgeBase> found = knowledgeBaseMapper.selectBatchIds(kbIds);
        if (found.size()!=kbIds.size()) {
            throw new BusinessException(400,"部分知识库不存在或不属于当前租户");
        }
    }

    private AgentResponse toResponse(AgentConfig e){
        AgentResponse r = new AgentResponse();
        r.setAgentId(e.getId());
        r.setAgentName(e.getAgentName());
        r.setDescription(e.getDescription());
        r.setMaxToken(e.getMaxToken());
        r.setStatus(e.getStatus());
        r.setTemperature(e.getTemperature());
        r.setTopK(e.getTopK());
        r.setModelName(e.getModelName());
        r.setRagEnable(Integer.valueOf(1).equals(e.getRagEnable()));
        r.setSystemPrompt(e.getSystemPrompt());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private AgentConfig toConfig(AgentSaveRequest request){
        AgentConfig entity = new AgentConfig();
        entity.setDescription(request.getDescription());
        entity.setAgentName(request.getAgentName());
        entity.setMaxToken(request.getMaxToken());
        entity.setTemperature(request.getTemperature());
        entity.setModelName(request.getModelName());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setRagEnable(Boolean.FALSE.equals(request.getRagEnable())?0:1);
        entity.setTopK(request.getTopK());
        return entity;
    }
}
