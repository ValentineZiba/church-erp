package com.churchos.church_erp.platform.service;

import com.churchos.church_erp.common.exception.InvalidCredentialsException;
import com.churchos.church_erp.platform.domain.PlatformAdmin;
import com.churchos.church_erp.platform.dto.LoginRequest;
import com.churchos.church_erp.platform.dto.LoginResponse;
import com.churchos.church_erp.platform.dto.PlatformAdminSummary;
import com.churchos.church_erp.platform.mapper.PlatformAdminMapper;
import com.churchos.church_erp.platform.repository.PlatformAdminRepository;
import com.churchos.church_erp.security.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PlatformAuthService {

    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlatformAdminMapper platformAdminMapper;

    public PlatformAuthService(
        PlatformAdminRepository platformAdminRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        PlatformAdminMapper platformAdminMapper
    ) {
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.platformAdminMapper = platformAdminMapper;
    }

    public LoginResponse login(LoginRequest request) {
        PlatformAdmin admin = platformAdminRepository.findByEmail(request.email())
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateToken(admin.getEmail());
        return new LoginResponse(
            token,
            "Bearer",
            jwtService.getExpirationMinutes(),
            platformAdminMapper.toSummary(admin)
        );
    }

    public PlatformAdminSummary getByEmail(String email) {
        PlatformAdmin admin = platformAdminRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);
        return platformAdminMapper.toSummary(admin);
    }
}
