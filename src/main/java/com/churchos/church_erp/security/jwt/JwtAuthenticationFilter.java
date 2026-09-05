package com.churchos.church_erp.security.jwt;

import com.churchos.church_erp.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates both platform-admin tokens ({@code JwtService#generateToken}) and tenant-user
 * tokens ({@code JwtService#generateTenantToken}). Runs after {@code TenantResolverFilter} in
 * {@code SecurityConfig}, so {@link TenantContext} already holds whatever tenant this request
 * resolved to (or nothing, for control-plane traffic).
 *
 * <p><b>Isolation-critical:</b> a tenant-user token is only honored when its embedded tenant
 * claim matches {@link TenantContext#getCurrentTenantSlug()} for this exact request. This is what
 * stops a token minted while logged into one church's subdomain from being replayed against a
 * different tenant (or against platform-admin routes, where no tenant is ever resolved) — without
 * it, tenant isolation would depend entirely on the client behaving, not on the server enforcing
 * it.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtService.isValid(token)) {
                authenticate(request, token);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        String email = jwtService.extractEmail(token);
        String tokenTenantSlug = jwtService.extractTenantSlug(token);

        String authority;
        if (tokenTenantSlug == null) {
            authority = "ROLE_SUPER_ADMIN";
        } else if (!tokenTenantSlug.equals(TenantContext.getCurrentTenantSlug())) {
            return;
        } else {
            authority = "ROLE_" + jwtService.extractRole(token);
        }

        AbstractAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            email, null, List.of(new SimpleGrantedAuthority(authority)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
