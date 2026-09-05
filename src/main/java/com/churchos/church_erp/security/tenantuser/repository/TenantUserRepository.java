package com.churchos.church_erp.security.tenantuser.repository;

import com.churchos.church_erp.security.tenantuser.domain.TenantUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Tenant-scoped repository — every method resolves against whichever tenant database
 * {@code TenantContext} currently points to (see {@code tenant/config/TenantJpaConfig}), never
 * the control-plane database. Callers must ensure a tenant is resolved before invoking this.
 */
public interface TenantUserRepository extends JpaRepository<TenantUser, Long> {

    Optional<TenantUser> findByEmail(String email);
}
