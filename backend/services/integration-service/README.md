# integration-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8096`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Adapter cho Redis, SMTP, S3, ONLYOFFICE, họp trực tuyến và dịch vụ bên thứ ba.
- **API chính:** `/api/v1/integrations`
- **PostgreSQL schema:** `integration`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=integration`
- **Bảng sở hữu:** integration_adapters
- **Phụ thuộc:** PostgreSQL và nhà cung cấp bên thứ ba

## Vị trí mã nguồn

- Controller/API: `IntegrationApi.kt`
- Migration: `V1__integration_schema.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:integration-service:test --no-daemon
./gradlew :services:integration-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
