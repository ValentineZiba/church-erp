package com.churchos.church_erp.tenant.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.domain.TenantStatus;
import com.churchos.church_erp.tenant.exception.TenantNotFoundException;
import com.churchos.church_erp.tenant.repository.TenantRegistryRepository;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class TenantDataSourceProviderTest {

    private final TenantRegistryRepository tenantRegistryRepository = mock(TenantRegistryRepository.class);
    private final TenantDataSourceProvider provider = new TenantDataSourceProvider(tenantRegistryRepository);

    @Test
    void buildsAndCachesAPoolForAnActiveTenant() {
        Tenant tenant = Tenant.builder()
            .slug("gracechapel")
            .dbHost("127.0.0.1")
            .dbPort(3306)
            .dbName("tenant_gracechapel")
            .dbUsername("churchos")
            .dbPassword("churchos")
            .status(TenantStatus.ACTIVE)
            .build();
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.of(tenant));

        DataSource first = provider.getDataSource("gracechapel");
        DataSource second = provider.getDataSource("gracechapel");

        assertThat(first).isSameAs(second);
        verify(tenantRegistryRepository, times(1)).findBySlug("gracechapel");
    }

    @Test
    void throwsForUnknownTenant() {
        when(tenantRegistryRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.getDataSource("ghost"))
            .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void throwsForNonActiveTenant() {
        Tenant tenant = Tenant.builder()
            .slug("suspended-church")
            .dbHost("127.0.0.1")
            .dbPort(3306)
            .dbName("tenant_suspended")
            .dbUsername("churchos")
            .dbPassword("churchos")
            .status(TenantStatus.SUSPENDED)
            .build();
        when(tenantRegistryRepository.findBySlug("suspended-church")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> provider.getDataSource("suspended-church"))
            .isInstanceOf(TenantNotFoundException.class);
    }
}
