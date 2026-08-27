package com.churchos.church_erp.tenant.datasource;

import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.domain.TenantStatus;
import com.churchos.church_erp.tenant.exception.TenantNotFoundException;
import com.churchos.church_erp.tenant.repository.TenantRegistryRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/**
 * Resolves a tenant to a pooled {@link DataSource}, backed by a bounded, idle-evicting cache so
 * an active church keeps a small warm Hikari pool while an inactive one gets its connections
 * released.
 *
 * <p>Two entry points, deliberately different in strictness:
 * <ul>
 *   <li>{@link #getDataSource(String)} — slug-only, looks the tenant up in the control-plane
 *       registry and requires it to be {@link TenantStatus#ACTIVE}. This is the request path
 *       ({@code TenantRoutingDataSource}), and the ACTIVE check here is defense-in-depth on top
 *       of {@code TenantResolverFilter} already having rejected anything non-ACTIVE.</li>
 *   <li>{@link #getDataSource(Tenant)} — takes a {@link Tenant} the caller already holds and
 *       trusts, no status check. For {@code TenantProvisioningService}, which needs a pool for a
 *       brand-new tenant that's still {@code PROVISIONING} — it hasn't earned ACTIVE yet, but
 *       it's the caller's own row, not an arbitrary request resolving an arbitrary slug.</li>
 * </ul>
 */
@Component
public class TenantDataSourceProvider {

    private static final int MAX_POOL_SIZE_PER_TENANT = 5;
    private static final int MAX_CACHED_TENANT_POOLS = 200;
    private static final Duration IDLE_EVICTION = Duration.ofMinutes(30);

    private final TenantRegistryRepository tenantRegistryRepository;
    private final Cache<String, HikariDataSource> pools;

    public TenantDataSourceProvider(TenantRegistryRepository tenantRegistryRepository) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.pools = Caffeine.newBuilder()
            .maximumSize(MAX_CACHED_TENANT_POOLS)
            .expireAfterAccess(IDLE_EVICTION)
            .<String, HikariDataSource>evictionListener((slug, dataSource, cause) -> {
                if (dataSource != null) {
                    dataSource.close();
                }
            })
            .build();
    }

    public DataSource getDataSource(String tenantSlug) {
        return pools.get(tenantSlug, slug -> openPool(lookupActiveTenant(slug)));
    }

    public DataSource getDataSource(Tenant tenant) {
        return pools.get(tenant.getSlug(), slug -> openPool(tenant));
    }

    private Tenant lookupActiveTenant(String tenantSlug) {
        return tenantRegistryRepository.findBySlug(tenantSlug)
            .filter(candidate -> candidate.getStatus() == TenantStatus.ACTIVE)
            .orElseThrow(() -> new TenantNotFoundException(tenantSlug));
    }

    private HikariDataSource openPool(Tenant tenant) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(
            "jdbc:mysql://%s:%d/%s".formatted(tenant.getDbHost(), tenant.getDbPort(), tenant.getDbName()));
        dataSource.setUsername(tenant.getDbUsername());
        dataSource.setPassword(tenant.getDbPassword());
        dataSource.setMaximumPoolSize(MAX_POOL_SIZE_PER_TENANT);
        dataSource.setMinimumIdle(1);
        dataSource.setPoolName("tenant-" + tenant.getSlug());
        return dataSource;
    }
}
