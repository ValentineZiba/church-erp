# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). This project
hasn't cut a first release yet (Phase 0), so everything so far lives under **Unreleased**.

## [Unreleased]

### Added

- Platform-admin authentication, dashboard shell, and the core tenant-isolation scaffolding
  (`TenantContext`, routing DataSource, control-plane schema).
- `TenantProvisioningService` for new-church signup/creation.
- `POST /api/platform/tenants` to trigger tenant provisioning.
- `TenantMigrationRunner` for per-tenant Flyway migrations.
- Tenant-scoped user authentication (`TenantUser`), separate JPA configs for the control-plane
  and tenant datasources, and the `V1` tenant users-table migration.
- Project scaffolding: README, MIT license, `CONTRIBUTING.md`, `CODEOWNERS`, pull request
  template, and issue templates.
- CI workflow running `./mvnw test` on JDK 21 for every push/PR to `main`/`master`.

### Fixed

- `TenantMigrationRunnerTest`'s in-memory H2 datasource now uses `MODE=MySQL`, so it correctly
  exercises the MySQL-only tenant migration SQL (`ENGINE=InnoDB`) instead of failing outright.

### Removed

- Roadmap PDF/markdown and design reference images from `docs/` (superseded by in-repo docs).
