package com.churchos.church_erp.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
        @Value("${churchos.security.jwt.secret}") String secret,
        @Value("${churchos.security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String subjectEmail) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(subjectEmail)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
            .signWith(key)
            .compact();
    }

    /**
     * A tenant-user token, scoped to one church. Carries the tenant slug and role as extra claims
     * so {@code JwtAuthenticationFilter} can both authorize (role) and, critically, refuse to
     * honor this token on any request that didn't resolve to the same tenant (isolation).
     */
    public String generateTenantToken(String subjectEmail, String tenantSlug, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(subjectEmail)
            .claim("tenant", tenantSlug)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
            .signWith(key)
            .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** Null for a plain platform-admin token (made by {@link #generateToken}). */
    public String extractTenantSlug(String token) {
        return parseClaims(token).get("tenant", String.class);
    }

    /** Null for a plain platform-admin token (made by {@link #generateToken}). */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
