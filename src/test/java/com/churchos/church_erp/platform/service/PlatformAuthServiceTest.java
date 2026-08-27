package com.churchos.church_erp.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.churchos.church_erp.common.exception.InvalidCredentialsException;
import com.churchos.church_erp.platform.domain.PlatformAdmin;
import com.churchos.church_erp.platform.dto.LoginRequest;
import com.churchos.church_erp.platform.dto.LoginResponse;
import com.churchos.church_erp.platform.dto.PlatformAdminSummary;
import com.churchos.church_erp.platform.mapper.PlatformAdminMapper;
import com.churchos.church_erp.platform.repository.PlatformAdminRepository;
import com.churchos.church_erp.security.jwt.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PlatformAuthServiceTest {

    @Mock
    private PlatformAdminRepository platformAdminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private PlatformAdminMapper platformAdminMapper;

    private PlatformAuthService platformAuthService;

    private PlatformAdmin admin;

    @BeforeEach
    void setUp() {
        platformAuthService = new PlatformAuthService(
            platformAdminRepository, passwordEncoder, jwtService, platformAdminMapper);

        admin = PlatformAdmin.builder()
            .id(1L)
            .email("admin@churchos.local")
            .passwordHash("hashed-password")
            .fullName("Platform Admin")
            .build();
    }

    @Test
    void loginReturnsTokenAndAdminSummaryOnMatchingPassword() {
        when(platformAdminRepository.findByEmail("admin@churchos.local")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("admin@churchos.local")).thenReturn("signed-token");
        when(jwtService.getExpirationMinutes()).thenReturn(720L);
        PlatformAdminSummary summary = new PlatformAdminSummary(1L, "admin@churchos.local", "Platform Admin");
        when(platformAdminMapper.toSummary(admin)).thenReturn(summary);

        LoginResponse response = platformAuthService.login(
            new LoginRequest("admin@churchos.local", "correct-password"));

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMinutes()).isEqualTo(720L);
        assertThat(response.admin()).isEqualTo(summary);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(platformAdminRepository.findByEmail("admin@churchos.local")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() ->
            platformAuthService.login(new LoginRequest("admin@churchos.local", "wrong-password")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(platformAdminRepository.findByEmail("nobody@churchos.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            platformAuthService.login(new LoginRequest("nobody@churchos.local", "whatever")))
            .isInstanceOf(InvalidCredentialsException.class);
    }
}
