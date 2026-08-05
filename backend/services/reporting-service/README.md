# reporting-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8088`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Read model, dashboard, KPI, báo cáo học tập và xuất báo cáo theo lịch.
- **API chính:** `/api/v1/reports`, `/api/v1/dashboard`, `/internal/v1/reports/reminders`
- **PostgreSQL schema:** `reporting`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=reporting`
- **Bảng sở hữu:** report_events, learner_course_read_model, report_schedules, report_export_jobs
- **Phụ thuộc:** PostgreSQL, RabbitMQ, Enrollment

## Vị trí mã nguồn

- Controller/API: `KpiReportingApi.kt, ReminderReportingApi.kt, ReportingApi.kt, ScheduledReportingApi.kt`
- Migration: `V1__reporting_schema.sql, V2__scheduled_report_exports.sql, V3__reminder_due_index.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:reporting-service:test --no-daemon
./gradlew :services:reporting-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
