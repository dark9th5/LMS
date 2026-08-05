# assessment-service

- **Tên:** Assessment
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8086`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `assessment`

## Phạm vi sở hữu

Ngân hàng câu hỏi, bài kiểm tra trong khóa học, bài thi độc lập, phiên làm bài và cuộc thi.

## API chính

- `/api/v1/assessment-assignments`
- `/api/v1/competitions`
- `/api/v1/exam-sessions`
- `/api/v1/exams`
- `/api/v1/questions`
- `/internal/v1/assessment`

## Controller Java

- `src/main/java/**/AssessmentControllers.java`

## Bảng dữ liệu sở hữu

- `assessment_assignments`
- `assessment_contexts`
- `competition_leaderboard`
- `competition_rewards`
- `competitions`
- `demo_seed_history`
- `exam_questions`
- `exam_session_events`
- `exam_sessions`
- `exams`
- `question_provenance`
- `questions`
- `reward_ledger`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:assessment-service:bootRun
./gradlew :services:assessment-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
