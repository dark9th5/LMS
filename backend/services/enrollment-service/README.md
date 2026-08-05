# enrollment-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8084`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Giao khóa học trực tiếp cho học viên, ghi danh, hạn học, phiên học trực tuyến và lộ trình học.
- **API chính:** `/api/v1/enrollments`, `/api/v1/course-assignments`, `/api/v1/live-sessions`, `/api/v1/learning-paths`
- **PostgreSQL schema:** `enrollment`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=enrollment`
- **Bảng sở hữu:** training_classes (tương thích dữ liệu cũ, không có UI lớp học), class_instructors, enrollments, course_cohorts_v2, course_assignments_v2, live_sessions, learning_paths, learning_path_items, learning_path_assignments, user_learning_paths
- **Phụ thuộc:** PostgreSQL, Course, Organization, Learning

## Vị trí mã nguồn

- Controller/API: `AssignmentAndLiveApi.kt, EnrollmentApi.kt, LearningPathApi.kt`
- Migration: `V1__enrollment_schema.sql, V3__course_cohorts_and_deadlines.sql, V4__live_sessions.sql, V5__learning_paths.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:enrollment-service:test --no-daemon
./gradlew :services:enrollment-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
