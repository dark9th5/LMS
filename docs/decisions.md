# Architecture Decision Record

## ADR-001 — One role-aware web portal

Use a single Next.js portal with route visibility and data scope based on claims. This avoids maintaining three duplicated frontends while preserving the three BA actors.

## ADR-002 — Schema-per-service for the one-server package

The BA allows a separate database or schema per service. The default package uses separate PostgreSQL schemas and accounts to reduce server cost. High-load customers can move a schema to another instance.

## ADR-003 — JWT plus refresh token

Access tokens are short lived. Refresh tokens are opaque, hashed in the Identity database and revocable. The web portal stores both in HttpOnly cookies through a BFF route, not browser local storage.

## ADR-004 — RabbitMQ topic exchange

Use a versioned topic exchange for domain events and dead-letter queues for failed consumers. The Outbox pattern is included in the implementation roadmap for every critical producer before production acceptance.

## ADR-005 — Local filesystem first, adapter-ready storage

The default File Storage service writes to a mounted local/NAS path. The storage interface allows a MinIO/S3-compatible adapter without changing Course, Assignment or Certificate logic.

## ADR-006 — Optional local AI

AI is disabled by default. When enabled, it calls a local OpenAI-compatible endpoint. AI output is always a draft and can never publish directly to the question bank.
