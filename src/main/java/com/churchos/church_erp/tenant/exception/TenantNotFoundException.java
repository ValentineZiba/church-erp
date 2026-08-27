package com.churchos.church_erp.tenant.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String tenantSlug) {
        super("Unknown or inactive tenant: " + tenantSlug);
    }
}
