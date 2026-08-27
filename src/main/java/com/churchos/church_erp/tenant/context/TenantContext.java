package com.churchos.church_erp.tenant.context;

/**
 * Holds the resolved tenant slug for the duration of the current request thread.
 *
 * <p>Does not propagate automatically to any other thread. Code that runs outside a request
 * (a scheduled job, an async task, a message listener) must call {@link #setCurrentTenantSlug}
 * itself before touching tenant-scoped data, and must clear it in a {@code finally} block —
 * a leaked tenant context is a cross-tenant data leak, not a cosmetic bug.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT_SLUG = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenantSlug(String tenantSlug) {
        CURRENT_TENANT_SLUG.set(tenantSlug);
    }

    public static String getCurrentTenantSlug() {
        return CURRENT_TENANT_SLUG.get();
    }

    public static void clear() {
        CURRENT_TENANT_SLUG.remove();
    }
}
