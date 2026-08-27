package com.churchos.church_erp.tenant.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.churchos.church_erp.tenant.context.TenantContext;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantRoutingDataSourceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void throwsWhenNoTenantIsResolved() {
        TenantDataSourceProvider provider = mock(TenantDataSourceProvider.class);
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(provider);

        assertThatThrownBy(routingDataSource::determineTargetDataSource)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delegatesToProviderForTheResolvedTenant() throws Exception {
        TenantDataSourceProvider provider = mock(TenantDataSourceProvider.class);
        DataSource tenantDataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(provider.getDataSource("gracechapel")).thenReturn(tenantDataSource);
        when(tenantDataSource.getConnection()).thenReturn(connection);

        TenantContext.setCurrentTenantSlug("gracechapel");
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(provider);

        assertThat(routingDataSource.getConnection()).isSameAs(connection);
    }
}
