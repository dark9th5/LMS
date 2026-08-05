# enrollment-service

- **Tên:** Enrollment
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8084`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `enrollment`

## Phạm vi sở hữu

Giao khóa học, ghi danh, lộ trình học và phiên học trực tuyến.

## API chính

- `/api/v1/course-assignments`
- `/api/v1/enrollments`
- `/api/v1/learning-paths`
- `/api/v1/live-sessions`
- `/internal/v1/course-access`
- `/internal/v1/enrollments`

## Controller Java

- `src/main/java/**/AssignmentAndLiveApi.java`
- `src/main/java/**/EnrollmentApi.java`
- `src/main/java/**/LearningPathApi.java`

## Bảng dữ liệu sở hữu

- `class_instructors`
- `course_assignments_v2`
- `course_cohorts_v2`
- `enrollments`
- `learning_path_assignments`
- `learning_path_items`
- `learning_paths`
- `live_sessions`
- `training_classes`
- `user_learning_paths`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:enrollment-service:bootRun
./gradlew :services:enrollment-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
