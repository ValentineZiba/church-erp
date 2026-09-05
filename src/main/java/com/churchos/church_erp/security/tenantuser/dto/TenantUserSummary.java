package com.churchos.church_erp.security.tenantuser.dto;

import com.churchos.church_erp.security.tenantuser.domain.TenantUserRole;

public record TenantUserSummary(
    Long id,
    String email,
    String fullName,
    TenantUserRole role
) {
}
