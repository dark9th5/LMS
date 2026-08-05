# assessment-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8086`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi.
- **API chính:** `/api/v1/questions`, `/api/v1/exams`, `/api/v1/exam-sessions`, `/api/v1/competitions`, `/api/v1/assessment-assignments`
- **PostgreSQL schema:** `assessment`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=assessment`
- **Bảng sở hữu:** questions, exams, exam_questions, exam_sessions, demo_seed_history, assessment_contexts, competitions, competition_leaderboard, competition_rewards, reward_ledger, question_provenance, assessment_assignments, exam_session_events
- **Phụ thuộc:** PostgreSQL, Course, Enrollment, Organization

## Vị trí mã nguồn

- Controller/API: `AssessmentApi.kt, AssessmentAssignmentApi.kt, CompetitionApi.kt`
- Migration: `V1__assessment_schema.sql, V2__demo_seed_history.sql, V3__assessment_context_competition.sql, V4__question_provenance.sql, V5__assessment_assignments.sql, V6__resumable_exam_sessions.sql, V7__exam_session_learning_context.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:assessment-service:test --no-daemon
./gradlew :services:assessment-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
