package com.churchos.church_erp.platform.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInMinutes,
    PlatformAdminSummary admin
) {}
