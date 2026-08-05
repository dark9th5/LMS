# course-service

- **Tên:** Course
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8083`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `course`

## Phạm vi sở hữu

Khóa học, danh mục, chương/bài học, phiên bản xuất bản và thảo luận.

## API chính

- `/api/v1/categories`
- `/api/v1/courses`
- `/api/v1/discussions`
- `/internal/v1/courses`

## Controller Java

- `src/main/java/**/CategoryController.java`
- `src/main/java/**/CourseController.java`
- `src/main/java/**/DiscussionApi.java`
- `src/main/java/**/InternalCourseController.java`

## Bảng dữ liệu sở hữu

- `course_categories`
- `course_document_links`
- `course_versions`
- `courses`
- `demo_seed_history`
- `discussion_posts`
- `discussion_threads`
- `lessons`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:course-service:bootRun
./gradlew :services:course-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
