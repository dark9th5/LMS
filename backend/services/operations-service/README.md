# operations-service

- **Tên:** Operations
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8097`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `operations`

## Phạm vi sở hữu

Health tổng hợp, job vận hành, lịch chạy và agent lease.

## API chính

- `/api/v1/operations`
- `/internal/v1/operations/jobs`

## Controller Java

- `src/main/java/**/InternalOperationsAgentController.java`
- `src/main/java/**/OperationsController.java`

## Bảng dữ liệu sở hữu

- `operation_jobs`
- `operation_schedules`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:operations-service:bootRun
./gradlew :services:operations-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
