package com.lethouse.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.lethouse.common.context.TenantContext;
import com.lethouse.common.exception.TenantMissingException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;


public class CustomTenantLineHandler implements TenantLineHandler {

    private static final Set<String> IGNORE_TABLES = Set.of(
            "tenant",
            "sys_permission"
    );
    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId==null) {
            throw new TenantMissingException("当前请求租户标识缺失，禁止访问");
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        String lowerName = tableName.toLowerCase();
        return IGNORE_TABLES.contains(lowerName);
    }
}
