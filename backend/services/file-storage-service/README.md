# file-storage-service

- **Tên:** File Storage
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8089`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `file_storage`

## Phạm vi sở hữu

Lưu trữ file, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa.

## API chính

- `/api/v1/files`
- `/internal/v1/files`
- `/public/v1/file-edit`

## Controller Java

- `src/main/java/**/FileEditingApi.java`
- `src/main/java/**/FileStorageApi.java`

## Bảng dữ liệu sở hữu

- `demo_seed_history`
- `file_access_grants`
- `file_edit_sessions`
- `file_versions_v2`
- `stored_files`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:file-storage-service:bootRun
./gradlew :services:file-storage-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
