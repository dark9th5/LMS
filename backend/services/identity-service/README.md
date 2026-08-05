# identity-service

- **Tên:** Identity
- **Owner:** Chưa phân công
- **Reviewer:** Chưa phân công
- **Port mặc định:** `8081`
- **Ngôn ngữ/runtime:** Java 21, Spring Boot 3.5.16
- **PostgreSQL schema:** `identity`

## Phạm vi sở hữu

Đăng nhập, JWT/refresh token, phiên, tài khoản, vai trò độc quyền ADMIN/INSTRUCTOR/STUDENT và quyền theo phạm vi.

## API chính

- `/api/v1/auth`
- `/api/v1/authorization`
- `/api/v1/directory`
- `/api/v1/roles`
- `/api/v1/users`
- `/api/v1/users/{userId}/sessions`
- `/internal/v1/authorization`
- `/internal/v1/users`

## Controller Java

- `src/main/java/**/IdentityControllers.java`

## Bảng dữ liệu sở hữu

- `authorization_grants`
- `bulk_operations`
- `identity_system_locks`
- `password_history`
- `refresh_tokens`
- `role_permissions`
- `roles`
- `scoped_role_assignments`
- `user_accounts`
- `user_roles`

## Thư mục quan trọng

- Main source: `src/main/java`
- Configuration: `src/main/resources/application.yml`
- Flyway: `src/main/resources/db/migration`
- Tests: `src/test/java`

## Chạy và test

```bash
cd backend
./gradlew :services:identity-service:bootRun
./gradlew :services:identity-service:test
```

## Checklist owner

- [ ] API/DTO tương thích contract hiện tại.
- [ ] Có unit test cho nghiệp vụ mới.
- [ ] Có test authorization và validation.
- [ ] Migration mới chạy được trên database sạch và database đã có dữ liệu.
- [ ] Không truy cập trực tiếp bảng của service khác.
- [ ] Cập nhật `docs/API_DATABASE_MAP.md` nếu API/DB thay đổi.
- [ ] Reviewer đã kiểm tra ảnh hưởng producer/consumer.
