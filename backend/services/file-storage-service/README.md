# file-storage-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8089`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Tải lên/tải xuống, quyền truy cập, phiên bản, xem PDF/DOCX/video và phiên chỉnh sửa.
- **API chính:** `/api/v1/files`, `/internal/v1/files`, `/public/v1/file-edit`
- **PostgreSQL schema:** `file_storage`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=file_storage`
- **Bảng sở hữu:** stored_files, demo_seed_history, file_versions_v2, file_edit_sessions, file_access_grants
- **Phụ thuộc:** PostgreSQL, S3 tùy chọn, ONLYOFFICE/Collabora tùy chọn

## Vị trí mã nguồn

- Controller/API: `FileEditingApi.kt, FileStorageApi.kt, InternalFileApi.kt`
- Migration: `V1__file_storage_schema.sql, V2__demo_seed_history.sql, V3__file_versions_and_edit_sessions.sql, V4__file_access_grants.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:file-storage-service:test --no-daemon
./gradlew :services:file-storage-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
