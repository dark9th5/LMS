# Test results — LMSPilot 0.17.0

Ngày kiểm tra: 2026-08-04.

## Đã đạt

### Repository validator

`python scripts/validate-repository.py`: **PASS**.

- 26 JSON và 28 YAML hợp lệ.
- 19 backend service được nhận diện.
- 18 service có Flyway migration.
- API Gateway wiring, Gradle wrapper, script và Docker layout hợp lệ.

### Static, contract và UI regression

`python -m unittest discover -s tests -p "test_*.py"`: **115 test đạt, 34 test skip (149 test tổng cộng)**.

34 test skip là contract của các giao diện 0.11–0.14 yêu cầu Cosmic UI, 10 theme hoặc module đã bị retire. Chúng được thay bằng test 0.16/0.17 kiểm tra:

- component và stylesheet legacy đã bị xóa;
- light/dark registry và control states ở login;
- nội dung học/thi nằm ở cột chính;
- Core Admin và route retire;
- `RepeatableField`, `NumberStepper` và form có cấu trúc;
- mọi class literal trong TSX có CSS runtime;
- không còn copy/asset fantasy;
- selector danh sách câu thi không tác động nhầm nút khác;
- đủ bộ ảnh phát hành.

### TypeScript syntax

`transpileModule` bằng TypeScript cài sẵn toàn cục: **62 file TS/TSX, 0 lỗi cú pháp**.

Đây là kiểm tra cú pháp và isolated transpilation, không thay thế semantic typecheck của toàn project.

### CSS syntax

`tinycss2`:

- `apps/web/app/globals.css`: 0 parse error.
- `apps/web/app/unified.css`: 0 parse error.

### Visual preview

`python scripts/render-ui-previews.py`: **7 ảnh được render thành công** bằng Chromium từ chính `globals.css`, `unified.css` và class runtime:

- login;
- dashboard light;
- course catalog;
- learning player;
- exam focus;
- dashboard dark;
- dashboard mobile.

Ảnh nằm tại `docs/screenshots/0.17.0`.

## Chưa xác minh trong môi trường đóng gói

- `npm ci --offline` dừng với `ENOTCACHED` vì `undici-types-7.16.0.tgz` không có trong npm cache.
- Vì không có `node_modules`, chưa chạy được `npm run typecheck` và `next build`.
- `backend/gradlew test --offline --no-daemon` dừng khi wrapper cần tải Gradle 8.14.5 nhưng DNS/network không truy cập được `services.gradle.org`.
- Docker stack, migration trên database thật và browser E2E kết nối API chưa chạy.

Các mục chưa xác minh **không được coi là PASS**. Phải chạy lại trên máy có Internet/dependency hoặc CI trước khi merge production.
