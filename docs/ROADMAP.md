# ChurchOS — Multi-Tenant Church Management ERP SaaS

> Markdown transcription of `docs/church-erp-roadmap.pdf` (prepared August 2026), kept here so
> Claude Code and other tools can read it without a PDF renderer. The PDF is the original; this file
> is for day-to-day reference. "ChurchOS" is a placeholder product name — replace with the real brand
> when decided.
>
> **Decision log:** the PDF recommends PostgreSQL 16. This project uses **MySQL** instead
> (`mysql-connector-j` + `flyway-mysql` in `pom.xml`, decided 2026-08-23). Everywhere below that says
> "Postgres", read "MySQL" unless noted otherwise — see `CLAUDE.md` for the concrete implications.

## 1. Vision & Product Positioning

ChurchOS is a SaaS platform that lets any church — from a 50-member congregation to a
10,000-member multi-campus network — sign up, get an isolated tenant environment in minutes, and
run every operational function of the church from one system: people, money, events, volunteers,
and communication.

To rival global players (Planning Center, Breeze ChMS, ChurchTrac, Rock RMS, Tithe.ly, Elvanto),
ChurchOS needs three things most regional competitors lack:

- **True enterprise-grade multi-tenancy** — each church's data is fully isolated (its own database),
  not just row-filtered in a shared table, which matters for financial and safeguarding data.
- **Fund accounting, not just "giving tracking"** — most SMB-focused competitors (Breeze,
  ChurchTrac) stop at donation logging; Rock RMS and enterprise players go further with real
  ledgers. ChurchOS should match that.
- **Africa-aware payments and messaging alongside global rails** — Stripe/PayPal for international
  reach, plus PayFast, Paystack, and Flutterwave for African markets, and SMS via Africa's
  Talking/Twilio.

## 2. Tech Stack Summary

| Layer | Choice | Notes |
|---|---|---|
| Language / Runtime | Java 21 (LTS) | |
| Framework | Spring Boot 3.x | Virtual threads (Project Loom) help with I/O-heavy multi-tenant request handling |
| Build tool | Maven (this project) | Gradle Kotlin DSL was the PDF's suggestion for multi-module speed; Maven is fine and is what's actually set up |
| Database | **MySQL** (PDF recommends PostgreSQL 16) | Spring Data JPA, Spring Security, Spring Validation, Spring Cache |
| Migrations | Flyway (`flyway-mysql`) | Separate migration sets for control-plane vs. tenant schemas |
| Caching / Sessions | Redis (reactive starter) | Tenant resolution cache, rate limiting, session store |
| Async messaging | RabbitMQ (or Kafka later) | Email/SMS sending, webhooks, report generation, audit events |
| Auth | Spring Security + JWT (+ optional OIDC via Keycloak) | Role hierarchy: Super Admin → Org Admin → Staff → Volunteer → Member |
| API style | REST (OpenAPI/Swagger) + optional GraphQL later | REST first; mobile app and integrations consume the same API |
| Frontend (admin console) | React + TypeScript (Next.js), or Vaadin for pure-Java | React/Next.js recommended for a modern SaaS UI and easier hiring |
| Mobile | Flutter or React Native, consuming the REST API | Phase 3 |
| Containerization / CI/CD | Docker + Docker Compose, GitHub Actions → SSH/Docker deploy to Contabo | Traefik as reverse proxy with automatic Let's Encrypt SSL; build, test, containerize, push to registry, deploy |
| Monitoring | Prometheus + Grafana + Loki | Metrics, dashboards, log aggregation |
| Testing | JUnit 5, Mockito, Testcontainers | Testcontainers spins up a real ephemeral database per test run |
| IDE | IntelliJ IDEA Ultimate | Native Spring, Docker, and Database tooling |
| AI pair-programmer | Claude Code (CLI + JetBrains plugin) | Scaffolding, refactors, tests, code review inside IntelliJ |

Architecture: **one control-plane database + one database per tenant.**

## 3. Multi-Tenancy Architecture: Database-per-Tenant

Database-per-tenant is the strongest isolation model — each church gets a fully separate database.
This matches higher-end enterprise SaaS products and is the right call given churches handle
sensitive data: giving records, safeguarding/child-protection information, and pastoral care notes.

### 3.1 Two-tier data model

- **Control-plane database** (`churchos_control`): one database for the whole platform. Holds the
  tenant registry, subscription/billing plans, super-admin users, and platform-wide audit logs.
- **Tenant databases** (`tenant_<uuid>` or `tenant_<slug>`): one per church, containing all
  operational data — members, giving, accounting, events, etc. Same schema across all tenants, kept
  in sync by Flyway migrations run against every tenant DB on deploy.

```
Control-Plane DB (churchos_control)
 |- tenants (id, name, slug, subdomain, db_host, db_name, db_credentials_ref, plan, status)
 |- subscriptions (tenant_id, plan_id, billing_cycle, stripe_customer_id, status)
 |- plans (id, name, price, feature_flags, tenant_limit_users, storage_quota)
 \- platform_admins (super-admin accounts)

Tenant DB: tenant_grace_chapel
 |- members, households, groups, tags
 |- donations, pledges, campaigns, payment_methods
 |- funds, accounts, ledger_entries, budgets
 |- events, registrations, check_ins
 |- volunteers, schedules
 |- messages, campaigns, templates
 \- roles, users, audit_log (per-tenant staff accounts)
```

### 3.2 Tenant resolution (request routing)

Each church gets a subdomain (`gracechapel.churchos.app`) or custom domain. On every incoming
request:

1. A `TenantResolverFilter` (a Spring `OncePerRequestFilter`) reads the `Host` header (or a custom
   `X-Tenant-ID` header for API/mobile clients).
2. It looks up the tenant in a Redis-backed cache (falling back to the control-plane DB on a cache
   miss) to get that tenant's database connection details.
3. It stores the resolved tenant identity in a `TenantContext` (a `ThreadLocal`, or a `ScopedValue` if
   adopting Java 21 virtual threads) for the duration of the request.
4. It clears the context in a `finally` block so nothing leaks across requests/threads.

### 3.3 Dynamic DataSource routing

Because each tenant has its own physical database, use Spring's `AbstractRoutingDataSource` backed
by a lazily-built, size-bounded cache of `HikariDataSource` instances (e.g. a Caffeine cache keyed by
tenant ID, with idle eviction so you don't hold thousands of open connection pools at once for
tenants that aren't currently active).

```java
public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant(); // returns tenant ID or null
    }
}
```

A `TenantDataSourceProvider` service resolves a tenant ID to a `DataSource`:

- Cache hit → reuse the pooled `HikariDataSource`.
- Cache miss → look up connection details in the control-plane DB, build a new `HikariDataSource`
  (small pool size per tenant, e.g. 2–5 connections), cache it, return it.

### 3.4 Tenant provisioning (new church signup)

A `TenantProvisioningService`, triggered when a new church completes signup or a super admin
creates a tenant:

1. Insert a row into `tenants` (control-plane DB) with status `PROVISIONING`.
2. Programmatically `CREATE DATABASE tenant_<slug>` via an admin JDBC connection.
3. Run all tenant-schema Flyway migrations against the new database.
4. Seed default reference data: default fund ("General Fund"), default roles (Admin, Staff,
   Volunteer, Member), default membership statuses, a welcome email template.
5. Create the first admin user for that church and send an invite email.
6. Flip tenant status to `ACTIVE`.
7. Publish a `TenantProvisionedEvent` to RabbitMQ so other modules (billing, analytics) react
   asynchronously.

Keep this whole flow idempotent and wrapped so a failure at any step leaves the tenant in a
clearly-flagged `FAILED` state rather than a half-created database — plan for a retry/cleanup job.

### 3.5 Keeping tenant schemas in sync

Since every tenant has a separate physical schema, a migration written today must be applied to
every tenant database, not just one:

- **Deploy-time migration runner** — a startup task (or a dedicated CI step) that iterates the tenant
  registry and runs Flyway against each tenant DB before the new application version starts serving
  traffic.
- Flyway's built-in multi-schema support isn't quite enough here since these are separate databases,
  not schemas within one database — write a small custom `TenantMigrationRunner` that loops over
  tenants and calls `Flyway.configure().dataSource(...).load().migrate()` per tenant.

At real scale (hundreds/thousands of tenants) this becomes the main operational cost of
database-per-tenant — budget engineering time for it, and consider batching/parallelizing
migrations.

## 4. Feature Set — Full Enterprise ERP Scope

Target scope to rival Planning Center, Breeze, ChurchTrac, Rock RMS, and Tithe.ly combined. Build
order is in Section 8; this is the target end-state.

### 4.1 Membership & CRM
People profiles with custom fields, households/families, membership status and history,
visitor/first-time-guest tracking and follow-up pipelines, tagging and smart segments (dynamic lists
like "givers who haven't given in 90 days"), group/ministry membership, background-check and
safeguarding record tracking for children's ministry volunteers, a searchable online member
directory (opt-in, privacy-controlled), and a self-service member portal for updating contact info
and viewing giving history.

### 4.2 Giving & Online Payments
One-time and recurring online giving, pledge campaigns and capital campaigns with progress
tracking, multiple funds per gift, text-to-give, kiosk/in-person giving stations, automatic
tax-deductible giving statements (PDF, emailed at year-end), and a payment gateway abstraction
layer supporting Stripe and PayPal (global) plus PayFast, Paystack, and Flutterwave (African
markets) so a church can pick the processor available in its country.

### 4.3 Fund Accounting (the enterprise differentiator)
A real chart of accounts, multiple funds with restricted/unrestricted designations, double-entry
ledger, budgets vs. actuals by fund/ministry, expense and reimbursement tracking with approval
workflows, bank account reconciliation, and exportable financial statements (balance sheet, income
statement, fund statement) — the feature area where most SMB competitors (Breeze, ChurchTrac)
fall short of Rock RMS-tier platforms.

### 4.4 Events & Check-In
Event creation with registration and ticketing (paid or free), capacity limits and waitlists, secure
children's check-in with printed name-tag/security-tag matching for pickup, kiosk mode
(tablet-friendly), and attendance analytics per service/event/campus.

### 4.5 Small Groups & Discipleship
Group directories (life groups, Bible studies), group-level attendance and curriculum tracking,
leader dashboards, and growth/discipleship pathway tracking for individual members.

### 4.6 Volunteers & Serving Teams
Position/role catalog per ministry, scheduling and shift assignment, automated reminder
notifications, shift swap/self-service, blockout dates, and skills/availability matching.

### 4.7 Communications
Mass email campaigns with templates and segmentation, SMS/text messaging (Twilio globally,
Africa's Talking for African numbers), push notifications for the companion mobile app, automated
workflows (e.g. "send a welcome sequence to every new visitor"), and delivery/engagement
analytics.

### 4.8 Multi-Campus / Multi-Site
Support for a single church organization running several physical (or online) campuses, with
campus-level reporting roll-up into an organization-level dashboard, shared or campus-specific
funds, and campus-scoped staff permissions — while still living inside that church's one tenant
database.

### 4.9 Facilities & Resource Booking
Room and equipment scheduling, conflict detection, and integration with the events module.

### 4.10 Reporting & Business Intelligence
Prebuilt dashboards (giving trends, attendance trends, member growth, volunteer engagement), a
custom report builder, scheduled report emails, and CSV/PDF export.

### 4.11 Workflow Automation
A trigger-action automation engine (e.g. "when a visitor attends twice, create a follow-up task for
their campus pastor"; "when a pledge is 30 days overdue, send a reminder") — this is the kind of
feature that separates enterprise ChMS platforms from basic ones.

### 4.12 Mobile App API & Member Portal
A documented REST API (OpenAPI/Swagger) powering both a staff-facing admin console and a
member-facing mobile app: giving, event registration, group check-in, directory lookup, and push
notifications.

### 4.13 Platform / Super-Admin Layer (the SaaS control plane itself)
Tenant management, subscription/plan management with usage-based feature gating (e.g. "Starter"
plan caps at 200 members; "Enterprise" plan unlocks multi-campus and API access), in-app billing
(Stripe Billing), platform-wide analytics, and a support/ticketing hook.

## 5. IntelliJ IDEA: Step-by-Step Project Setup

**Step 1 — Install prerequisites:** IntelliJ IDEA Ultimate, JDK 21 (Temurin/Eclipse Adoptium),
Docker Desktop (local infra + Testcontainers), the Claude Code CLI and its JetBrains plugin
(Settings → Plugins → Marketplace → "Claude Code").

**Step 2 — Create the project via Spring Initializr:** Group `com.churchos`, Artifact `church-erp`,
Java 21. *(Already done for this repo — see the real package name in Section 6 of `CLAUDE.md`.)*

**Step 3 — Configure JDK and code style:** Project SDK → JDK 21, language level 21; agree on a
formatter (Google Java Format or `.editorconfig`) so Claude Code's generated code matches style
automatically; enable Lombok annotation processing.

**Step 4 — Module/package skeleton:** Start as a **modular monolith** — one deployable Spring Boot
app, internally organized into clearly bounded packages per business module. Faster to build and
refactor early; extract a well-isolated package into a microservice later once real scaling
bottlenecks are known — far easier than un-tangling a premature microservice mesh.

**Step 5 — Local infrastructure with Docker Compose:** `docker/docker-compose.dev.yml` with the
database, Redis, RabbitMQ, and MailHog (dev email capture). Add an IntelliJ "Docker Compose" run
config plus a Compound config that starts infra + app together.

**Step 6 — Database tooling:** IntelliJ's Database tool window, pointed at the local control-plane
database, for schema browsing, query console, and ER diagrams without leaving the IDE.

**Step 7 — Version control:** Git init, `.gitignore`, initial commit, push to a private
GitHub/GitLab repo (unlocks CI/CD).

**Step 8 — First run:** Run `ChurchErpApplication.main()`, confirm it starts against local Docker
infra, hit `/actuator/health`.

**Step 9 — Wire up Claude Code:** Open a terminal (or the plugin panel) in the project root, run
`claude`. `CLAUDE.md` at the project root gives every session the right context automatically.

## 6. Project Structure (Folder Tree, as designed in the PDF)

```
church-erp/
|-- build.gradle.kts / pom.xml
|-- settings.gradle.kts
|-- CLAUDE.md
|-- docker/
|   |-- docker-compose.dev.yml       # local Postgres/MySQL, Redis, RabbitMQ, MailHog
|   |-- docker-compose.prod.yml      # production stack incl. Traefik
|   |-- Dockerfile                   # multi-stage build for the Spring Boot app
|   \-- traefik/traefik.yml
|-- .github/workflows/
|   |-- ci.yml                       # build + test on every PR
|   \-- deploy.yml                   # build image, push, deploy to Contabo
|-- db/
|   |-- control-plane/               # Flyway migrations for churchos_control
|   |   \-- V1__init_control_plane.sql
|   \-- tenant/                      # Flyway migrations applied to every tenant DB
|       |-- V1__init_core.sql
|       |-- V2__membership.sql
|       |-- V3__giving.sql
|       |-- V4__accounting.sql
|       |-- V5__events.sql
|       \-- V6__communications.sql
\-- src/main/
    |-- java/com/churchos/erp/
    |   |-- ChurchErpApplication.java
    |   |-- config/               # SecurityConfig, DataSourceConfig, RedisConfig, OpenApiConfig
    |   |-- tenant/               # TenantContext, TenantResolverFilter, TenantRoutingDataSource,
    |   |                         # TenantDataSourceProvider, TenantProvisioningService
    |   |-- common/               # BaseEntity, AuditableEntity, exceptions, PageResponse, utils
    |   |-- platform/             # super-admin: tenants, subscriptions, plans, billing
    |   |-- membership/ {domain, repository, service, dto, web}
    |   |-- giving/ {domain, gateway (StripeGatewayClient, PayFastGatewayClient, PaystackGatewayClient...), service, web}
    |   |-- accounting/ {domain (Fund, Account, LedgerEntry, Budget), service, web}
    |   |-- events/ {domain (Event, Registration, CheckIn), service, web}
    |   |-- groups/               # small groups / discipleship
    |   |-- volunteers/           # scheduling, serving teams
    |   |-- communications/ {domain (Campaign, MessageTemplate, MessageLog), channel (EmailChannel, SmsChannel), service}
    |   |-- reporting/            # dashboards, custom report builder
    |   |-- automation/           # workflow trigger/action engine
    |   \-- security/             # JWT, roles/permissions, auth controllers
    \-- resources/
        |-- application.yml
        |-- application-dev.yml
        |-- application-staging.yml
        \-- application-prod.yml
```

Each business package is self-contained (own `domain`/`repository`/`service`/`web`) and only talks to
other modules through their public service interfaces — never by reaching directly into another
module's repositories. This discipline is what makes it realistic to peel a module out into its own
microservice later without a rewrite.

> This repo's actual base package is `com.churchos.church_erp` (Spring Initializr's auto-generated
> name from the Maven artifact id), not `com.churchos.erp`. Follow the sub-package layout above
> under whichever base package the project actually uses — check `src/main/java` before assuming.

## 7. Claude Code + IntelliJ Workflow

**7.1** `CLAUDE.md` at the repo root describes module boundaries, the multi-tenancy rule ("never
write a JPA query or repository method that isn't automatically scoped to `TenantContext`'s active
DataSource — there is no `tenant_id` column to filter by, isolation comes from the routing DataSource
itself"), coding conventions (Lombok, DTO/mapper via MapStruct, test naming), and build/test/run
commands.

**7.2 Division of labor:** IntelliJ for structural navigation, refactoring with compile-time safety,
breakpoint debugging, and the Database tool window. Claude Code for scaffolding a new module's
boilerplate (entity + repository + service + controller + DTO + mapper + Flyway migration in one
pass), writing unit/integration tests against existing services, reviewing a diff before commit, and
drafting OpenAPI docs for new endpoints.

**7.3 Suggested loop for a new module:**
1. Describe the module in plain language ("add a Pledge Campaign feature to the giving module:
   entities, Flyway migration, service with create/list/close, REST controller, unit tests").
2. Review the generated diff before accepting.
3. Run the generated tests locally.
4. Iterate on failures in the same session.
5. Commit once green.

**7.4 Guardrails:** Keep multi-tenancy-critical code (`tenant/` package, the routing DataSource,
provisioning logic) under stricter human review — treat AI-generated changes there as drafts to
scrutinize line-by-line, since a bug in tenant isolation is the single highest-severity class of bug this
product can have.

## 8. Build Roadmap — Phases & Milestones

**Phase 0 — Foundation (Weeks 1–3):** Project skeleton, control-plane + tenant schema baseline
migrations, `TenantContext`/`TenantResolverFilter`/`TenantRoutingDataSource`, tenant provisioning
service with automated tests (Testcontainers spinning up a real throwaway database in CI),
authentication and role-based authorization, Docker Compose dev environment, CI pipeline
(build + test on every PR).

**Phase 1 — MVP Core (Weeks 4–10):** Membership & CRM, basic giving with one payment gateway
end-to-end (Stripe first — most documented; add PayFast/Paystack/Flutterwave once the pattern is
proven), events with registration (no check-in kiosk yet), email communications (transactional +
basic campaigns), member self-service portal, super-admin tenant/subscription console. Deploy to
staging on Contabo — this is the version for the first 3–5 pilot churches.

**Phase 2 — Enterprise Differentiators (Weeks 11–18):** Fund accounting (chart of accounts, ledger,
budgets, financial statements), volunteer scheduling, small groups, multi-campus support,
additional payment gateways (PayFast, Paystack, Flutterwave) and SMS (Twilio, Africa's Talking),
children's check-in with security tags, facilities/resource booking. Closes the gap with Rock
RMS-tier competitors.

**Phase 3 — Scale & Intelligence (Weeks 19–26):** Reporting/BI dashboards and custom report
builder, workflow automation engine, public REST API + API keys for third-party integrations,
companion mobile app (Flutter/React Native), push notifications, performance work on the
tenant-routing layer (connection pool tuning, migration-runner parallelization) as tenant count
grows.

**Phase 4 — Go-to-Market Hardening (Ongoing):** Billing automation (Stripe Billing subscriptions,
dunning, plan upgrades/downgrades), usage-based feature gating, in-app onboarding flows, SOC
2-style security hardening and audit logging, backup/disaster-recovery drills, a public status page.

## 9. Deployment on Contabo

**9.1 Server setup:** Contabo VPS (8 vCPU / 24–32 GB RAM to start; scale up or split app/DB across
two VPS instances as tenant count grows). Docker Engine + Compose plugin. Lock down SSH
(key-only auth, non-root user, ufw allowing only 22/80/443).

**9.2 Reverse proxy & wildcard SSL:** Traefik in front of the app container. Wildcard DNS
(`*.churchos.app` → Contabo IP) + Traefik's Let's Encrypt DNS challenge for an auto-renewing
wildcard TLS cert covering every tenant subdomain.

**9.3 Database hosting strategy:** Run the database in Docker (or as a native service) on the same
VPS to start, one tenant = one database on that instance/cluster. As you grow, introduce tiering:
lower-cost plans stay on the shared instance, an "Enterprise" plan tenant gets its own dedicated
DB container/VPS — a marketable differentiator, since few SMB-focused ChMS platforms offer
dedicated database isolation as a plan tier.

**9.4 CI/CD pipeline:** GitHub Actions: push to main → run tests → build Docker image → push to
registry → SSH into Contabo → `docker compose pull && docker compose up -d` → run the tenant
migration runner → smoke-test the health endpoint. Keep a manual approval gate on deploy until
confident in the pipeline.

**9.5 Backups:** Nightly dump per tenant database (or WAL/binlog-based continuous backup for
larger tenants), shipped to Contabo Object Storage or an S3-compatible bucket, with a documented
and *tested* restore procedure — a backup you haven't restored from is not a backup.

**9.6 Monitoring:** Prometheus scraping `/actuator/prometheus`, Grafana dashboards for request
latency/error rate/DB pool saturation, Loki for centralized log search across containers. Alert on
tenant provisioning failures and connection-pool exhaustion specifically — the failure modes unique
to this architecture.

## 10. Security, Compliance & Data Protection

Church membership data includes sensitive categories (religious affiliation is itself sensitive
personal data under most privacy laws, plus safeguarding/child-protection records and financial
giving history), so build compliance in from the start rather than retrofitting it:

- Encrypt data at rest (disk/filesystem-level) and in transit (TLS everywhere, including between the
  app and each tenant database).
- Implement per-tenant audit logging of who accessed or changed what.
- Support data export and account/data deletion requests — both POPIA (South Africa) and GDPR
  (if there will be European members) give data subjects these rights.
- Enforce strong password/MFA policies for staff accounts given they can access financial and
  child-safety data.

## 11. Appendix — Recommended Libraries & Tools

| Purpose | Library |
|---|---|
| Object mapping (entity ↔ DTO) | MapStruct |
| API documentation | springdoc-openapi |
| PDF generation (giving statements, reports) | OpenPDF or Apache PDFBox |
| Excel/CSV export | Apache POI |
| Payment gateways | Stripe Java SDK, PayFast REST integration, Paystack Java SDK, Flutterwave |
| SMS | Twilio SDK (global), Africa's Talking SDK (Africa) |
| Email | Spring Mail + a transactional provider (SendGrid, Postmark, or Amazon SES) |
| Feature flags / plan gating | Custom (`plans.feature_flags` JSON column), or Unleash |
| Testing | JUnit 5, Mockito, Testcontainers, RestAssured for API tests |
| Code quality | Checkstyle/SpotBugs, SonarQube (self-hosted or SonarCloud) |

## Next Step

Start with Phase 0. Once the tenant provisioning flow and the routing DataSource are working and
covered by integration tests (the highest-risk, hardest-to-retrofit part of the whole system), the
rest of the modules follow a repeatable pattern that Claude Code can help scaffold quickly inside
IntelliJ.
