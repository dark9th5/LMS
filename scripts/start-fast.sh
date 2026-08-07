#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker chưa được cài hoặc chưa có trong PATH." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "Docker Engine chưa chạy." >&2
  exit 1
fi
if [ ! -f .env ]; then
  echo "Thiếu .env. Hãy sao chép .env.example thành .env và điền các secret bắt buộc." >&2
  exit 1
fi

# Build đúng hai image dùng chung. 19 service Java đều dùng backend-bundle,
# tránh để Compose lặp lại bước build cho từng service.
export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-8}"
docker compose build --pull api-gateway web

docker compose up -d --no-build --remove-orphans --wait --wait-timeout "${LMSPILOT_STARTUP_TIMEOUT:-360}"
echo "LMSPilot đã sẵn sàng tại http://localhost:3000"
