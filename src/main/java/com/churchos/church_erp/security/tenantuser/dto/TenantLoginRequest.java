package com.churchos.church_erp.security.tenantuser.dto;

import jakarta.validation.constraints.NotBlank;

public record TenantLoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {
}
