# BẮT ĐẦU TẠI ĐÂY — LMSPilot CLS 0.9.0

Đây là repository đầy đủ; không cần áp overlay.

## 1. Giải nén an toàn

Giải nén `LMS-CLS-complete-0.9.0.zip` vào thư mục mới, không ghi đè repository đang chạy. Nếu muốn thay code trong repository Git hiện có, tạo branch/tag dự phòng và giữ nguyên thư mục `.git`.

## 2. Yêu cầu

- Windows: Docker Desktop, Linux containers, PowerShell.
- Linux: Docker Engine và Docker Compose v2.
- Lần build đầu cần truy cập registry hoặc có sẵn image/dependency cache.
- Python 3 trên host nếu dùng Operations Agent cho backup/restore từ giao diện.

## 3. Cài đặt

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

Linux:

```bash
chmod +x scripts/*.sh
./scripts/setup.sh
```

Không sửa migration hoặc dùng secret mẫu trước lần chạy đầu. Setup sẽ tạo `.env`, sinh secret, chạy preflight, build, health check và smoke test.

## 4. Đăng nhập

Khi thấy `SMOKE TEST PASSED`, mở `http://localhost:3000`.

Username demo:

```text
admin
instructor
learner
```

Mật khẩu nằm trong `.env`, biến `LMSPILOT_DEFAULT_ADMIN_PASSWORD`. Hệ thống có thể yêu cầu đổi mật khẩu ở lần đăng nhập đầu tùy seed/policy.

## 5. Kiểm tra khi có lỗi

```bash
docker compose ps
docker compose logs --tail=200
./scripts/preflight.sh
./scripts/test-static.sh
```

Windows dùng các lệnh Docker tương tự và `scripts\preflight.ps1`, `scripts\test-static.ps1`.

Đọc tiếp: `README.md`, `DELIVERY_STATUS.md`, `docs/UI_UX_REDESIGN_0.9.0.md`, `docs/BUILD_VERIFICATION_0.9.0.md`, `docs/AUDIT_0.8.2.md`, `docs/BA_CLS_TRACEABILITY_0.8.2.md`, `docs/OPERATIONS_RUNBOOK.md`.
