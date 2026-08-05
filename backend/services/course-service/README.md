# course-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8083`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Khóa học, chương, bài học, tài liệu, video, bài thực hành, thảo luận và phiên bản xuất bản.
- **API chính:** `/api/v1/categories`, `/api/v1/courses`, `/api/v1/discussions`
- **PostgreSQL schema:** `course`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=course`
- **Bảng sở hữu:** course_categories, courses, lessons, demo_seed_history, course_document_links, course_versions, discussion_threads, discussion_posts
- **Phụ thuộc:** PostgreSQL, File Storage, Enrollment

## Vị trí mã nguồn

- Controller/API: `CourseApi.kt, DiscussionApi.kt`
- Migration: `V1__course_schema.sql, V2__demo_seed_history.sql, V3__course_document_links.sql, V4__immutable_course_versions.sql, V5__course_discussions.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:course-service:test --no-daemon
./gradlew :services:course-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
