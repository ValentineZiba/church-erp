package com.churchos.church_erp.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.churchos.church_erp.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The isolation-critical behavior: a tenant-user token must only authenticate when the request's
 * resolved {@link TenantContext} matches the tenant the token was issued for. Without this check,
 * a token stolen or replayed against a different tenant's subdomain (or against platform-admin
 * routes, where no tenant is ever resolved) would still authenticate.
 */
class JwtAuthenticationFilterTest {

    private final JwtService jwtService = new JwtService("test-secret-01234567890123456789012345678901234567", 60);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformTokenAuthenticatesAsSuperAdminRegardlessOfTenantContext() throws Exception {
        String token = jwtService.generateToken("admin@churchos.local");
        TenantContext.setCurrentTenantSlug("gracechapel");

        filter.doFilterInternal(requestWithBearer(token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin@churchos.local");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting(Object::toString).containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    void tenantTokenAuthenticatesWithItsRoleWhenTenantContextMatches() throws Exception {
        String token = jwtService.generateTenantToken("staffer@gracechapel.test", "gracechapel", "STAFF");
        TenantContext.setCurrentTenantSlug("gracechapel");

        filter.doFilterInternal(requestWithBearer(token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
            .isEqualTo("staffer@gracechapel.test");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting(Object::toString).containsExactly("ROLE_STAFF");
    }

    @Test
    void tenantTokenIsRejectedWhenTenantContextIsForADifferentTenant() throws Exception {
        String token = jwtService.generateTenantToken("staffer@gracechapel.test", "gracechapel", "STAFF");
        TenantContext.setCurrentTenantSlug("othertenant");

        filter.doFilterInternal(requestWithBearer(token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tenantTokenIsRejectedWhenNoTenantIsResolvedAtAll() throws Exception {
        String token = jwtService.generateTenantToken("staffer@gracechapel.test", "gracechapel", "STAFF");

        filter.doFilterInternal(requestWithBearer(token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
