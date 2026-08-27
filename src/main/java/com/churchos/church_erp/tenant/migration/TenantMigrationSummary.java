package com.churchos.church_erp.tenant.migration;

import java.util.List;
import java.util.Map;

/**
 * Result of one {@code TenantMigrationRunner.migrateAll()} sweep. {@code failedTenants} maps
 * tenant slug to the failure message so a bad migration on one church is visible without having
 * to dig through logs, while every other tenant still got attempted.
 */
public record TenantMigrationSummary(List<String> succeededTenantSlugs, Map<String, String> failedTenants) {

    public boolean hasFailures() {
        return !failedTenants.isEmpty();
    }
}
