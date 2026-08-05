# Kết quả kiểm tra LMSPilot 0.20.1

Ngày kiểm tra: 2026-08-05.

## Đạt

- `python scripts/validate-repository.py`: PASS — 28 JSON, 28 YAML, 19 service, 18 Flyway service, 19 port duy nhất và wiring chính hợp lệ.
- `python scripts/validate-service-ports.py`: PASS — 19 backend service dùng port riêng 8080–8098.
- `python -m unittest discover -s tests -p "test_*.py"`: 178 test, 144 đạt, 34 skip có chủ ý.
- `node scripts/check-typescript-syntax.js`: PASS — 88 tệp TypeScript/TSX transpile cú pháp thành công.
- `python scripts/render-ui-previews-v0201.py`: PASS — render đủ 18 ảnh QA tĩnh vào `docs/screenshots/0.20.1`.
- Kiểm tra ZIP sau đóng gói: không có lỗi.

## Chưa thể xác minh đầy đủ trong môi trường đóng gói

- `node scripts/check-typescript.js` không thể hoàn tất vì môi trường không có `node_modules` của React/Next, dẫn đến thiếu `react/jsx-runtime` và `next/navigation`; đây không phải lỗi cú pháp do vòng tinh chỉnh giao diện.
- `npm ci`, Next.js production build, full Gradle test, Docker smoke, migration database thật, browser E2E ba vai trò, AI provider thật và UAT vẫn cần chạy trên CI hoặc máy có đầy đủ dependency.
