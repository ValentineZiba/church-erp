package com.churchos.church_erp.tenant.provisioning;

import com.churchos.church_erp.security.tenantuser.domain.TenantUser;
import com.churchos.church_erp.security.tenantuser.domain.TenantUserRole;
import com.churchos.church_erp.security.tenantuser.repository.TenantUserRepository;
import com.churchos.church_erp.tenant.context.TenantContext;
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
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Tenant provisioning (docs/ROADMAP.md §3.4): triggered when a new church signs up or a super
 * admin creates a tenant. Steps 1-3 and 5-7 of the roadmap flow are implemented here; step 4
 * (seed default fund/membership statuses) is deliberately NOT — there are no membership/accounting
 * entities to seed into yet. Once those modules exist, this is where their seeding call belongs.
 *
 * <p><b>Deliberately not {@code @Transactional}.</b> The two {@code tenantRegistryRepository.save}
 * calls below (the initial PROVISIONING insert, and the final ACTIVE/FAILED update) must each
 * commit immediately and independently, each via Spring Data's own per-call transaction. If this
 * whole method were wrapped in one transaction, a crash between "create the database" and "flip
 * to ACTIVE" would roll back the PROVISIONING row too — losing the one durable signal that a
 * provisioning attempt happened and where it got stuck.
 *
 * <p><b>Idempotent by design</b>, so a retry (manual, or a future retry job) is always safe: a
 * request for a slug already {@code FAILED} or still {@code PROVISIONING} resumes on the existing
 * row rather than erroring or duplicating it; {@code CREATE DATABASE IF NOT EXISTS} and
 * {@code Flyway.migrate()} are both no-ops if already done. A slug that's {@code ACTIVE} or
 * {@code SUSPENDED} is rejected outright — that's not this method's job to change.
 */
@Service
public class TenantProvisioningService {

    static final String SLUG_PATTERN = "^[a-z][a-z0-9-]{1,49}$";

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMigrationRunner tenantMigrationRunner;
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource controlPlaneDataSource;
    private final ApplicationEventPublisher eventPublisher;
    private final String defaultDbHost;
    private final int defaultDbPort;
    private final String defaultDbUsername;
    private final String defaultDbPassword;

    public TenantProvisioningService(
        TenantRegistryRepository tenantRegistryRepository,
        TenantMigrationRunner tenantMigrationRunner,
        TenantUserRepository tenantUserRepository,
        PasswordEncoder passwordEncoder,
        @Qualifier("controlPlaneDataSource") DataSource controlPlaneDataSource,
        ApplicationEventPublisher eventPublisher,
        @Value("${DB_HOST:127.0.0.1}") String defaultDbHost,
        @Value("${DB_PORT:3306}") int defaultDbPort,
        @Value("${DB_USER:churchos}") String defaultDbUsername,
        @Value("${DB_PASSWORD:churchos}") String defaultDbPassword
    ) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.tenantMigrationRunner = tenantMigrationRunner;
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.controlPlaneDataSource = controlPlaneDataSource;
        this.eventPublisher = eventPublisher;
        this.defaultDbHost = defaultDbHost;
        this.defaultDbPort = defaultDbPort;
        this.defaultDbUsername = defaultDbUsername;
        this.defaultDbPassword = defaultDbPassword;
    }

    public Tenant provision(TenantProvisioningRequest request) {
        if (!request.slug().matches(SLUG_PATTERN)) {
            throw new InvalidTenantSlugException(request.slug(), SLUG_PATTERN);
        }

        Tenant tenant = tenantRegistryRepository.findBySlug(request.slug())
            .map(this::requireResumable)
            .orElseGet(() -> beginProvisioning(request));

        try {
            createDatabaseIfNotExists(tenant.getDbName());
            tenantMigrationRunner.migrateOne(tenant);
            seedInitialAdminUser(tenant, request);

            tenant.setStatus(TenantStatus.ACTIVE);
            tenant = tenantRegistryRepository.save(tenant);
        } catch (Exception ex) {
            log.error("Provisioning failed for tenant '{}'", tenant.getSlug(), ex);
            tenant.setStatus(TenantStatus.FAILED);
            tenantRegistryRepository.save(tenant);
            throw new TenantProvisioningException(tenant.getSlug(), ex);
        }

        publishProvisionedEvent(tenant.getSlug());
        return tenant;
    }

    private Tenant requireResumable(Tenant existing) {
        if (existing.getStatus() == TenantStatus.ACTIVE || existing.getStatus() == TenantStatus.SUSPENDED) {
            throw new TenantAlreadyExistsException(existing.getSlug());
        }
        return existing;
    }

    private Tenant beginProvisioning(TenantProvisioningRequest request) {
        Tenant tenant = Tenant.builder()
            .name(request.name())
            .slug(request.slug())
            .subdomain(request.resolvedSubdomain())
            .dbHost(defaultDbHost)
            .dbPort(defaultDbPort)
            .dbName("tenant_" + request.slug())
            .dbUsername(defaultDbUsername)
            .dbPassword(defaultDbPassword)
            .status(TenantStatus.PROVISIONING)
            .build();
        return tenantRegistryRepository.save(tenant);
    }

    private void createDatabaseIfNotExists(String dbName) {
        try (Connection connection = controlPlaneDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create database '" + dbName + "'", ex);
        }
    }

    /**
     * Runs outside any HTTP request, so unlike a normal tenant-scoped repository call there is no
     * {@code TenantResolverFilter} to resolve {@link TenantContext} beforehand — it must be set
     * and cleared by hand here (see CLAUDE.md's multi-tenancy rules). Guarded by {@code count() >
     * 0} so a provisioning retry (this method runs again on a resumed {@code PROVISIONING}/
     * {@code FAILED} tenant) never creates a second admin or collides on the unique email
     * constraint.
     */
    private void seedInitialAdminUser(Tenant tenant, TenantProvisioningRequest request) {
        TenantContext.setCurrentTenantSlug(tenant.getSlug());
        try {
            if (tenantUserRepository.count() > 0) {
                return;
            }
            TenantUser admin = TenantUser.builder()
                .email(request.adminEmail())
                .passwordHash(passwordEncoder.encode(request.adminPassword()))
                .fullName(request.adminFullName())
                .role(TenantUserRole.ADMIN)
                .build();
            tenantUserRepository.save(admin);
        } finally {
            TenantContext.clear();
        }
    }

    private void publishProvisionedEvent(String tenantSlug) {
        try {
            eventPublisher.publishEvent(new TenantProvisionedEvent(tenantSlug));
        } catch (Exception ex) {
            log.error("A TenantProvisionedEvent listener failed for tenant '{}'; tenant is ACTIVE regardless", tenantSlug, ex);
        }
    }
}
