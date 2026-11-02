package com.lethouse.task.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface UsageReconcileMapper {

    /**
     * 补录：一条 SQL 完成「查缺口 → 关联模型单价 → 算金额 → 插入」。
     *
     * 幂等三重保障：
     *   1. LEFT JOIN token_usage_log u ... u.id IS NULL —— 只捞没流水的
     *   2. ON DUPLICATE KEY UPDATE —— 并发下唯一键兜底，不报错
     *   3. ORDER BY m.id LIMIT —— 分批推进，补过的下次不会再被选中
     *
     * 注意 ON DUPLICATE 里的列必须用表名限定，因为上面的 SELECT 也 join 了
     * token_usage_log u，不限定会报 Column 'id' in field list is ambiguous。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO token_usage_log "
            + "  (tenant_id,user_id,agent_id,conversation_id,message_id,model_name, "
            + "   input_tokens,output_tokens,input_cost,output_cost,total_cost,request_id) "
            + "SELECT m.tenant_id, COALESCE(c.user_id,0), COALESCE(c.agent_id,0), m.conversation_id, m.id, "
            + "       COALESCE(a.model_name,'unknown'), "
            + "       COALESCE(m.token_input,0), COALESCE(m.token_output,0), "
            + "       ROUND(COALESCE(m.token_input,0)/1000000*COALESCE(p.input_price,0),6), "
            + "       ROUND(COALESCE(m.token_output,0)/1000000*COALESCE(p.output_price,0),6), "
            + "       ROUND(COALESCE(m.token_input,0)/1000000*COALESCE(p.input_price,0) "
            + "           + COALESCE(m.token_output,0)/1000000*COALESCE(p.output_price,0),6), "
            + "       m.trace_id "
            + "FROM message m "
            + "JOIN conversation c ON c.id = m.conversation_id "
            + "LEFT JOIN agent_config a ON a.id = c.agent_id "
            + "LEFT JOIN model_price p ON p.model_name = a.model_name AND p.status = 1 "
            + "LEFT JOIN token_usage_log u ON u.tenant_id = m.tenant_id AND u.message_id = m.id "
            + "WHERE m.role='assistant' AND m.msg_status=1 "
            + "  AND m.created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) "
            + "  AND (COALESCE(m.token_input,0)>0 OR COALESCE(m.token_output,0)>0) "
            + "  AND u.id IS NULL "
            + "ORDER BY m.id LIMIT #{limit} "
            + "ON DUPLICATE KEY UPDATE token_usage_log.total_cost = token_usage_log.total_cost")
    int backfill(@Param("days") int days, @Param("limit") int limit);
}
