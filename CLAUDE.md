# ChurchOS — church-erp

Multi-tenant Church Management ERP SaaS. Java 21 + Spring Boot, database-per-tenant isolation,
enterprise ERP feature scope (membership, giving, fund accounting, events, volunteers, comms,
multi-campus). Full product/architecture spec: **`docs/ROADMAP.md`** (markdown transcription of
`docs/church-erp-roadmap.pdf`, the original source doc). Read that file before scaffolding any new
module — this file only holds the operational rules Claude Code needs on every task.

## Current status

Fresh Spring Initializr skeleton (Phase 0, not started). Nothing tenant-aware exists yet — no
`TenantContext`, no routing DataSource, no control-plane schema. Don't assume any of Section 3's
infrastructure is built; check `src/main/java` before referencing a class.

## Tech stack (as actually configured in `pom.xml` — this overrides the PDF where they differ)

- Java 21, Spring Boot (parent `4.1.1`), Maven (`./mvnw`, not Gradle).
- **Database: MySQL** (`mysql-connector-j` + `flyway-mysql`) — the PDF recommends PostgreSQL 16;
  the team decided to use MySQL instead (decided 2026-08-23). Write all SQL, Flyway migrations, and
  JPA dialect assumptions for MySQL, not Postgres.
- Redis via `spring-boot-starter-data-redis-reactive` — reactive client (Lettuce), not the blocking
  `spring-boot-starter-data-redis`. Keep tenant-cache and session code reactive/non-blocking to match.
- Spring Security, Spring Validation, Spring Web MVC (`spring-boot-starter-webmvc` — despite the
  reactive Redis starter, the web layer is standard servlet MVC, not WebFlux). Don't mix reactive
  controllers into this app; only the Redis client is reactive.
- Lombok is on the classpath and annotation processing is wired in the Maven compiler plugin —
  use it for boilerplate (`@Getter`/`@Setter`/`@Builder`/`@RequiredArgsConstructor`) rather than
  hand-written getters/setters/constructors.
- Base package: `com.churchos.church_erp` (auto-derived from the Maven artifact id, *not*
  `com.churchos.erp` as written in the PDF's folder tree). Follow the PDF's sub-package layout
  (`tenant/`, `common/`, `platform/`, `membership/`, `giving/`, `accounting/`, `events/`, `groups/`,
  `volunteers/`, `communications/`, `reporting/`, `automation/`, `security/`) under this base package.

## Multi-tenancy rules — the highest-severity area in this codebase

- Isolation is **database-per-tenant**, not a shared schema with a `tenant_id` column. There is no
  `tenant_id` to filter by — never write a query, repository method, or cache key that assumes
  row-level tenant filtering. Isolation comes entirely from which physical database the connection
  is routed to for the current request.
- Every tenant-scoped JPA repository/service call must run under a resolved `TenantContext`. If
  you're writing code that runs outside a request (a scheduled job, an async task, a message
  listener), you must explicitly set and clear the tenant context yourself — it does not propagate
  automatically across threads unless the code says so.
- Always clear tenant context in a `finally` block. A leaked tenant context is a data-leak bug across
  churches, not a cosmetic one.
- Treat any change under `tenant/` (context, resolver filter, routing DataSource, provisioning
  service) as a draft to be reviewed line-by-line, not auto-accepted — flag this explicitly when you
  generate code there instead of presenting it as routine.
- Because MySQL is the engine, tenant provisioning creates databases with `CREATE DATABASE
  tenant_<slug>` (not schemas), and the `TenantMigrationRunner` must run Flyway per tenant using a
  MySQL-flavored `DataSource` per tenant (see `docs/ROADMAP.md` §3.5 for why Flyway's built-in
  multi-schema support isn't sufficient here).

## Module boundaries

Each business package (`membership`, `giving`, `accounting`, `events`, `groups`, `volunteers`,
`communications`, `reporting`, `automation`) is self-contained: its own `domain` / `repository` /
`service` / `dto` / `web` sub-packages. A module talks to another module **only** through its public
`service` interface — never by importing another module's `repository` or `domain` internals
directly. This is what keeps it realistic to later extract a module into its own microservice.

## Conventions

- Entity ↔ DTO mapping via MapStruct, not hand-written mappers.
- Migrations: two independent Flyway locations — one for the control-plane DB, one for the tenant
  schema (applied to every tenant DB). Never combine them.
- New module scaffold = entity + repository + service + REST controller + DTO + mapper + Flyway
  migration + unit tests, generated together, per `docs/ROADMAP.md` §7.3's suggested loop.
- API surface is REST + OpenAPI/Swagger (springdoc-openapi) — document new endpoints as you add
  them, don't defer it.

## Commands

- Build: `./mvnw clean install`
- Run: `./mvnw spring-boot:run`
- Test: `./mvnw test`
- Health check once running: `GET /actuator/health`
- Local infra (once `docker/docker-compose.dev.yml` exists per the roadmap): MySQL, Redis,
  RabbitMQ, MailHog.

## Division of labor with IntelliJ

Use Claude Code for scaffolding module boilerplate in one pass, writing tests against existing
services, reviewing diffs before commit, and drafting OpenAPI docs. Use IntelliJ directly for
structural refactors (rename/extract/move), breakpoint debugging, and the Database tool window for
inspecting tenant data — don't reach for Claude Code to do what IntelliJ's refactoring tools do with
compile-time safety.

## MCP servers

Configured in `.mcp.json`, both opt-in (nothing breaks if their env vars are unset — Claude Code just
won't be able to start that server):

- **`mysql`** (`@benborla29/mcp-server-mysql`, via `npx`) — read-only introspection of a local dev
  database (write operations explicitly disabled via `ALLOW_*_OPERATION=false`). Lets Claude Code
  answer schema questions ("what columns does `members` have") by querying directly instead of
  reconstructing state from migration files. Set `DB_HOST`/`DB_PORT`/`DB_USER`/`DB_PASSWORD`/
  `DB_NAME` in your shell/env before starting Claude Code — defaults target `127.0.0.1:3306` /
  `churchos_control` if unset. Because this is database-per-tenant, it only ever sees one database at
  a time — point `DB_NAME` at whichever tenant DB you're debugging.
- **`github`** (official `ghcr.io/github/github-mcp-server`, via `docker run`) — issue/PR-aware
  tasks. Requires `GITHUB_PERSONAL_ACCESS_TOKEN` in your env and Docker running locally.

Neither is required for day-to-day scaffolding work; Claude Code's built-in file tools cover that.

## Security & compliance posture

Membership data includes sensitive categories (religious affiliation, safeguarding/child-protection
records, financial giving history). Default to: TLS-only assumptions in any config you write, audit
logging on anything touching `members`, `donations`, or `ledger_entries`, and never log or print raw
PII/financial data in exception messages or debug logging. See `docs/ROADMAP.md` §10 for the full
POPIA/GDPR posture.
