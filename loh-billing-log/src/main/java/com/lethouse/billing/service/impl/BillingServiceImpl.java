package com.lethouse.billing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lethouse.billing.dto.UsageReportRequest;
import com.lethouse.billing.dto.UsageSummaryResponse;
import com.lethouse.billing.entity.ModelPrice;
import com.lethouse.billing.entity.TokenUsageLog;
import com.lethouse.billing.mapper.ModelPriceMapper;
import com.lethouse.billing.mapper.TokenUsageLogMapper;
import com.lethouse.billing.service.BillingService;
import com.lethouse.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final ModelPriceMapper modelPriceMapper;
    private final TokenUsageLogMapper tokenUsageLogMapper;


    @Override
    public void report(UsageReportRequest req, Long tenantId) {
        //幂等前置检查，神雕大部分重复插入的异常开销
        Long exists = tokenUsageLogMapper.selectCount(new LambdaQueryWrapper<TokenUsageLog>()
                .eq(TokenUsageLog::getTenantId, tenantId)
                .eq(TokenUsageLog::getMessageId, req.getMessageId()));
        if (exists>0) {
            log.debug("用量已记录，跳过messageId={}",req.getMessageId());
            return ;
        }
        //查单价，查不到按0记录
        ModelPrice price = modelPriceMapper.selectOne(new LambdaQueryWrapper<ModelPrice>()
                .eq(ModelPrice::getModelName, req.getModelName())
                .eq(ModelPrice::getStatus, 1)
                .last("limit 1"));
        BigDecimal inPrice = price == null ? BigDecimal.ZERO : price.getInputPrice();
        BigDecimal outPrice = price == null ? BigDecimal.ZERO : price.getOutputPrice();

        //计算价格
        BigDecimal million = BigDecimal.valueOf(1_000_000);
        BigDecimal inCost = BigDecimal.valueOf(req.getInputTokens()).divide(million,6, RoundingMode.HALF_UP).multiply(inPrice);
        BigDecimal outCost = BigDecimal.valueOf(req.getOutputTokens()).divide(million,6,RoundingMode.HALF_UP).multiply(outPrice);

        TokenUsageLog log0 = new TokenUsageLog();
        log0.setTenantId(tenantId);
        log0.setMessageId(req.getMessageId());
        log0.setConversationId(req.getConversationId());
        log0.setAgentId(req.getAgentId());
        log0.setUserId(req.getUserId());
        log0.setModelName(req.getModelName());
        log0.setInputTokens(req.getInputTokens());
        log0.setOutputTokens(req.getOutputTokens());
        log0.setInputCost(inCost);
        log0.setOutputCost(outCost);
        log0.setTotalCost(inCost.add(outCost).setScale(6,RoundingMode.HALF_UP));
        log0.setRequestId(req.getRequestId());
        try {
            tokenUsageLogMapper.insert(log0);
        }catch (DuplicateKeyException e ){
            log.debug("并发重复上报，已忽略：messageId={}",req.getMessageId());
        }

    }

    @Override
    public UsageSummaryResponse summary(Long tenantId, String from, String to) {
        // 用哨兵值代替 null：JDBC 对 null 参数无法推断类型，容易报参数类型错误
        String fromTs = (from == null || from.isBlank()) ? "1970-01-01 00:00:00" : from;
        String toTs = (to == null || to.isBlank()) ? "9999-12-31 23:59:59" : to;
        UsageSummaryResponse resp = tokenUsageLogMapper.summarize(tenantId, fromTs, toTs);
        if (resp == null) {
            resp = new UsageSummaryResponse();
            resp.setTotalInputTokens(0L);
            resp.setTotalOutputTokens(0L);
            resp.setTotalCost(BigDecimal.ZERO);
            resp.setCallCount(0L);
        }
        return resp;
    }

    @Override
    public PageResult<TokenUsageLog> detail(Long tenantId, int pageNum, int pageSize) {
        pageSize = Math.min(pageSize, 100);
        Page<TokenUsageLog> page = new Page<>(pageNum, pageSize);
        Page<TokenUsageLog> result = tokenUsageLogMapper.selectPage(page,
                new LambdaQueryWrapper<TokenUsageLog>()
                        .eq(TokenUsageLog::getTenantId, tenantId)
                        .orderByDesc(TokenUsageLog::getId));
        return PageResult.of(result);
    }
}
