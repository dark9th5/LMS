# certificate-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8093`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Mẫu chứng chỉ, cấp, tra cứu, in, thu hồi và cấp lại chứng chỉ.
- **API chính:** `/api/v1/certificates`, `/public/v1/certificates`
- **PostgreSQL schema:** `certificate`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=certificate`
- **Bảng sở hữu:** certificates, certificate_templates
- **Phụ thuộc:** PostgreSQL

## Vị trí mã nguồn

- Controller/API: `CertificateApi.kt`
- Migration: `V1__certificate_schema.sql, V2__certificate_templates.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:certificate-service:test --no-daemon
./gradlew :services:certificate-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
