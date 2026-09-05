## Summary

<!-- What does this PR do, and why? -->

## Testing

<!-- How did you verify this? e.g. `./mvnw test`, manual testing steps -->

- [ ] `./mvnw test` passes locally

## Tenant isolation

<!-- Delete this section if this PR doesn't touch tenant/ or any tenant-scoped code path. -->

- [ ] This PR touches `tenant/` (context, resolver filter, routing DataSource, provisioning
      service, migration runner) and I'm flagging it for line-by-line review, not a rubber stamp.
- [ ] No query, repository method, or cache key assumes row-level tenant filtering — isolation
      stays database-per-tenant.
- [ ] Any tenant-scoped code that runs outside a request (scheduled job, async task, message
      listener) explicitly sets and clears `TenantContext` itself, in a `finally` block.

## Module boundaries

- [ ] New/changed modules talk to other modules only through their public `service` interface.
- [ ] New endpoints are documented via springdoc-openapi annotations.
- [ ] New Flyway migrations went into the correct location (control-plane vs. tenant schema —
      never both).

## Security & compliance

- [ ] No raw PII or financial data (members, donations, ledger entries) is logged or printed in
      exception messages or debug logging.
