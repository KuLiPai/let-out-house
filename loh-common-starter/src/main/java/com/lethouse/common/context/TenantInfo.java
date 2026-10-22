package com.lethouse.common.context;

public record TenantInfo(Long tenantId,Long userId,String requestId) {
}
