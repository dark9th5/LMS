#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() { echo "PRECHECK FAILED: $*" >&2; exit 2; }
command -v docker >/dev/null 2>&1 || fail "Chưa cài Docker."
docker compose version >/dev/null 2>&1 || fail "Cần Docker Compose v2."
docker info >/dev/null 2>&1 || fail "Docker Engine chưa chạy hoặc tài khoản hiện tại không có quyền truy cập."
[[ -f .env ]] || fail "Thiếu .env. Hãy chạy scripts/setup.sh để tạo tự động."

set -a
# shellcheck disable=SC1091
source .env
set +a

for name in LMSPILOT_JWT_SECRET LMSPILOT_INTERNAL_TOKEN CONFIGURATION_SECRET_KEY AI_SECRET_KEY LMSPILOT_DEFAULT_ADMIN_PASSWORD POSTGRES_ADMIN_PASSWORD POSTGRES_SERVICE_PASSWORD RABBITMQ_DEFAULT_PASS REDIS_PASSWORD; do
  value="${!name:-}"
  [[ -n "$value" ]] || fail "$name đang trống."
  case "$value" in
    replace-with-*|ChangeMe-*) fail "$name vẫn đang dùng giá trị mẫu." ;;
  esac
done
[[ ${#LMSPILOT_JWT_SECRET} -ge 32 ]] || fail "LMSPILOT_JWT_SECRET phải có ít nhất 32 ký tự."
[[ ${#LMSPILOT_INTERNAL_TOKEN} -ge 32 ]] || fail "LMSPILOT_INTERNAL_TOKEN phải có ít nhất 32 ký tự."
[[ ${#CONFIGURATION_SECRET_KEY} -ge 32 ]] || fail "CONFIGURATION_SECRET_KEY phải có ít nhất 32 ký tự."
[[ ${#AI_SECRET_KEY} -ge 32 ]] || fail "AI_SECRET_KEY phải có ít nhất 32 ký tự."

docker compose config --quiet || fail "docker-compose.yml hoặc .env không hợp lệ."

free_kb="$(df -Pk "$ROOT_DIR" | awk 'NR==2 {print $4}')"
[[ "${free_kb:-0}" -ge 10485760 ]] || fail "Cần tối thiểu khoảng 10 GB dung lượng trống để build và chạy lần đầu."

echo "PRECHECK PASSED"
