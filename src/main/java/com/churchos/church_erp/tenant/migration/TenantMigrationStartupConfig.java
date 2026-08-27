package com.churchos.church_erp.tenant.migration;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Opt-in: runs {@link TenantMigrationRunner} once at application startup, before this instance
 * starts serving traffic, when {@code churchos.tenant.migrate-on-startup=true}. Off by default so
 * every local restart and every test context load doesn't reach out to real tenant databases —
 * flip it on for a deploy, or run {@code TenantMigrationRunner.migrateAll()} from a dedicated CI
 * step instead (see docs/ROADMAP.md §3.5 and §9.4).
 */
@Configuration
public class TenantMigrationStartupConfig {

    @Bean
    @ConditionalOnProperty(name = "churchos.tenant.migrate-on-startup", havingValue = "true")
    public ApplicationRunner tenantMigrationStartupRunner(TenantMigrationRunner tenantMigrationRunner) {
        return args -> tenantMigrationRunner.migrateAll();
    }
}
