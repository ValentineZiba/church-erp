package com.churchos.church_erp.tenant.web;

import com.churchos.church_erp.tenant.context.TenantContext;
import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.domain.TenantStatus;
import com.churchos.church_erp.tenant.repository.TenantRegistryRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the tenant for every request that isn't platform/control-plane traffic, and stores it
 * in {@link TenantContext} for the lifetime of the request. Always clears the context in a
 * {@code finally} block, including on the early-return error paths below, so nothing leaks across
 * requests on a pooled request-handling thread.
 *
 * <p>Resolution order: an explicit {@code X-Tenant-ID} header (for API/mobile clients that don't
 * go through a subdomain) takes precedence, otherwise the leftmost label of the request host is
 * used as the tenant slug (e.g. {@code gracechapel.churchos.app} -> {@code gracechapel}). Requests
 * against the bare base domain, {@code www}, or a host that isn't a direct subdomain of it never
 * resolve — callers must use the header in those cases (this includes local dev against
 * {@code localhost}, which has no subdomain at all).
 */
@Component
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private static final List<String> EXEMPT_PATH_PATTERNS = List.of(
        "/api/auth/**",
        "/api/platform/**",
        "/actuator/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    );

    private final TenantRegistryRepository tenantRegistryRepository;
    private final String baseDomain;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantResolverFilter(
        TenantRegistryRepository tenantRegistryRepository,
        @Value("${churchos.tenant.base-domain}") String baseDomain
    ) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.baseDomain = baseDomain;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXEMPT_PATH_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String tenantSlug = resolveTenantSlug(request);
            if (tenantSlug == null) {
                response.sendError(HttpStatus.BAD_REQUEST.value(),
                    "Unable to resolve a tenant from this request. Use a tenant subdomain or the "
                        + TENANT_HEADER + " header.");
                return;
            }

            Optional<Tenant> tenant = tenantRegistryRepository.findBySlug(tenantSlug);
            if (tenant.isEmpty() || tenant.get().getStatus() != TenantStatus.ACTIVE) {
                response.sendError(HttpStatus.NOT_FOUND.value(), "Unknown or inactive tenant: " + tenantSlug);
                return;
            }

            TenantContext.setCurrentTenantSlug(tenant.get().getSlug());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenantSlug(HttpServletRequest request) {
        String headerTenant = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(headerTenant)) {
            return headerTenant.trim().toLowerCase(Locale.ROOT);
        }
        return extractSubdomain(request.getServerName());
    }

    private String extractSubdomain(String host) {
        if (host == null || !host.endsWith("." + baseDomain)) {
            return null;
        }
        String subdomain = host.substring(0, host.length() - baseDomain.length() - 1);
        if (subdomain.isEmpty() || subdomain.equals("www") || subdomain.contains(".")) {
            return null;
        }
        return subdomain.toLowerCase(Locale.ROOT);
    }
}
