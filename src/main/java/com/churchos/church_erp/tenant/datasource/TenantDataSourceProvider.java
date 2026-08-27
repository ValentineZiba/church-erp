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
 * Resolves a tenant slug to a pooled {@link DataSource}, backed by a bounded, idle-evicting
 * cache so an active church keeps a small warm Hikari pool while an inactive one gets its
 * connections released. Cache miss looks the tenant up in the control-plane {@code tenants}
 * registry and opens a new pool against that tenant's physical database.
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
        return pools.get(tenantSlug, this::openPool);
    }

    private HikariDataSource openPool(String tenantSlug) {
        Tenant tenant = tenantRegistryRepository.findBySlug(tenantSlug)
            .filter(candidate -> candidate.getStatus() == TenantStatus.ACTIVE)
            .orElseThrow(() -> new TenantNotFoundException(tenantSlug));

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(
            "jdbc:mysql://%s:%d/%s".formatted(tenant.getDbHost(), tenant.getDbPort(), tenant.getDbName()));
        dataSource.setUsername(tenant.getDbUsername());
        dataSource.setPassword(tenant.getDbPassword());
        dataSource.setMaximumPoolSize(MAX_POOL_SIZE_PER_TENANT);
        dataSource.setMinimumIdle(1);
        dataSource.setPoolName("tenant-" + tenantSlug);
        return dataSource;
    }
}
