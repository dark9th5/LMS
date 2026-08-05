# notification-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8092`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Thông báo trong hệ thống, email, tin tức, mẫu và nhắc hạn.
- **API chính:** `/api/v1/notifications`, `/api/v1/news`
- **PostgreSQL schema:** `notification`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=notification`
- **Bảng sở hữu:** notifications, news_articles, news_attachments, news_receipts, notification_templates, notification_reminder_rules, notification_reminder_dispatches
- **Phụ thuộc:** PostgreSQL, RabbitMQ, SMTP tùy chọn

## Vị trí mã nguồn

- Controller/API: `NewsApi.kt, NotificationApi.kt, NotificationAutomationApi.kt`
- Migration: `V1__notification_schema.sql, V2__news_broadcast.sql, V3__email_delivery_outbox.sql, V4__recover_stale_email_leases.sql, V5__notification_templates_and_reminders.sql, V6__notification_delivery_error_length.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:notification-service:test --no-daemon
./gradlew :services:notification-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
