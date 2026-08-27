package com.churchos.church_erp.platform.web;

import com.churchos.church_erp.platform.dto.PlatformAdminSummary;
import com.churchos.church_erp.platform.service.PlatformAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@Tag(name = "Platform Admin", description = "Authenticated platform (super-admin) profile")
public class PlatformAdminController {

    private final PlatformAuthService platformAuthService;

    public PlatformAdminController(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated platform admin")
    public PlatformAdminSummary me(Principal principal) {
        return platformAuthService.getByEmail(principal.getName());
    }
}
