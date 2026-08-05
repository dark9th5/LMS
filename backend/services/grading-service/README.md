# grading-service

- **Tên:** Grading
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8087`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `grading`

## Phạm vi sở hữu

Chấm tự động, chấm thủ công, lịch sử điểm và phúc khảo.

## API chính

- `/api/v1/grades`

## Controller Java

- `src/main/java/**/GradeController.java`

## Bảng dữ liệu sở hữu

- `grade_appeals`
- `grade_results`
- `grade_revisions`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:grading-service:bootRun
./gradlew :services:grading-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
