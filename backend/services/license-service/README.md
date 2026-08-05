# license-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8090`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Kích hoạt giấy phép, entitlement và giới hạn tính năng triển khai.
- **API chính:** `/api/v1/license`
- **PostgreSQL schema:** `license`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=license`
- **Bảng sở hữu:** licenses
- **Phụ thuộc:** PostgreSQL

## Vị trí mã nguồn

- Controller/API: `LicenseApi.kt`
- Migration: `V1__license_schema.sql, V2__license_grace_period.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:license-service:test --no-daemon
./gradlew :services:license-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
