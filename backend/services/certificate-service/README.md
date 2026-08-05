# certificate-service

- **Tên:** Certificate
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8093`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `certificate`

## Phạm vi sở hữu

Mẫu chứng chỉ, cấp, tra cứu, thu hồi và cấp lại.

## API chính

- `/api/v1/certificates`
- `/public/v1/certificates`

## Controller Java

- `src/main/java/**/CertificateController.java`
- `src/main/java/**/PublicCertificateController.java`

## Bảng dữ liệu sở hữu

- `certificate_templates`
- `certificates`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:certificate-service:bootRun
./gradlew :services:certificate-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
