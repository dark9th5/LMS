#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
[[ -f .env ]] || { echo "Thiếu .env." >&2; exit 2; }
set -a
# shellcheck disable=SC1091
source .env
set +a

STAMP="$(date +%Y%m%d-%H%M%S)"
FINAL_DEST="${ROOT_DIR}/backups/${STAMP}"
TEMP_DEST="${FINAL_DEST}.tmp"
rm -rf "$TEMP_DEST"
mkdir -p "$TEMP_DEST/postgres" "$TEMP_DEST/files" "$TEMP_DEST/rabbitmq" "$TEMP_DEST/config"

cleanup() {
  if [[ -d "$TEMP_DEST" ]]; then rm -rf "$TEMP_DEST"; fi
}
trap cleanup EXIT

docker compose ps postgres rabbitmq >/dev/null
for service in postgres rabbitmq; do
  [[ -n "$(docker compose ps --status running -q "$service")" ]] || { echo "$service chưa chạy." >&2; exit 1; }
done

schemas=(identity organization course enrollment learning assessment grading reporting file_storage license audit notification certificate ai configuration integration operations competency)
for schema in "${schemas[@]}"; do
  docker compose exec -T postgres pg_dump \
    -U postgres -d "${POSTGRES_DB:-lmspilot}" -n "$schema" \
    --clean --if-exists --no-owner --no-privileges \
    > "$TEMP_DEST/postgres/${schema}.sql"
done

docker compose exec -T rabbitmq rabbitmqctl export_definitions /tmp/lmspilot-definitions.json >/dev/null
docker compose cp rabbitmq:/tmp/lmspilot-definitions.json "$TEMP_DEST/rabbitmq/definitions.json" >/dev/null

docker run --rm \
  -v lmspilot_files:/source:ro \
  -v "$TEMP_DEST/files:/backup" \
  alpine:3.22 sh -c 'cd /source && tar czf /backup/files.tar.gz .'

cp .env.example "$TEMP_DEST/config/env.example"
if [[ "${LMSPILOT_BACKUP_INCLUDE_SECRETS:-false}" == "true" ]]; then
  cp .env "$TEMP_DEST/config/env.secrets"
  chmod 600 "$TEMP_DEST/config/env.secrets"
  echo "WARNING: backup contains runtime secrets; store it only on encrypted media." >&2
else
  printf '%s\n' 'Runtime secrets were intentionally excluded. Set LMSPILOT_BACKUP_INCLUDE_SECRETS=true only when the backup destination is encrypted.' > "$TEMP_DEST/config/SECRETS_NOT_INCLUDED.txt"
fi

printf '{"createdAt":"%s","formatVersion":1,"applicationVersion":"0.9.0"}\n' "$(date -Iseconds)" > "$TEMP_DEST/manifest.json"
(
  cd "$TEMP_DEST"
  find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS
  sha256sum -c SHA256SUMS >/dev/null
)

mv "$TEMP_DEST" "$FINAL_DEST"
trap - EXIT
echo "Backup created and verified: $FINAL_DEST"
