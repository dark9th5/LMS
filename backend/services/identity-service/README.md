# identity-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8081`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Đăng nhập, phiên, tài khoản và mô hình một tài khoản–một vai trò `ADMIN`/`INSTRUCTOR`/`STUDENT`.
- **API chính:** `/api/v1/auth`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/authorization`, `/api/v1/directory`
- **PostgreSQL schema:** `identity`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=identity`
- **Bảng sở hữu:** roles, role_permissions, user_accounts, user_roles, refresh_tokens, authorization_grants, scoped_role_assignments, bulk_operations, identity_system_locks, password_history
- **Phụ thuộc:** PostgreSQL, RabbitMQ

## Vị trí mã nguồn

- Controller/API: `AuthorizationCatalogApi.kt`
- Migration: `V1__identity_schema.sql, V2__scoped_rbac_and_protected_admin.sql, V3__identity_system_locks.sql, V4__password_policy_and_sessions.sql, V5__bulk_operation_serialization.sql, V6__exclusive_three_role_model.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:identity-service:test --no-daemon
./gradlew :services:identity-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
