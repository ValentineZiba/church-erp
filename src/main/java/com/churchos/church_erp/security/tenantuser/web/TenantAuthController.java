package com.churchos.church_erp.security.tenantuser.web;

import com.churchos.church_erp.security.tenantuser.dto.TenantLoginRequest;
import com.churchos.church_erp.security.tenantuser.dto.TenantLoginResponse;
import com.churchos.church_erp.security.tenantuser.service.TenantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant-user login. Unlike {@code PlatformAuthController}, this path is NOT exempt from
 * {@code TenantResolverFilter} — the request must already carry a resolvable tenant (subdomain or
 * {@code X-Tenant-ID}) so the login lookup hits the right church's database.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Tenant Auth", description = "Church staff/volunteer/member authentication")
public class TenantAuthController {

    private final TenantAuthService tenantAuthService;

    public TenantAuthController(TenantAuthService tenantAuthService) {
        this.tenantAuthService = tenantAuthService;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in as a tenant user (church staff/volunteer/member) and receive a JWT")
    public TenantLoginResponse login(@Valid @RequestBody TenantLoginRequest request) {
        return tenantAuthService.login(request);
    }
}
