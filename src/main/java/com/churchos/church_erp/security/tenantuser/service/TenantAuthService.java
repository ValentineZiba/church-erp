package com.churchos.church_erp.security.tenantuser.service;

import com.churchos.church_erp.common.exception.InvalidCredentialsException;
import com.churchos.church_erp.security.jwt.JwtService;
import com.churchos.church_erp.security.tenantuser.domain.TenantUser;
import com.churchos.church_erp.security.tenantuser.dto.TenantLoginRequest;
import com.churchos.church_erp.security.tenantuser.dto.TenantLoginResponse;
import com.churchos.church_erp.security.tenantuser.mapper.TenantUserMapper;
import com.churchos.church_erp.security.tenantuser.repository.TenantUserRepository;
import com.churchos.church_erp.tenant.context.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Called only for requests {@code TenantResolverFilter} has already resolved to a specific
 * tenant, so {@link TenantUserRepository} here transparently hits that tenant's own database —
 * never the control plane and never another church's.
 */
@Service
public class TenantAuthService {

    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantUserMapper tenantUserMapper;

    public TenantAuthService(
        TenantUserRepository tenantUserRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        TenantUserMapper tenantUserMapper
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tenantUserMapper = tenantUserMapper;
    }

    public TenantLoginResponse login(TenantLoginRequest request) {
        TenantUser user = tenantUserRepository.findByEmail(request.email())
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateTenantToken(
            user.getEmail(), TenantContext.getCurrentTenantSlug(), user.getRole().name());
        return new TenantLoginResponse(
            token,
            "Bearer",
            jwtService.getExpirationMinutes(),
            tenantUserMapper.toSummary(user)
        );
    }
}
