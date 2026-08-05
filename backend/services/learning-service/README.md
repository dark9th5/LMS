# learning-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8085`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Tiến độ học, mở nội dung, hoàn thành bài học, nộp/chấm bài thực hành và xAPI.
- **API chính:** `/api/v1/learning`, `/api/v1/learning/assignments`, `/api/v1/xapi/statements`
- **PostgreSQL schema:** `learning`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=learning`
- **Bảng sở hữu:** course_progress, lesson_progress, idempotency_records, assignment_submissions, xapi_statements
- **Phụ thuộc:** PostgreSQL, Course, Enrollment, File Storage

## Vị trí mã nguồn

- Controller/API: `AssignmentSubmissionApi.kt, LearningApi.kt, XapiApi.kt`
- Migration: `V1__learning_schema.sql, V2__course_version_and_assignments.sql, V3__xapi_learning_record_store.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:learning-service:test --no-daemon
./gradlew :services:learning-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
