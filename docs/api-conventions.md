# API Conventions

- Public APIs are under `/api/v1`.
- Internal service APIs are under `/internal/v1` and require `X-Service-Token`.
- Mutating requests that may be retried use `Idempotency-Key`.
- Every response carries or echoes `X-Correlation-Id`.
- Validation errors use HTTP 400, authentication errors 401, authorization errors 403, missing resources 404, conflict/idempotency errors 409, and unavailable optional capabilities 503.

Error response:

```json
{
  "timestamp": "2026-07-30T05:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "path": "/api/v1/courses",
  "correlationId": "...",
  "fieldErrors": {"name": "Không được để trống"}
}
```
