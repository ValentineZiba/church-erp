package com.churchos.church_erp.tenant.web;

import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.dto.TenantSummary;
import com.churchos.church_erp.tenant.mapper.TenantMapper;
import com.churchos.church_erp.tenant.provisioning.TenantProvisioningRequest;
import com.churchos.church_erp.tenant.provisioning.TenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Super-admin tenant management. Under {@code /api/platform/**}, so it's already exempt from
 * {@code TenantResolverFilter} (this is control-plane traffic, not a request for a specific
 * tenant) and already requires an authenticated platform admin via {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/platform/tenants")
@Tag(name = "Tenant Management", description = "Super-admin tenant provisioning")
public class TenantAdminController {

    private final TenantProvisioningService tenantProvisioningService;
    private final TenantMapper tenantMapper;

    public TenantAdminController(TenantProvisioningService tenantProvisioningService, TenantMapper tenantMapper) {
        this.tenantProvisioningService = tenantProvisioningService;
        this.tenantMapper = tenantMapper;
    }

    @PostMapping
    @Operation(summary = "Provision a new tenant (church): registers it, creates its database, "
        + "runs its baseline schema, and activates it")
    public ResponseEntity<TenantSummary> provision(@Valid @RequestBody TenantProvisioningRequest request) {
        Tenant tenant = tenantProvisioningService.provision(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantMapper.toSummary(tenant));
    }
}
