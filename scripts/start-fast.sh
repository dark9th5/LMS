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

export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-8}"

if [ "${1:-}" = "--build" ]; then
  echo "Đang biên dịch lại Docker Image từ mã nguồn..."
  docker compose build api-gateway web
else
  echo "Đang kiểm tra và khởi động hệ thống từ Docker Image pre-built..."
  docker compose pull --ignore-build-failures || true
fi

docker compose up -d --remove-orphans --wait --wait-timeout "${LMSPILOT_STARTUP_TIMEOUT:-360}"
echo "🎉 LMSPilot đã sẵn sàng tại http://localhost:3000"
