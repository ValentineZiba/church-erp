package com.churchos.church_erp.tenant.provisioning;

/**
 * Published once a tenant flips to {@code ACTIVE}. An in-process stand-in for the
 * {@code TenantProvisionedEvent} on RabbitMQ described in docs/ROADMAP.md §3.4 step 7 — there's
 * no AMQP dependency wired up yet. A listener that forwards this onto RabbitMQ once that's built
 * is a drop-in addition; nothing here needs to change.
 */
public record TenantProvisionedEvent(String tenantSlug) {
}
