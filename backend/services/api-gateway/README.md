# api-gateway

- **Sản phẩm:** LMSPilot
- **Port mặc định:** `8080`
- **Owner:** `TBD`
- **Reviewer:** `TBD`
- **Phạm vi sở hữu:** Điểm vào duy nhất của frontend; xác thực JWT, rate limit, correlation ID và định tuyến.
- **API chính:** `/api/v1/**`, `/public/v1/**` → các service nội bộ
- **PostgreSQL schema:** `—`
- **DB URL local:** `Không áp dụng`
- **Bảng sở hữu:** —
- **Phụ thuộc:** Identity, Redis và toàn bộ service backend

## Vị trí mã nguồn

- Controller/API: `Gateway route/configuration`
- Migration: `Không có database migration`
- Main source: `src/main/kotlin`
- Test: `src/test/kotlin`
- Config: `src/main/resources/application.yml`

## Chạy riêng

```bash
cd backend
./gradlew :services:api-gateway:test --no-daemon
./gradlew :services:api-gateway:bootRun
```

## Checklist owner

- Nghiệp vụ và authorization đúng phạm vi service.
- API có validation, error contract và test.
- Migration Flyway chỉ thêm mới, không sửa lịch sử đã phát hành.
- Không truy cập trực tiếp database service khác.
- Cập nhật `docs/API_DATABASE_MAP.md` khi thêm endpoint hoặc bảng.
- Chạy `python scripts/validate-service-ports.py` trước pull request.
