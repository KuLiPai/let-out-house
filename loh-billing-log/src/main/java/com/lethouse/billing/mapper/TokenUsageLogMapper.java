package com.lethouse.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lethouse.billing.dto.UsageSummaryResponse;
import com.lethouse.billing.entity.TokenUsageLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TokenUsageLogMapper extends BaseMapper<TokenUsageLog> {

    @Select("SELECT COALESCE(SUM(input_tokens),0) AS totalInputTokens, " +
            "COALESCE(SUM(output_tokens),0) AS totalOutputTokens, " +
            "COALESCE(SUM(total_cost),0) AS totalCost, COUNT(*) AS callCount " +
            "FROM token_usage_log WHERE tenant_id = #{tenantId} " +
            "AND (#{from} IS NULL OR created_at >= #{from}) " +
            "AND (#{to} IS NULL OR created_at < #{to})")
    UsageSummaryResponse summarize(@Param("tenantId")Long tenantId,@Param("from") String from,@Param("to")String to);
}
