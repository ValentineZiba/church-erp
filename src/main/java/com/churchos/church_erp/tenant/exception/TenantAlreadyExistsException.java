package com.churchos.church_erp.tenant.exception;

public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String tenantSlug) {
        super("A tenant already exists for slug: " + tenantSlug);
    }
}
