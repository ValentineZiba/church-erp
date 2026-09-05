package com.churchos.church_erp.tenant.provisioning;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TenantProvisioningRequest(
    @NotBlank String name,
    @NotBlank @Pattern(
        regexp = TenantProvisioningService.SLUG_PATTERN,
        message = "slug must be lowercase, start with a letter, and contain only letters, digits, and hyphens"
    ) String slug,
    String subdomain,
    @NotBlank @Email String adminEmail,
    @NotBlank String adminFullName,
    @NotBlank String adminPassword
) {

    public String resolvedSubdomain() {
        return (subdomain == null || subdomain.isBlank()) ? slug : subdomain;
    }
}
