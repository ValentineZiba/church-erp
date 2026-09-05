# Security Policy

## Supported Versions

church-erp is pre-release (Phase 0) — there are no tagged releases yet. Security fixes land on
`master`/`main` only; there is no backport policy until a first release ships.

## Reporting a Vulnerability

**Please do not open a public issue for security vulnerabilities.**

Report privately using one of these channels:

- [GitHub private security advisory](https://github.com/ValentineZiba/church-erp/security/advisories/new)
  (preferred — keeps the report and any discussion out of public view until a fix is ready)
- Email valentineziba1@gmail.com

Include as much detail as you can: the affected component, reproduction steps, and the potential
impact. You should get an initial response within a few days.

## What to expect

1. We confirm receipt and try to reproduce the issue.
2. We work on a fix, coordinating with you on disclosure timing if you'd like credit.
3. We publish a fix and, where appropriate, a GitHub security advisory describing the issue and
   affected versions.

## Scope and what matters most here

This is a multi-tenant church management SaaS with database-per-tenant isolation. The
highest-severity class of bug is anything that lets one tenant's data (or `TenantContext`) leak
into another tenant's request or database connection — treat and report these with priority even
if the practical impact seems small.

Membership data includes sensitive categories: religious affiliation, safeguarding/child-protection
records, and financial giving history. Reports involving exposure of this data, authentication
bypass, or injection vulnerabilities (SQL, etc.) are treated as high severity.

When reporting, please avoid including real member/donor data or other PII in the report itself —
describe the vulnerability with synthetic or redacted examples where possible.
