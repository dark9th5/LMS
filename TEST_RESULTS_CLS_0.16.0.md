# Test results — LMSPilot CLS 0.16.0

Ngày kiểm tra: 2026-08-04.

## Đã đạt

- `python scripts/validate-repository.py`: PASS.
  - 24 JSON, 28 YAML.
  - 19 backend service.
  - 18 service có Flyway.
  - API Gateway wiring, Gradle wrapper, script và Docker layout hợp lệ.
- `pytest -q`: **111 passed, 25 skipped, 2 subtests passed**.
  - 25 test skip là các contract UI 0.11–0.14 yêu cầu stylesheet/10 theme đã được thay thế có chủ ý.
  - `test_v016_unified_ui.py` kiểm tra runtime style, light/dark registry, login control states, layout học/thi, Core Admin, route retired và structured fields.
- TypeScript `transpileModule`: **64 TS/TSX file, 0 lỗi cú pháp**.
- `tinycss2`:
  - `globals.css`: 0 parse error.
  - `unified.css`: 0 parse error.

## Chưa xác minh trong môi trường đóng gói

- `npm ci` không hoàn thành do registry/dependency không khả dụng.
- Vì vậy chưa xác nhận semantic TypeScript và `next build`.
- Gradle distribution/dependency không có sẵn, chưa chạy full multi-module test.
- Docker stack và browser E2E chưa chạy.

Các mục chưa xác minh không được coi là PASS. Phải chạy lại trên máy đích trước production.
