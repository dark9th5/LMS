# BẮT ĐẦU TẠI ĐÂY — LMSPilot 0.20.4

Đây là repository đầy đủ của LMSPilot. Giải nén vào một thư mục mới, tạo branch/tag và backup dữ liệu trước khi thay thế hệ thống đang chạy.

## Dành cho PM/Tech Lead

1. Đọc `README.md`.
2. Mở `docs/TEAM_SERVICE_ASSIGNMENT.md` và gán owner/reviewer.
3. Mỗi người đọc `backend/services/<service-name>/README.md`.
4. Tạo Epic cho từng service; tách Issue nâng cấp, bug và test.
5. Bảo vệ nhánh `main`, yêu cầu ít nhất một review và CI đạt.

## Dành cho developer

```bash
python scripts/validate-repository.py
python scripts/validate-service-ports.py
cd backend
./gradlew :services:<service-name>:test --no-daemon
```

Không truy cập database của service khác và không tự approve pull request của mình.

## Tài liệu chính

- `docs/ARCHITECTURE.md`
- `docs/SERVICE_CATALOG.md`
- `docs/API_DATABASE_MAP.md`
- `docs/TEAM_SERVICE_ASSIGNMENT.md`
- `DELIVERY_STATUS.md`
- `TEST_RESULTS_LMSPILOT_0.20.4.md`
