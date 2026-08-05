#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
[[ -f .env ]] || { echo "Thiếu .env. Hãy chạy ./scripts/setup.sh trước." >&2; exit 2; }
set -a
# shellcheck disable=SC1091
source .env
set +a
GATEWAY="${LMSPILOT_SMOKE_GATEWAY_URL:-http://localhost:8080}"
WEB="${LMSPILOT_SMOKE_WEB_URL:-http://localhost:3000}"
PASSWORD="${LMSPILOT_DEFAULT_ADMIN_PASSWORD:?Thiếu LMSPILOT_DEFAULT_ADMIN_PASSWORD trong .env}"

wait_http() {
  local url="$1" name="$2" attempts="${3:-120}"
  local i
  for ((i=1; i<=attempts; i++)); do
    if curl -fsS --max-time 4 "$url" >/dev/null 2>&1; then
      echo "READY: $name"
      return 0
    fi
    sleep 2
  done
  echo "TIMEOUT: $name ($url)" >&2
  docker compose ps >&2 || true
  return 1
}

json_access_token() {
  sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1
}

login() {
  local username="$1"
  curl -fsS --max-time 15 -H 'Content-Type: application/json' \
    --data-binary "{\"username\":\"${username}\",\"password\":\"${PASSWORD}\"}" \
    "$GATEWAY/api/v1/auth/login"
}

assert_role() {
  local payload="$1" role="$2" username="$3"
  printf '%s' "$payload" | grep -q "\"$role\"" || {
    echo "Sai vai trò đăng nhập cho $username: cần $role" >&2
    echo "$payload" >&2
    exit 1
  }
  echo "PASS: $username -> $role"
}

web_login_check() {
  local username="$1" role="$2" landing="$3" expected_name="$4"
  local jar response page
  jar="$(mktemp)"
  response="$(curl -fsS --max-time 20 -c "$jar" -H 'Content-Type: application/json' \
    --data-binary "{\"username\":\"${username}\",\"password\":\"${PASSWORD}\"}" \
    "$WEB/api/auth/login")"
  assert_role "$response" "$role" "$username (Web)"
  page="$(curl -fsS --max-time 20 -b "$jar" "$WEB$landing")"
  rm -f "$jar"
  printf '%s' "$page" | grep -q "$expected_name" || {
    echo "Web $username mở $landing nhưng không hiển thị đúng hồ sơ $expected_name" >&2
    exit 1
  }
  echo "PASS: Web $username opens $landing as $expected_name"
}

auth_get() {
  local token="$1" path="$2"
  curl -fsS --max-time 20 -H "Authorization: Bearer $token" "$GATEWAY$path" >/dev/null
  echo "PASS: GET $path"
}

wait_http "$GATEWAY/actuator/health/readiness" "API Gateway"
wait_http "$WEB/login" "Web portal"

ADMIN_JSON="$(login admin)"
ADMIN_TOKEN="$(printf '%s' "$ADMIN_JSON" | json_access_token)"
[[ -n "$ADMIN_TOKEN" ]] || { echo "Đăng nhập admin không trả access token: $ADMIN_JSON" >&2; exit 1; }
assert_role "$ADMIN_JSON" "ADMIN" "admin"

auth_get "$ADMIN_TOKEN" /api/v1/auth/me
auth_get "$ADMIN_TOKEN" '/api/v1/users?size=5'
auth_get "$ADMIN_TOKEN" /api/v1/organization/units
auth_get "$ADMIN_TOKEN" '/api/v1/courses?size=5'
auth_get "$ADMIN_TOKEN" /api/v1/users
auth_get "$ADMIN_TOKEN" /api/v1/reports/dashboard
auth_get "$ADMIN_TOKEN" /api/v1/configuration
auth_get "$ADMIN_TOKEN" /api/v1/operations/health

INSTRUCTOR_JSON="$(login instructor)"
INSTRUCTOR_TOKEN="$(printf '%s' "$INSTRUCTOR_JSON" | json_access_token)"
[[ -n "$INSTRUCTOR_TOKEN" ]] || { echo "Đăng nhập instructor không trả access token: $INSTRUCTOR_JSON" >&2; exit 1; }
assert_role "$INSTRUCTOR_JSON" "INSTRUCTOR" "instructor"
auth_get "$INSTRUCTOR_TOKEN" '/api/v1/courses?size=5'
auth_get "$INSTRUCTOR_TOKEN" /api/v1/courses
auth_get "$INSTRUCTOR_TOKEN" /api/v1/grades/queue

LEARNER_JSON="$(login learner)"
LEARNER_TOKEN="$(printf '%s' "$LEARNER_JSON" | json_access_token)"
[[ -n "$LEARNER_TOKEN" ]] || { echo "Đăng nhập learner không trả access token: $LEARNER_JSON" >&2; exit 1; }
assert_role "$LEARNER_JSON" "LEARNER" "learner"
auth_get "$LEARNER_TOKEN" /api/v1/learning/me
auth_get "$LEARNER_TOKEN" /api/v1/exams
auth_get "$LEARNER_TOKEN" /api/v1/grades/me
auth_get "$LEARNER_TOKEN" /api/v1/certificates/me

web_login_check admin ADMIN /dashboard "Quản trị hệ thống"
web_login_check instructor INSTRUCTOR /dashboard "Giảng viên mẫu"
web_login_check learner LEARNER /learning "Học viên mẫu"

echo "SMOKE TEST PASSED"
