package com.churchos.church_erp.platform.repository;

import com.churchos.church_erp.platform.domain.PlatformAdmin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {

    Optional<PlatformAdmin> findByEmail(String email);
}
