# organization-service

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8082`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Cây cơ cấu tổ chức, đơn vị, quan hệ thành viên và phạm vi dữ liệu.
- **API chính:** `/api/v1/organization/units`, `/api/v1/organization/memberships`
- **PostgreSQL schema:** `organization`
- **DB URL local:** `jdbc:postgresql://localhost:5432/lmspilot?currentSchema=organization`
- **Bảng sở hữu:** organization_units, organization_memberships_v2, organization_scope_projection
- **Phụ thuộc:** PostgreSQL

## Vị trí mã nguồn

- Controller/API: `MembershipApi.kt, OrganizationApi.kt`
- Migration: `V1__organization_schema.sql, V2__organization_scopes.sql`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:organization-service:test --no-daemon
./gradlew :services:organization-service:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
