package com.churchos.church_erp.security.tenantuser.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.churchos.church_erp.common.exception.InvalidCredentialsException;
import com.churchos.church_erp.security.jwt.JwtService;
import com.churchos.church_erp.security.tenantuser.domain.TenantUser;
import com.churchos.church_erp.security.tenantuser.domain.TenantUserRole;
import com.churchos.church_erp.security.tenantuser.dto.TenantLoginRequest;
import com.churchos.church_erp.security.tenantuser.dto.TenantLoginResponse;
import com.churchos.church_erp.security.tenantuser.dto.TenantUserSummary;
import com.churchos.church_erp.security.tenantuser.mapper.TenantUserMapper;
import com.churchos.church_erp.security.tenantuser.repository.TenantUserRepository;
import com.churchos.church_erp.tenant.context.TenantContext;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TenantAuthServiceTest {

    @Mock
    private TenantUserRepository tenantUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TenantUserMapper tenantUserMapper;

    private TenantAuthService tenantAuthService;

    private TenantUser user;

    @BeforeEach
    void setUp() {
        tenantAuthService = new TenantAuthService(tenantUserRepository, passwordEncoder, jwtService, tenantUserMapper);

        user = TenantUser.builder()
            .id(1L)
            .email("staffer@gracechapel.test")
            .passwordHash("hashed-password")
            .fullName("Grace Staffer")
            .role(TenantUserRole.STAFF)
            .build();

        TenantContext.setCurrentTenantSlug("gracechapel");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void loginReturnsTokenScopedToTheCurrentTenantOnMatchingPassword() {
        when(tenantUserRepository.findByEmail("staffer@gracechapel.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateTenantToken("staffer@gracechapel.test", "gracechapel", "STAFF"))
            .thenReturn("signed-token");
        when(jwtService.getExpirationMinutes()).thenReturn(720L);
        TenantUserSummary summary = new TenantUserSummary(1L, "staffer@gracechapel.test", "Grace Staffer", TenantUserRole.STAFF);
        when(tenantUserMapper.toSummary(user)).thenReturn(summary);

        TenantLoginResponse response = tenantAuthService.login(
            new TenantLoginRequest("staffer@gracechapel.test", "correct-password"));

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMinutes()).isEqualTo(720L);
        assertThat(response.user()).isEqualTo(summary);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(tenantUserRepository.findByEmail("staffer@gracechapel.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() ->
            tenantAuthService.login(new TenantLoginRequest("staffer@gracechapel.test", "wrong-password")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(tenantUserRepository.findByEmail("nobody@gracechapel.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            tenantAuthService.login(new TenantLoginRequest("nobody@gracechapel.test", "whatever")))
            .isInstanceOf(InvalidCredentialsException.class);
    }
}
