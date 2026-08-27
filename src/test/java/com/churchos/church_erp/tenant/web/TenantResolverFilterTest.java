package com.churchos.church_erp.tenant.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.churchos.church_erp.tenant.context.TenantContext;
import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.domain.TenantStatus;
import com.churchos.church_erp.tenant.repository.TenantRegistryRepository;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantResolverFilterTest {

    private TenantRegistryRepository tenantRegistryRepository;
    private TenantResolverFilter filter;

    @BeforeEach
    void setUp() {
        tenantRegistryRepository = mock(TenantRegistryRepository.class);
        filter = new TenantResolverFilter(tenantRegistryRepository, "churchos.app");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolvesTenantFromSubdomainAndClearsAfterRequest() throws Exception {
        Tenant tenant = activeTenant("gracechapel");
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.of(tenant));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        request.setServerName("gracechapel.churchos.app");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        String[] tenantSeenByChain = new String[1];
        FilterChain capturingChain = (req, res) -> tenantSeenByChain[0] = TenantContext.getCurrentTenantSlug();

        filter.doFilterInternal(request, response, capturingChain);

        assertThat(tenantSeenByChain[0]).isEqualTo("gracechapel");
        assertThat(TenantContext.getCurrentTenantSlug()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void headerTakesPrecedenceOverSubdomain() throws Exception {
        Tenant tenant = activeTenant("othertenant");
        when(tenantRegistryRepository.findBySlug("othertenant")).thenReturn(Optional.of(tenant));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        request.setServerName("gracechapel.churchos.app");
        request.addHeader("X-Tenant-ID", "OtherTenant");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void returnsBadRequestWhenNoTenantCanBeResolved() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        request.setServerName("localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        verify(chain, never()).doFilter(request, response);
        assertThat(TenantContext.getCurrentTenantSlug()).isNull();
    }

    @Test
    void returnsNotFoundForUnknownTenant() throws Exception {
        when(tenantRegistryRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        request.setServerName("ghost.churchos.app");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void returnsNotFoundForSuspendedTenant() throws Exception {
        Tenant tenant = activeTenant("gracechapel");
        tenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRegistryRepository.findBySlug("gracechapel")).thenReturn(Optional.of(tenant));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        request.setServerName("gracechapel.churchos.app");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void exemptsPlatformAndAuthPaths() {
        MockHttpServletRequest authRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletRequest platformRequest = new MockHttpServletRequest("GET", "/api/platform/me");
        MockHttpServletRequest actuatorRequest = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletRequest businessRequest = new MockHttpServletRequest("GET", "/api/members");

        assertThat(filter.shouldNotFilter(authRequest)).isTrue();
        assertThat(filter.shouldNotFilter(platformRequest)).isTrue();
        assertThat(filter.shouldNotFilter(actuatorRequest)).isTrue();
        assertThat(filter.shouldNotFilter(businessRequest)).isFalse();
    }

    private static Tenant activeTenant(String slug) {
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
            .status(TenantStatus.ACTIVE)
            .build();
    }
}
