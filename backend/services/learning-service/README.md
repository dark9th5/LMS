# learning-service

- **Tên:** Learning
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8085`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `learning`

## Phạm vi sở hữu

Tiến độ học, hoàn thành bài học, nộp/chấm bài thực hành và xAPI.

## API chính

- `/api/v1/learning`
- `/api/v1/learning/assignments`
- `/api/v1/xapi/statements`
- `/internal/v1/learning`

## Controller Java

- `src/main/java/**/AssignmentSubmissionApi.java`
- `src/main/java/**/LearningApi.java`
- `src/main/java/**/XapiApi.java`

## Bảng dữ liệu sở hữu

- `assignment_submissions`
- `course_progress`
- `idempotency_records`
- `lesson_progress`
- `xapi_statements`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:learning-service:bootRun
./gradlew :services:learning-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
