# ai-service

- **Tên:** AI
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8094`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `ai`

## Phạm vi sở hữu

Model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra, review và import câu hỏi theo độ khó.

## API chính

- `/api/v1/ai`

## Controller Java

- `src/main/java/**/AiApi.java`
- `src/main/java/**/QuestionGenerationApi.java`

## Bảng dữ liệu sở hữu

- `ai_provider_configs`
- `question_generation_jobs`
- `question_generation_reviews`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:ai-service:bootRun
./gradlew :services:ai-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
