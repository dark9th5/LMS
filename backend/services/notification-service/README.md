# notification-service

- **Tên:** Notification
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8092`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `notification`

## Phạm vi sở hữu

Thông báo, email outbox, tin tức, template và nhắc hạn.

## API chính

- `/api/v1/news`
- `/api/v1/notifications`
- `/api/v1/notifications/reminder-rules`
- `/api/v1/notifications/templates`

## Controller Java

- `src/main/java/**/NewsApi.java`
- `src/main/java/**/NotificationApi.java`
- `src/main/java/**/NotificationAutomationApi.java`

## Bảng dữ liệu sở hữu

- `news_articles`
- `news_attachments`
- `news_receipts`
- `notification_reminder_dispatches`
- `notification_reminder_rules`
- `notification_templates`
- `notifications`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:notification-service:bootRun
./gradlew :services:notification-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
