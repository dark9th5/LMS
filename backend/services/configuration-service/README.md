# configuration-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8095`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Thông tin hệ thống, thương hiệu, logo, màu sắc, ảnh nền đăng nhập và cấu hình dịch vụ ngoài.
- **API chính:** `/api/v1/configuration`, `/api/v1/branding`, `/api/v1/external-services`, `/public/v1/configuration`, `/public/v1/branding`
- **PostgreSQL schema:** `configuration`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=configuration`
- **Bảng sở hữu:** product_configuration, branding_profiles, external_service_configs
- **Phụ thuộc:** PostgreSQL, File Storage

## Vị trí mã nguồn

- Controller/API: `ConfigurationApi.kt, CustomizationApi.kt`
- Migration: `V1__configuration_schema.sql, V2__branding_and_external_services.sql, V3__default_branding.sql, V4__branding_theme_studio.sql, V5__diverse_theme_catalog.sql, V6__soft_spectrum_default.sql, V7__unified_design_system.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:configuration-service:test --no-daemon
./gradlew :services:configuration-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
