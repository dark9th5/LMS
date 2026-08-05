# Test results — LMSPilot 0.20.3

## Đạt

- `python -m unittest discover -s tests -p "test_*.py"`: 186 tổng, 152 đạt, 34 skip có chủ ý.
- `python scripts/validate-repository.py`: PASS — 30 JSON, 28 YAML, 19 service, 18 Flyway service, 19 port duy nhất.
- `node scripts/check-typescript-syntax.js`: PASS — 89 TypeScript/TSX.
- `python scripts/test-ai-question-quality.py`: PASS.
  - 10 câu đúng yêu cầu.
  - EASY 3, MEDIUM 5, HARD 2.
  - Citation khớp nguồn 10/10.
  - Phương án không trùng 10/10.
  - Lời giải đầy đủ 10/10.
  - Stem trùng: 0.
  - Negative control phát hiện phân bố sai và citation giả.

## Chưa chạy

- `:services:ai-service:test`: Gradle Wrapper cần tải Gradle 8.14.5 nhưng môi trường không phân giải được `services.gradle.org`.
- Next.js semantic typecheck/build: package dependencies không có sẵn trong môi trường đóng gói.
- Live AI provider E2E: không có API key/model endpoint của khách hàng.
- Docker, PostgreSQL migration và browser E2E.
