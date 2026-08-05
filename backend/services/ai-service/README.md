# ai-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8094`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Cấu hình model OpenAI-compatible; trích xuất PDF/DOCX; sinh, kiểm tra và duyệt câu hỏi theo độ khó.
- **API chính:** `/api/v1/ai`
- **PostgreSQL schema:** `ai`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=ai`
- **Bảng sở hữu:** ai_provider_configs, question_generation_jobs, question_generation_reviews
- **Phụ thuộc:** PostgreSQL, File Storage, Course, Assessment, AI provider

## Vị trí mã nguồn

- Controller/API: `AiApi.kt, QuestionGenerationApi.kt`
- Migration: `V2__question_generation_pipeline.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:ai-service:test --no-daemon
./gradlew :services:ai-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
