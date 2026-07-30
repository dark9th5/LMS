# Architecture

## Product boundary

LMSPilot runs on customer-controlled infrastructure in a LAN. Runtime operation does not require public Internet. Customer data, files, exam answers, grades, reports, certificates, audit records and license state stay on the customer's infrastructure.

## Logical flow

```text
Browser / Desktop shell
        |
        v
API Gateway -- JWT, rate limit, access log, correlation ID
        |
        +--> Identity ---- Organization
        +--> Course ------ File Storage
        +--> Enrollment -- Learning
        +--> Assessment -- Grading
        +--> Reporting --- Certificate
        +--> License ----- Configuration
        +--> Audit ------- Notification
        +--> AI ---------- Integration adapters

All services publish/consume versioned events through RabbitMQ.
```

## Data ownership

Each service owns its schema and Flyway migrations. Services must not read another service's tables. Synchronous validation uses versioned internal REST endpoints protected by a service token; asynchronous state propagation uses domain events with event IDs and idempotent consumers.

The development compose file uses one PostgreSQL instance with isolated schemas and accounts. Production may split high-load services into independent PostgreSQL instances without changing the public contracts.

## Reliability rules

- Write operations accept an `Idempotency-Key` where duplicate submission would be harmful.
- Event envelopes contain `eventId`, `eventType`, `occurredAt`, `correlationId`, `producer` and `payload`.
- Consumers persist processed event IDs before acknowledging work.
- Notification, reporting and AI failures never roll back core learning, grading or enrollment transactions.
- Assessment, Reporting and File Storage are stateless at the application layer and can scale horizontally.

## Security layers

1. Gateway validates the token and applies rate limiting.
2. Every business service validates the token again and enforces method-level permissions.
3. Internal service calls use a separate service token.
4. Secrets are injected through environment variables or mounted secret files.
5. Databases, RabbitMQ and Redis are on an internal Docker network and are not published to the customer LAN by default.
