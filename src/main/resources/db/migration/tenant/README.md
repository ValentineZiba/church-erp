# Tenant-schema Flyway migrations

Applied to every tenant database (`tenant_<slug>`) by `TenantMigrationRunner`, never to the
control-plane database. Keep this location completely separate from
`db/migration/controlplane` — see `CLAUDE.md`.

Empty for now: no business module (membership, giving, accounting, ...) has entities yet. The
first migration here will land alongside that module's first entity/repository/service scaffold.
