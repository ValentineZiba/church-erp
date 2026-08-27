package com.churchos.church_erp.tenant.migration;

import com.churchos.church_erp.tenant.datasource.TenantDataSourceProvider;
import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.domain.TenantStatus;
import com.churchos.church_erp.tenant.repository.TenantRegistryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deploy-time migration runner (docs/ROADMAP.md §3.5): applies every pending tenant-schema Flyway
 * migration to each tenant's physical database. Deliberately independent of the per-request
 * {@code TenantContext}/{@code TenantRoutingDataSource} path — this runs outside any request
 * (at startup, or from a CI step), so it resolves a {@link DataSource} per tenant directly via
 * {@link TenantDataSourceProvider} instead.
 *
 * <p>Only {@link TenantStatus#ACTIVE} tenants are migrated. {@code PROVISIONING} tenants get
 * their baseline schema from the (not yet built) provisioning flow instead; {@code SUSPENDED}
 * and {@code FAILED} tenants are left alone so a broad platform migration sweep doesn't touch a
 * database that's paused or already in a known-bad state.
 *
 * <p>Each tenant is migrated independently and a failure on one does not stop the run — see
 * {@link TenantMigrationSummary}. Never combine this with the control-plane Flyway location; see
 * {@code CLAUDE.md}.
 */
@Component
public class TenantMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationRunner.class);
    private static final String TENANT_MIGRATION_LOCATION = "classpath:db/migration/tenant";

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantDataSourceProvider tenantDataSourceProvider;

    public TenantMigrationRunner(
        TenantRegistryRepository tenantRegistryRepository,
        TenantDataSourceProvider tenantDataSourceProvider
    ) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.tenantDataSourceProvider = tenantDataSourceProvider;
    }

    public TenantMigrationSummary migrateAll() {
        List<Tenant> tenants = tenantRegistryRepository.findByStatus(TenantStatus.ACTIVE);
        List<String> succeeded = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();

        for (Tenant tenant : tenants) {
            try {
                migrateOne(tenant);
                succeeded.add(tenant.getSlug());
            } catch (Exception ex) {
                log.error("Tenant migration failed for '{}'", tenant.getSlug(), ex);
                failed.put(tenant.getSlug(), ex.getMessage());
            }
        }

        log.info("Tenant migration run complete: {} succeeded, {} failed", succeeded.size(), failed.size());
        return new TenantMigrationSummary(succeeded, failed);
    }

    /**
     * Applies pending tenant-schema migrations to a single tenant, regardless of its current
     * {@link TenantStatus}. Used by {@link #migrateAll()} for the ACTIVE sweep, and directly by
     * {@code TenantProvisioningService} to lay down the baseline schema on a brand-new database
     * (still {@code PROVISIONING} at that point).
     */
    public void migrateOne(Tenant tenant) {
        DataSource dataSource = tenantDataSourceProvider.getDataSource(tenant);
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(TENANT_MIGRATION_LOCATION)
            .baselineOnMigrate(true)
            .load();
        flyway.migrate();
    }
}
