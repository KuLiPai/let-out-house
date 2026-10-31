package com.lethouse.billing.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UsageSummaryResponse {
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private BigDecimal totalCost;
    private Long callCount;
}
