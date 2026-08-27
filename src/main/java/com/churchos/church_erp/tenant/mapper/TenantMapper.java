package com.churchos.church_erp.tenant.mapper;

import com.churchos.church_erp.tenant.domain.Tenant;
import com.churchos.church_erp.tenant.dto.TenantSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantSummary toSummary(Tenant tenant);
}
