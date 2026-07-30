#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
command -v docker >/dev/null || { echo "Docker chưa được cài đặt." >&2; exit 2; }
docker compose version >/dev/null || { echo "Docker Compose v2 chưa sẵn sàng." >&2; exit 2; }

random_token() {
  local bytes="${1:-48}"
  head -c "$bytes" /dev/urandom | base64 | tr -d '\r\n=' | tr '/+' '_-'
}

replace_env() {
  local key="$1" value="$2" file="$3" tmp
  tmp="${file}.tmp"
  awk -v k="$key" -v v="$value" 'BEGIN{done=0} index($0,k"=")==1 {$0=k"="v;done=1} {print} END{if(!done) print k"="v}' "$file" > "$tmp"
  mv "$tmp" "$file"
}

if [[ ! -f .env ]]; then
  cp .env.example .env
  replace_env LMSPILOT_JWT_SECRET "$(random_token 64)" .env
  replace_env LMSPILOT_INTERNAL_TOKEN "$(random_token 56)" .env
  replace_env LMSPILOT_DEFAULT_ADMIN_PASSWORD "Lp-$(random_token 18)-A9!" .env
  replace_env POSTGRES_ADMIN_PASSWORD "$(random_token 24)" .env
  replace_env POSTGRES_SERVICE_PASSWORD "$(random_token 24)" .env
  replace_env RABBITMQ_DEFAULT_PASS "$(random_token 24)" .env
  replace_env REDIS_PASSWORD "$(random_token 24)" .env
  echo "Đã tạo .env với secret ngẫu nhiên. Mật khẩu demo nằm tại LMSPILOT_DEFAULT_ADMIN_PASSWORD."
else
  echo "Giữ nguyên file .env hiện có."
fi

./scripts/preflight.sh
docker compose up -d --build
./scripts/smoke-test.sh

echo "LMSPilot đã sẵn sàng: http://localhost:3000"
