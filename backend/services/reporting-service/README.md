# reporting-service

- **Tên:** Reporting
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8088`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `reporting`

## Phạm vi sở hữu

Read model báo cáo, dashboard, KPI, xuất báo cáo và lịch báo cáo.

## API chính

- `/api/v1/reports`
- `/api/v1/reports/kpis`
- `/internal/v1/reports/reminders`

## Controller Java

- `src/main/java/**/InternalReminderReportingController.java`
- `src/main/java/**/KpiReportingController.java`
- `src/main/java/**/ReportingController.java`
- `src/main/java/**/ScheduledReportingController.java`

## Bảng dữ liệu sở hữu

- `learner_course_read_model`
- `report_events`
- `report_export_jobs`
- `report_schedules`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:reporting-service:bootRun
./gradlew :services:reporting-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
