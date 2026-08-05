# configuration-service

- **Tên:** Configuration
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8095`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `configuration`

## Phạm vi sở hữu

Thông tin hệ thống, thương hiệu, logo, ảnh nền đăng nhập và dịch vụ ngoài.

## API chính

- `/api/v1/branding`
- `/api/v1/configuration`
- `/api/v1/external-services`
- `/api/v1/external-services/{id}`
- `/api/v1/external-services/{id}/test`
- `/public/v1/branding`
- `/public/v1/branding/assets/{kind}`
- `/public/v1/configuration`

## Controller Java

- `src/main/java/**/CustomizationController.java`
- `src/main/java/**/ProductConfigurationController.java`

## Bảng dữ liệu sở hữu

- `branding_profiles`
- `external_service_configs`
- `product_configuration`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:configuration-service:bootRun
./gradlew :services:configuration-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
