package com.churchos.church_erp.tenant.datasource;

import com.churchos.church_erp.tenant.context.TenantContext;
import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Routes every JDBC connection acquisition to the physical database of whichever tenant is set
 * in {@link TenantContext} for the current thread. Not yet wired to any {@code EntityManagerFactory}
 * — no tenant-scoped JPA repositories exist yet (see docs/ROADMAP.md §3.3). It's registered as
 * infrastructure a future tenant-scoped persistence config (membership, giving, etc.) will target.
 *
 * <p>Overrides {@link #determineTargetDataSource()} directly, bypassing the base class's static
 * {@code resolvedDataSources} map, so pools can be created and evicted dynamically by
 * {@link TenantDataSourceProvider} instead of being fixed at startup.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final TenantDataSourceProvider dataSourceProvider;

    public TenantRoutingDataSource(TenantDataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
        setTargetDataSources(new HashMap<>());
        afterPropertiesSet();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        String tenantSlug = TenantContext.getCurrentTenantSlug();
        if (tenantSlug == null) {
            throw new IllegalStateException(
                "No tenant resolved in TenantContext — refusing to route a database connection");
        }
        return dataSourceProvider.getDataSource(tenantSlug);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenantSlug();
    }
}
