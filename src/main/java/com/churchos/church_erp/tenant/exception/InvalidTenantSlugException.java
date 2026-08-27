package com.churchos.church_erp.tenant.exception;

public class InvalidTenantSlugException extends RuntimeException {

    public InvalidTenantSlugException(String tenantSlug, String pattern) {
        super("Invalid tenant slug '" + tenantSlug + "': must match " + pattern);
    }
}
