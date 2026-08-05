# operations-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8097`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Health tổng hợp, tác vụ vận hành, agent lease và lịch chạy nội bộ.
- **API chính:** `/api/v1/operations`
- **PostgreSQL schema:** `operations`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=operations`
- **Bảng sở hữu:** operation_jobs, operation_schedules
- **Phụ thuộc:** PostgreSQL và các service nghiệp vụ

## Vị trí mã nguồn

- Controller/API: `OperationsApi.kt`
- Migration: `V1__operations_schema.sql, V2__operation_agent_leases.sql, V3__operation_schedules.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:operations-service:test --no-daemon
./gradlew :services:operations-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
