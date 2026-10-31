package com.lethouse.billing.service;

import com.lethouse.billing.dto.UsageReportRequest;

public interface BillingService {
    void report(UsageReportRequest req,Long tenantId);
}
