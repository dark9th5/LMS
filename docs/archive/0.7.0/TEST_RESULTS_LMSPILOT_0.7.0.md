# Test results — LMSPilot 0.7.0

Date: 2026-08-03

## Passed

```text
bash scripts/test-static.sh
STATIC TEST SUITE PASSED
Ran 50 tests — OK
```

The suite validates:

- JSON/YAML and repository layout.
- 18 services, 17 Flyway-backed services, no duplicate/destructive migrations.
- API Gateway route uniqueness and internal endpoint token protection.
- TypeScript/TSX syntax and UI permission/icon contracts.
- Account model, scoped RBAC, organizations, courses/classes/progress.
- Standalone exams, competitions, AI schema, documents and branding/news.
- CSV/XLSX import, license limits, immutable course versions and assignments.
- LDAP fallback, durable email outbox, integration probes, report license guard.
- Operations agent fixed allowlist, restore confirmation and maintenance policy.

## Environment-limited checks

- `npm ci --offline` failed because the local mirror/cache does not contain `undici-types-7.16.0.tgz`.
- Gradle wrapper cannot download its distribution while DNS/Internet access is unavailable.
- Docker E2E was not run because Docker Engine is unavailable.

These limitations mean the package is a full-source release candidate, not a production certification.
