# Build verification — LMSPilot 0.8.0

Ngày kiểm tra: 03/08/2026.

## Môi trường tạo bản

- OpenJDK 21.0.10
- Node.js 22.16.0
- npm 10.9.2
- Python 3.13.5
- Không có Docker CLI/Engine

## Kiểm tra thành công

```bash
python3 scripts/validate-repository.py
python3 -m unittest discover -s tests -v
bash scripts/test-static.sh
```

Kết quả:

- Repository validator: OK.
- 10 JSON và 28 YAML hợp lệ.
- 19 backend service; 18 service có Flyway migration.
- Không có migration trùng phiên bản hoặc lệnh `DROP TABLE`/`DROP COLUMN` trong migration được kiểm soát.
- 73/73 static/contract/unit tests đạt.
- TypeScript/TSX parser check và shell syntax check đạt.

## Kiểm tra không thể hoàn tất

### Backend Gradle

Lệnh:

```bash
cd backend
./gradlew test --no-daemon
```

Dừng trước khi compile vì Gradle Wrapper không thể phân giải DNS/tải distribution từ `services.gradle.org`. Không có Gradle distribution/dependency cache cục bộ để thay thế.

### Frontend npm

Lệnh:

```bash
cd apps/web
npm ci --no-audit --no-fund
```

Dừng trước typecheck/build vì registry mirror của môi trường trả 404 cho `undici-types@7.16.0`.

### Docker/E2E

Docker không được cài trong môi trường tạo bản, nên không thể chạy Compose, database migrations thật, RabbitMQ events, browser E2E hoặc smoke test container.

## Lệnh bắt buộc trên máy đích

```bash
cd backend && ./gradlew test --no-daemon
cd ../../apps/web && npm ci --no-audit --no-fund && npm run typecheck && npm run build
cd ../.. && docker compose config --quiet
./scripts/setup.sh
```

Windows dùng `scripts/setup.ps1`. Chỉ chấp nhận runtime sau khi `SMOKE TEST PASSED` và tất cả service healthy.
