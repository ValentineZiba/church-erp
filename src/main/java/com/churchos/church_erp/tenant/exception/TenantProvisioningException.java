package com.churchos.church_erp.tenant.exception;

/**
 * Thrown when any step of provisioning a tenant fails. By the time this is thrown, the tenant's
 * registry row has already been flipped to {@code FAILED} — the caller doesn't need to do that
 * cleanup, only decide whether/when to retry (retrying is safe: provisioning is idempotent).
 */
public class TenantProvisioningException extends RuntimeException {

    public TenantProvisioningException(String tenantSlug, Throwable cause) {
        super("Provisioning failed for tenant '" + tenantSlug + "', marked FAILED: " + cause.getMessage(), cause);
    }
}
