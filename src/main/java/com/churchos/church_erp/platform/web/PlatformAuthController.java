package com.churchos.church_erp.platform.web;

import com.churchos.church_erp.platform.dto.LoginRequest;
import com.churchos.church_erp.platform.dto.LoginResponse;
import com.churchos.church_erp.platform.service.PlatformAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Platform Auth", description = "Super-admin (control-plane) authentication")
public class PlatformAuthController {

    private final PlatformAuthService platformAuthService;

    public PlatformAuthController(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in as a platform (super-admin) user and receive a JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return platformAuthService.login(request);
    }
}
