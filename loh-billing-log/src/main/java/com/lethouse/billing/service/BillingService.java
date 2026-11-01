package com.lethouse.billing.service;

import com.lethouse.billing.dto.UsageReportRequest;
import com.lethouse.billing.dto.UsageSummaryResponse;
import com.lethouse.billing.entity.TokenUsageLog;
import com.lethouse.common.result.PageResult;

public interface BillingService {
    void report(UsageReportRequest req,Long tenantId);

    UsageSummaryResponse summary(Long tenantId, String from, String to);

    PageResult<TokenUsageLog> detail(Long tenantId, int pageNum, int pageSize);
}
