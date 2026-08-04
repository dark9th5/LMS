#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
PID_FILE=.runtime/operations-agent.pid
if [[ ! -f "$PID_FILE" ]]; then
  echo "Không có operations agent đang được quản lý."
  exit 0
fi
pid="$(cat "$PID_FILE" 2>/dev/null || true)"
if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
  kill "$pid"
  for _ in {1..20}; do kill -0 "$pid" 2>/dev/null || break; sleep .2; done
fi
rm -f "$PID_FILE"
echo "Operations agent đã dừng."
