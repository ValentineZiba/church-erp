package com.churchos.church_erp.tenant.provisioning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TenantProvisioningRequest(
    @NotBlank String name,
    @NotBlank @Pattern(
        regexp = TenantProvisioningService.SLUG_PATTERN,
        message = "slug must be lowercase, start with a letter, and contain only letters, digits, and hyphens"
    ) String slug,
    String subdomain
) {

    public String resolvedSubdomain() {
        return (subdomain == null || subdomain.isBlank()) ? slug : subdomain;
    }
}
