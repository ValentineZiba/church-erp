package com.churchos.church_erp.security.tenantuser.dto;

public record TenantLoginResponse(
    String accessToken,
    String tokenType,
    long expiresInMinutes,
    TenantUserSummary user
) {
}
