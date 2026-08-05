# audit-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8091`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Nhật ký kiểm toán bất biến và xuất dữ liệu kiểm toán.
- **API chính:** `/api/v1/audit`
- **PostgreSQL schema:** `audit`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=audit`
- **Bảng sở hữu:** audit_entries
- **Phụ thuộc:** PostgreSQL, RabbitMQ

## Vị trí mã nguồn

- Controller/API: `AuditApi.kt`
- Migration: `V1__audit_schema.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:audit-service:test --no-daemon
./gradlew :services:audit-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
