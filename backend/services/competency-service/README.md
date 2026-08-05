# competency-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8098`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Khung năng lực, hồ sơ năng lực, đánh giá khoảng thiếu và ánh xạ khóa học.
- **API chính:** `/api/v1/competencies`
- **PostgreSQL schema:** `competency`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=competency`
- **Bảng sở hữu:** competencies, competency_profiles, competency_profile_requirements, user_competency_profiles, user_competency_assessments, course_competency_maps
- **Phụ thuộc:** PostgreSQL, License

## Vị trí mã nguồn

- Controller/API: `CompetencyApi.kt`
- Migration: `V1__competency_framework.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:competency-service:test --no-daemon
./gradlew :services:competency-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
