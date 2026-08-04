#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
mkdir -p .runtime
PID_FILE=.runtime/operations-agent.pid
LOG_FILE=.runtime/operations-agent.log

if [[ -f "$PID_FILE" ]]; then
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
    echo "Operations agent đang chạy (PID $pid)."
    exit 0
  fi
  rm -f "$PID_FILE"
fi

command -v python3 >/dev/null || { echo "Không tìm thấy python3; hãy chạy scripts/operations-agent.py bằng Python 3." >&2; exit 2; }
[[ -f .env ]] || { echo "Thiếu .env. Hãy chạy setup trước." >&2; exit 2; }

nohup python3 scripts/operations-agent.py --interval 10 >>"$LOG_FILE" 2>&1 &
pid=$!
echo "$pid" > "$PID_FILE"
sleep 1
if ! kill -0 "$pid" 2>/dev/null; then
  echo "Operations agent không khởi động được. Xem $LOG_FILE" >&2
  exit 1
fi
echo "Operations agent đã chạy (PID $pid, log $LOG_FILE)."
