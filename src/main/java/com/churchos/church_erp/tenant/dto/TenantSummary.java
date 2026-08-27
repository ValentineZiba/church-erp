package com.churchos.church_erp.tenant.dto;

import com.churchos.church_erp.tenant.domain.TenantStatus;
import java.time.Instant;

/**
 * Deliberately excludes {@code dbHost}/{@code dbPort}/{@code dbName}/{@code dbUsername}/
 * {@code dbPassword} — those are tenant database credentials and must never leave the
 * control-plane process in an API response.
 */
public record TenantSummary(
    Long id,
    String name,
    String slug,
    String subdomain,
    TenantStatus status,
    Instant createdAt
) {}
