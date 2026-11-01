package com.lethouse.billing.controller;

import com.lethouse.billing.dto.UsageReportRequest;
import com.lethouse.billing.dto.UsageSummaryResponse;
import com.lethouse.billing.entity.TokenUsageLog;
import com.lethouse.billing.service.BillingService;
import com.lethouse.common.result.PageResult;
import com.lethouse.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/usage")
    public R<Void> report(@RequestBody @Valid UsageReportRequest req, @RequestHeader("X-Tenant-Id") Long tenantId){
        billingService.report(req,tenantId);
        return R.ok();
    }

    @GetMapping("/summary")
    public R<UsageSummaryResponse> summary(@RequestParam(required = false)String from,
                                           @RequestParam(required = false)String to,
                                           @RequestHeader("X-Tenant-Id")Long tenantId){
        return R.ok(billingService.summary(tenantId, from, to));
    }

    @GetMapping("/detail")
    public R<PageResult<TokenUsageLog>> detail(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestHeader("X-Tenant-Id") Long tenantId){
        return R.ok(billingService.detail(tenantId, pageNum, pageSize));
    }
}
