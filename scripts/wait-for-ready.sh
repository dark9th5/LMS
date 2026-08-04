#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
for _ in $(seq 1 120); do
  if [[ -n "$(docker compose ps --status exited -q 2>/dev/null)" ]]; then
    echo "Có container đã dừng:" >&2
    docker compose ps >&2
    exit 1
  fi
  if curl -fsS --max-time 4 http://localhost:8080/actuator/health/readiness >/dev/null 2>&1 \
     && curl -fsS --max-time 4 http://localhost:3000/login >/dev/null 2>&1; then
    echo "LMSPilot ready"
    exit 0
  fi
  sleep 2
done
echo "LMSPilot did not become ready in time" >&2
docker compose ps >&2
exit 1
