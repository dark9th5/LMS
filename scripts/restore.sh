#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
SOURCE="${1:-}"
[[ -n "$SOURCE" ]] || { echo "Usage: ./scripts/restore.sh backups/<folder>" >&2; exit 2; }
SOURCE="$(cd "$SOURCE" 2>/dev/null && pwd)" || { echo "Backup folder not found." >&2; exit 2; }
[[ -f "$SOURCE/SHA256SUMS" && -f "$SOURCE/manifest.json" ]] || { echo "Backup không đầy đủ." >&2; exit 2; }
[[ -f .env ]] || { echo "Thiếu .env." >&2; exit 2; }
(cd "$SOURCE" && sha256sum -c SHA256SUMS)
set -a
# shellcheck disable=SC1091
source .env
set +a

read -r -p "Khôi phục sẽ ghi đè dữ liệu hiện tại. Nhập RESTORE để tiếp tục: " confirm
[[ "$confirm" == "RESTORE" ]] || { echo "Đã hủy."; exit 1; }

docker compose stop >/dev/null || true
docker compose up -d postgres rabbitmq redis
for _ in $(seq 1 60); do
  if [[ -n "$(docker compose ps --status running -q postgres)" ]] && docker compose exec -T postgres pg_isready -U postgres -d "${POSTGRES_DB:-lmspilot}" >/dev/null 2>&1; then break; fi
  sleep 2
done
docker compose exec -T postgres pg_isready -U postgres -d "${POSTGRES_DB:-lmspilot}" >/dev/null

for sql in "$SOURCE"/postgres/*.sql; do
  docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d "${POSTGRES_DB:-lmspilot}" < "$sql"
done

docker run --rm \
  -v lmspilot_files:/target \
  -v "$SOURCE/files:/backup:ro" \
  alpine:3.22 sh -c 'rm -rf /target/* /target/.[!.]* /target/..?* 2>/dev/null || true; tar xzf /backup/files.tar.gz -C /target'

docker compose cp "$SOURCE/rabbitmq/definitions.json" rabbitmq:/tmp/lmspilot-definitions.json >/dev/null
docker compose exec -T rabbitmq rabbitmqctl import_definitions /tmp/lmspilot-definitions.json >/dev/null

echo "Restore completed. Run: docker compose up -d && ./scripts/smoke-test.sh"
