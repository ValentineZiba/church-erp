package com.churchos.church_erp.tenant.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.churchos.church_erp.tenant.datasource.TenantDataSourceProvider;
import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.domain.TenantStatus;
import com.churchos.church_erp.tenant.repository.TenantRegistryRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

class TenantMigrationRunnerTest {

    private final TenantRegistryRepository tenantRegistryRepository = mock(TenantRegistryRepository.class);
    private final TenantDataSourceProvider tenantDataSourceProvider = mock(TenantDataSourceProvider.class);
    private final TenantMigrationRunner runner =
        new TenantMigrationRunner(tenantRegistryRepository, tenantDataSourceProvider);

    @Test
    void migratesEveryActiveTenantAndSkipsNonActiveOnes() {
        Tenant active = tenant("gracechapel", TenantStatus.ACTIVE);
        DataSource realDataSource = freshH2DataSource();
        when(tenantRegistryRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(active));
        when(tenantDataSourceProvider.getDataSource(active)).thenReturn(realDataSource);

        TenantMigrationSummary summary = runner.migrateAll();

        assertThat(summary.succeededTenantSlugs()).containsExactly("gracechapel");
        assertThat(summary.hasFailures()).isFalse();
        assertThat(flywaySchemaHistoryExists(realDataSource)).isTrue();
    }

    @Test
    void oneTenantFailingDoesNotStopTheOthers() {
        Tenant healthy = tenant("gracechapel", TenantStatus.ACTIVE);
        Tenant broken = tenant("unreachable-church", TenantStatus.ACTIVE);
        DataSource healthyDataSource = freshH2DataSource();
        DataSource brokenDataSource = brokenDataSource();
        when(tenantRegistryRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(healthy, broken));
        when(tenantDataSourceProvider.getDataSource(healthy)).thenReturn(healthyDataSource);
        when(tenantDataSourceProvider.getDataSource(broken)).thenReturn(brokenDataSource);

        TenantMigrationSummary summary = runner.migrateAll();

        assertThat(summary.succeededTenantSlugs()).containsExactly("gracechapel");
        assertThat(summary.hasFailures()).isTrue();
        assertThat(summary.failedTenants()).containsKey("unreachable-church");
    }

    @Test
    void isIdempotentAcrossRepeatedRuns() {
        Tenant active = tenant("gracechapel", TenantStatus.ACTIVE);
        DataSource realDataSource = freshH2DataSource();
        when(tenantRegistryRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(active));
        when(tenantDataSourceProvider.getDataSource(active)).thenReturn(realDataSource);

        TenantMigrationSummary first = runner.migrateAll();
        TenantMigrationSummary second = runner.migrateAll();

        assertThat(first.hasFailures()).isFalse();
        assertThat(second.hasFailures()).isFalse();
    }

    private static Tenant tenant(String slug, TenantStatus status) {
        return Tenant.builder()
            .slug(slug)
            .name(slug)
            .subdomain(slug)
            .dbHost("127.0.0.1")
            .dbPort(3306)
            .dbName("tenant_" + slug)
            .dbUsername("churchos")
            .dbPassword("churchos")
            .status(status)
            .build();
    }

    private static DataSource freshH2DataSource() {
        String schema = "tenant_test_" + UUID.randomUUID().toString().replace("-", "");
        return DataSourceBuilder.create()
            .driverClassName("org.h2.Driver")
            .url("jdbc:h2:mem:" + schema + ";DB_CLOSE_DELAY=-1")
            .username("sa")
            .password("")
            .build();
    }

    private static DataSource brokenDataSource() {
        DataSource dataSource = mock(DataSource.class);
        try {
            when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));
        } catch (SQLException impossible) {
            throw new AssertionError(impossible);
        }
        return dataSource;
    }

    private static boolean flywaySchemaHistoryExists(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT COUNT(*) FROM information_schema.tables WHERE UPPER(table_name) = 'FLYWAY_SCHEMA_HISTORY'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        } catch (SQLException ex) {
            throw new AssertionError(ex);
        }
    }
}
