package com.churchos.church_erp.tenant.repository;

import com.churchos.church_erp.tenant.domain.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Control-plane repository — always resolves against {@code churchos_control} via the
 * {@code @Primary} datasource, regardless of any tenant currently set in {@code TenantContext}.
 */
public interface TenantRegistryRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlug(String slug);
}
