# ChurchOS — church-erp

Multi-tenant Church Management ERP SaaS, built with database-per-tenant isolation and an
enterprise ERP feature scope: membership, giving, fund accounting, events, volunteers,
communications, and multi-campus support.

## Status

Early stage (Phase 0). Tenant-scoped auth, per-tenant JPA configuration, and tenant
provisioning/migration infrastructure are in place; most business modules
(membership, giving, accounting, events, groups, volunteers, communications, reporting,
automation) are not yet built.

## Tech stack

- Java 21, Spring Boot, Maven (`./mvnw`)
- MySQL (`mysql-connector-j`, `flyway-mysql`) — one physical database per tenant
- Redis (`spring-boot-starter-data-redis-reactive`, Lettuce)
- Spring Security, Spring Validation, Spring Web MVC
- MapStruct for entity ↔ DTO mapping
- REST + OpenAPI/Swagger (springdoc-openapi)

## Multi-tenancy

Isolation is database-per-tenant, not a shared schema with a `tenant_id` column. Each
tenant gets its own MySQL database (`tenant_<slug>`), provisioned and migrated
independently via a per-tenant Flyway runner. Tenant-scoped code must run under a
resolved `TenantContext`, which is cleared after every request.

## Commands

```bash
./mvnw clean install       # build
./mvnw spring-boot:run     # run
./mvnw test                 # test
```

Health check once running: `GET /actuator/health`

## Module boundaries

Each business module (`membership`, `giving`, `accounting`, `events`, `groups`,
`volunteers`, `communications`, `reporting`, `automation`) is self-contained with its own
`domain` / `repository` / `service` / `dto` / `web` sub-packages, and is accessed by other
modules only through its public `service` interface.
