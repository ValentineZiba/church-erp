# Contributing to church-erp

## Prerequisites

- Java 21
- MySQL and Redis running locally (or via `docker/docker-compose.dev.yml` once it exists)
- `./mvnw` (bundled — no separate Maven install needed)

## Getting started

```bash
./mvnw clean install       # build
./mvnw spring-boot:run     # run
./mvnw test                # test
```

Health check once running: `GET /actuator/health`

## Before opening a PR

1. `./mvnw test` passes locally — CI (`.github/workflows/ci.yml`) runs the same command against
   JDK 21 and must be green.
2. New/changed endpoints are documented via springdoc-openapi annotations, not left undocumented.
3. New modules follow the full scaffold: entity + repository + service + REST controller + DTO +
   mapper + Flyway migration + unit tests, generated together.

## Multi-tenancy — read before touching `tenant/`

Isolation is **database-per-tenant**, not a shared schema with a `tenant_id` column. Never write a
query, repository method, or cache key that assumes row-level tenant filtering.

- Every tenant-scoped call must run under a resolved `TenantContext`, cleared in a `finally` block.
  Code running outside a request (scheduled jobs, async tasks, message listeners) must set and
  clear the context explicitly — it does not propagate automatically.
- Any PR touching `tenant/` (context, resolver filter, routing DataSource, provisioning service,
  migration runner) should be reviewed line-by-line, not rubber-stamped. Call this out explicitly
  in the PR description.
- Tenant databases are created with `CREATE DATABASE tenant_<slug>` and migrated with a
  MySQL-flavored `DataSource` per tenant (see `TenantMigrationRunner`'s Javadoc for why Flyway's
  built-in multi-schema support isn't used instead).

## Module boundaries

Each business module (`membership`, `giving`, `accounting`, `events`, `groups`, `volunteers`,
`communications`, `reporting`, `automation`) is self-contained with its own `domain` /
`repository` / `service` / `dto` / `web` sub-packages. A module talks to another module **only**
through its public `service` interface — never by importing another module's `repository` or
`domain` internals directly.

## Migrations

Two independent Flyway locations: one for the control-plane DB, one for the tenant schema
(applied to every tenant DB). Never combine them.

## Conventions

- Entity ↔ DTO mapping via MapStruct, not hand-written mappers.
- Lombok for boilerplate (`@Getter`/`@Setter`/`@Builder`/`@RequiredArgsConstructor`).
- Standard servlet MVC controllers — Redis is the only reactive piece in this app, don't mix
  reactive controllers into the web layer.

## Security & compliance

Membership data includes sensitive categories (religious affiliation, safeguarding records,
financial giving history). Assume TLS-only in any config you write, add audit logging on anything
touching `members`, `donations`, or `ledger_entries`, and never log or print raw PII/financial
data in exception messages or debug logging.
