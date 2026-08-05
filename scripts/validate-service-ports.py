#!/usr/bin/env python3
"""Validate that every backend service has one unique, documented port."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SERVICES = ROOT / "backend" / "services"
EXPECTED = {
    "api-gateway": 8080,
    "identity-service": 8081,
    "organization-service": 8082,
    "course-service": 8083,
    "enrollment-service": 8084,
    "learning-service": 8085,
    "assessment-service": 8086,
    "grading-service": 8087,
    "reporting-service": 8088,
    "file-storage-service": 8089,
    "license-service": 8090,
    "audit-service": 8091,
    "notification-service": 8092,
    "certificate-service": 8093,
    "ai-service": 8094,
    "configuration-service": 8095,
    "integration-service": 8096,
    "operations-service": 8097,
    "competency-service": 8098,
}
PORT_RE = re.compile(r"\$\{SERVER_PORT:(\d+)\}")


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    found: dict[str, int] = {}
    for service_dir in sorted(path for path in SERVICES.iterdir() if path.is_dir()):
        config = service_dir / "src" / "main" / "resources" / "application.yml"
        if not config.is_file():
            fail(f"{service_dir.name} thiếu application.yml")
        matches = PORT_RE.findall(config.read_text(encoding="utf-8"))
        if len(matches) != 1:
            fail(f"{service_dir.name} phải khai báo đúng một ${{SERVER_PORT:N}}, hiện có {matches}")
        found[service_dir.name] = int(matches[0])

    missing = set(EXPECTED) - set(found)
    extra = set(found) - set(EXPECTED)
    if missing or extra:
        fail(f"Danh sách service sai; thiếu={sorted(missing)}, thừa={sorted(extra)}")

    for service, expected_port in EXPECTED.items():
        if found[service] != expected_port:
            fail(f"{service} dùng port {found[service]}, mong đợi {expected_port}")

    reverse: dict[int, list[str]] = {}
    for service, port in found.items():
        reverse.setdefault(port, []).append(service)
    duplicates = {port: names for port, names in reverse.items() if len(names) > 1}
    if duplicates:
        fail(f"Port bị trùng: {duplicates}")

    print(f"OK: {len(found)} backend services use unique ports 8080-8098.")


if __name__ == "__main__":
    main()
