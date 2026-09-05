# Tenant-schema Flyway migrations

Applied to every tenant database (`tenant_<slug>`) by `TenantMigrationRunner`, never to the
control-plane database. Keep this location completely separate from
`db/migration/controlplane` — see `CLAUDE.md`.

`V1__create_users_table.sql` is the first migration here, landing alongside
`security/tenantuser` (per-tenant staff/volunteer/member accounts) — the first tenant-scoped
entity. No business module (membership, giving, accounting, ...) has entities yet; each one's
first migration lands here the same way, alongside its own entity/repository/service scaffold.
