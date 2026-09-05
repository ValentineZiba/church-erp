package com.churchos.church_erp.security.tenantuser.mapper;

import com.churchos.church_erp.security.tenantuser.domain.TenantUser;
import com.churchos.church_erp.security.tenantuser.dto.TenantUserSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantUserMapper {

    TenantUserSummary toSummary(TenantUser user);
}
