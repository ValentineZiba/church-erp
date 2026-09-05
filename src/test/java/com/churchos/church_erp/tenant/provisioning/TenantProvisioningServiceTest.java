package com.churchos.church_erp.tenant.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.churchos.church_erp.security.tenantuser.domain.TenantUser;
import com.churchos.church_erp.security.tenantuser.domain.TenantUserRole;
import com.churchos.church_erp.security.tenantuser.repository.TenantUserRepository;
import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.domain.TenantStatus;
import com.churchos.church_erp.tenant.exception.InvalidTenantSlugException;
import com.churchos.church_erp.tenant.exception.TenantAlreadyExistsException;
import com.churchos.church_erp.tenant.exception.TenantProvisioningException;
import com.churchos.church_erp.tenant.migration.TenantMigrationRunner;
import com.churchos.church_erp.tenant.repository.TenantRegistryRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class TenantProvisioningServiceTest {

    private final TenantRegistryRepository tenantRegistryRepository = mock(TenantRegistryRepository.class);
    private final TenantMigrationRunner tenantMigrationRunner = mock(TenantMigrationRunner.class);
    private final TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DataSource controlPlaneDataSource = mock(DataSource.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Connection connection = mock(Connection.class);
    private final Statement statement = mock(Statement.class);

    private TenantProvisioningService service;

    @BeforeEach
    void setUp() throws SQLException {
        when(controlPlaneDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(tenantRegistryRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        service = new TenantProvisioningService(
            tenantRegistryRepository, tenantMigrationRunner, tenantUserRepository, passwordEncoder,
            controlPlaneDataSource, eventPublisher,
            "127.0.0.1", 3306, "churchos", "churchos");
    }

    @Test
    void provisionsNewTenantEndToEndAndMarksActive() throws SQLException {
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.empty());

        Tenant result = service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass"));

        assertThat(result.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(result.getDbName()).isEqualTo("tenant_gracechapel");
        assertThat(result.getSubdomain()).isEqualTo("gracechapel");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement).executeUpdate(sql.capture());
        assertThat(sql.getValue()).isEqualTo(
            "CREATE DATABASE IF NOT EXISTS `tenant_gracechapel` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        verify(tenantMigrationRunner).migrateOne(result);
        verify(tenantRegistryRepository, times(2)).save(any(Tenant.class));
        verify(eventPublisher).publishEvent(new TenantProvisionedEvent("gracechapel"));

        ArgumentCaptor<TenantUser> savedAdmin = ArgumentCaptor.forClass(TenantUser.class);
        verify(tenantUserRepository).save(savedAdmin.capture());
        assertThat(savedAdmin.getValue().getEmail()).isEqualTo("admin@gracechapel.test");
        assertThat(savedAdmin.getValue().getFullName()).isEqualTo("Grace Admin");
        assertThat(savedAdmin.getValue().getRole()).isEqualTo(TenantUserRole.ADMIN);
        assertThat(savedAdmin.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void usesExplicitSubdomainWhenProvided() {
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.empty());

        Tenant result = service.provision(
            new TenantProvisioningRequest(
                "Grace Chapel", "gracechapel", "grace-chapel-online",
                "admin@gracechapel.test", "Grace Admin", "s3cret-pass"));

        assertThat(result.getSubdomain()).isEqualTo("grace-chapel-online");
    }

    @Test
    void rejectsInvalidSlugBeforeTouchingAnyCollaborator() {
        assertThatThrownBy(() -> service.provision(new TenantProvisioningRequest(
            "Bad", "Not_A_Slug!", null, "admin@bad.test", "Bad Admin", "s3cret-pass")))
            .isInstanceOf(InvalidTenantSlugException.class);

        verifyNoInteractions(tenantRegistryRepository, tenantMigrationRunner, controlPlaneDataSource, eventPublisher);
    }

    @Test
    void rejectsAnAlreadyActiveTenant() {
        Tenant active = existingTenant("gracechapel", TenantStatus.ACTIVE);
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass")))
            .isInstanceOf(TenantAlreadyExistsException.class);

        verifyNoInteractions(tenantMigrationRunner, controlPlaneDataSource, eventPublisher);
        verify(tenantRegistryRepository, never()).save(any());
    }

    @Test
    void rejectsAnAlreadySuspendedTenant() {
        Tenant suspended = existingTenant("gracechapel", TenantStatus.SUSPENDED);
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass")))
            .isInstanceOf(TenantAlreadyExistsException.class);
    }

    @Test
    void resumesFromAFailedTenantWithoutInsertingANewRow() {
        Tenant failed = existingTenant("gracechapel", TenantStatus.FAILED);
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.of(failed));

        Tenant result = service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass"));

        assertThat(result.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        verify(tenantRegistryRepository, times(1)).save(any(Tenant.class));
    }

    @Test
    void resumesFromAStillProvisioningTenant() {
        Tenant provisioning = existingTenant("gracechapel", TenantStatus.PROVISIONING);
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.of(provisioning));

        Tenant result = service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass"));

        assertThat(result.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        verify(tenantRegistryRepository, times(1)).save(any(Tenant.class));
    }

    @Test
    void marksFailedWhenDatabaseCreationThrows() throws SQLException {
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.empty());
        doThrow(new SQLException("connection refused")).when(statement).executeUpdate(anyString());

        assertThatThrownBy(() -> service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass")))
            .isInstanceOf(TenantProvisioningException.class);

        verifyNoInteractions(tenantMigrationRunner);
        ArgumentCaptor<Tenant> savedTenant = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRegistryRepository, times(2)).save(savedTenant.capture());
        assertThat(savedTenant.getAllValues().get(1).getStatus()).isEqualTo(TenantStatus.FAILED);
    }

    @Test
    void marksFailedWhenMigrationThrows() {
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("bad migration script")).when(tenantMigrationRunner).migrateOne(any());

        assertThatThrownBy(() -> service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass")))
            .isInstanceOf(TenantProvisioningException.class)
            .hasCauseInstanceOf(RuntimeException.class);

        ArgumentCaptor<Tenant> savedTenant = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRegistryRepository, times(2)).save(savedTenant.capture());
        assertThat(savedTenant.getAllValues().get(1).getStatus()).isEqualTo(TenantStatus.FAILED);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void aFailingEventListenerDoesNotFailTheOverallProvisioningCall() {
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("listener exploded")).when(eventPublisher).publishEvent(any());

        Tenant result = service.provision(new TenantProvisioningRequest(
            "Grace Chapel", "gracechapel", null, "admin@gracechapel.test", "Grace Admin", "s3cret-pass"));

        assertThat(result.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    private static Tenant existingTenant(String slug, TenantStatus status) {
        return Tenant.builder()
            .id(1L)
            .name("Grace Chapel")
            .slug(slug)
            .subdomain(slug)
            .dbHost("127.0.0.1")
            .dbPort(3306)
            .dbName("tenant_" + slug)
            .dbUsername("churchos")
            .dbPassword("churchos")
            .status(status)
            .build();
    }
}
