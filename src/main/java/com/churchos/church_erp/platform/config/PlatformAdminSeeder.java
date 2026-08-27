package com.churchos.church_erp.platform.config;

import com.churchos.church_erp.platform.domain.PlatformAdmin;
import com.churchos.church_erp.platform.repository.PlatformAdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Dev bootstrap only: seeds a single super-admin account on first startup so
 * there's something to log in with before a real invite/provisioning flow
 * exists. No-op once platform_admins has any row.
 */
@Component
@Slf4j
public class PlatformAdminSeeder implements ApplicationRunner {

    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;

    public PlatformAdminSeeder(
        PlatformAdminRepository platformAdminRepository,
        PasswordEncoder passwordEncoder,
        @Value("${churchos.platform.seed-admin.email}") String seedEmail,
        @Value("${churchos.platform.seed-admin.password}") String seedPassword
    ) {
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (platformAdminRepository.count() > 0) {
            return;
        }

        PlatformAdmin admin = PlatformAdmin.builder()
            .email(seedEmail)
            .passwordHash(passwordEncoder.encode(seedPassword))
            .fullName("Platform Admin")
            .build();
        platformAdminRepository.save(admin);
        log.info("Seeded initial platform admin account for {}", seedEmail);
    }
}
