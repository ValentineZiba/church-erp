package com.churchos.church_erp.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService =
        new JwtService("test-secret-01234567890123456789012345678901234567", 60);

    @Test
    void tokenRoundTripsToTheSameSubjectEmail() {
        String token = jwtService.generateToken("admin@churchos.local");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@churchos.local");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("admin@churchos.local");

        assertThat(jwtService.isValid(token + "tampered")).isFalse();
    }

    @Test
    void gibberishTokenIsRejected() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }
}
