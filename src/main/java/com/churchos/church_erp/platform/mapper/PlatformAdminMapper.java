package com.churchos.church_erp.platform.mapper;

import com.churchos.church_erp.platform.domain.PlatformAdmin;
import com.churchos.church_erp.platform.dto.PlatformAdminSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlatformAdminMapper {

    PlatformAdminSummary toSummary(PlatformAdmin admin);
}
