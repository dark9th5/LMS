# grading-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8087`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Chấm tự động, chấm thủ công, lịch sử điểm, phản hồi và phúc khảo.
- **API chính:** `/api/v1/grades`, `/api/v1/grading`
- **PostgreSQL schema:** `grading`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=grading`
- **Bảng sở hữu:** grade_results, grade_revisions, grade_appeals
- **Phụ thuộc:** PostgreSQL, Assessment

## Vị trí mã nguồn

- Controller/API: `GradingApi.kt`
- Migration: `V1__grading_schema.sql, V2__grade_course_context.sql, V3__grade_history_and_appeals.sql, V4__grade_learning_context.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:grading-service:test --no-daemon
./gradlew :services:grading-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
