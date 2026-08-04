#!/usr/bin/env python3
"""Trusted host-side agent for LMSPilot operation jobs.

The web/API process never executes shell commands. This agent polls the loopback-only
operations endpoint, accepts only a fixed allowlist, sends heartbeats, and reports a
structured result. Run it from the repository root on the on-premise host.
"""
from __future__ import annotations

import argparse
import json
import os
import socket
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
API = os.getenv("LMSPILOT_OPERATIONS_AGENT_URL", "http://127.0.0.1:8097")


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    env_file = ROOT / ".env"
    if env_file.exists():
        for raw in env_file.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def request(path: str, token: str, payload: dict[str, Any]) -> tuple[int, dict[str, Any] | None]:
    req = urllib.request.Request(
        API + path,
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json", "X-Service-Token": token},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            body = response.read()
            return response.status, json.loads(body) if body else None
    except urllib.error.HTTPError as exc:
        body = exc.read().decode(errors="replace")
        if exc.code == 204:
            return 204, None
        raise RuntimeError(f"Operations API {exc.code}: {body}") from exc


def run_command(command: list[str], extra_env: dict[str, str] | None = None) -> dict[str, Any]:
    env = os.environ.copy()
    if extra_env:
        env.update(extra_env)
    started = time.time()
    completed = subprocess.run(command, cwd=ROOT, env=env, text=True, capture_output=True)
    return {
        "command": command,
        "exitCode": completed.returncode,
        "durationSeconds": round(time.time() - started, 3),
        "stdout": completed.stdout[-12000:],
        "stderr": completed.stderr[-12000:],
    }


def execute(job: dict[str, Any]) -> tuple[bool, dict[str, Any]]:
    kind = str(job["type"]).upper()
    params = {str(k): str(v) for k, v in (job.get("parameters") or {}).items()}
    if kind == "BACKUP":
        result = run_command(["bash", "scripts/backup.sh"])
    elif kind == "RESTORE":
        backup = params.get("backupPath", "")
        if not backup:
            return False, {"error": "backupPath is required"}
        result = run_command(["bash", "scripts/restore.sh", backup], {"LMSPILOT_RESTORE_CONFIRMATION": "RESTORE"})
    elif kind == "MAINTENANCE":
        mode = params.get("mode", "").upper()
        if mode == "ON":
            result = run_command(["docker", "compose", "stop", "web", "api-gateway"])
        elif mode == "OFF":
            result = run_command(["docker", "compose", "up", "-d", "api-gateway", "web"])
        else:
            return False, {"error": "mode must be ON or OFF"}
    elif kind in {"UPDATE", "ROLLBACK"}:
        # Deliberately do not execute arbitrary release commands. Offline packages must
        # be applied by the signed release workflow after vendor/customer approval.
        return False, {"error": f"{kind} requires an approved signed-release adapter; arbitrary shell execution is disabled"}
    else:
        return False, {"error": f"Unsupported operation type: {kind}"}
    return result["exitCode"] == 0, result


def process_once(token: str, agent_id: str) -> bool:
    status, job = request("/internal/v1/operations/jobs/claim", token, {"agentId": agent_id})
    if status == 204 or not job:
        return False
    stop = threading.Event()

    def heartbeat() -> None:
        while not stop.wait(30):
            try:
                request(f"/internal/v1/operations/jobs/{job['id']}/heartbeat", token, {"claimToken": job["claimToken"]})
            except Exception as exc:  # keep the operation running; completion still authenticates the claim
                print(f"heartbeat warning: {exc}", file=sys.stderr)

    thread = threading.Thread(target=heartbeat, daemon=True)
    thread.start()
    try:
        success, result = execute(job)
    except Exception as exc:
        success, result = False, {"error": str(exc)}
    finally:
        stop.set()
        thread.join(timeout=2)
    request(
        f"/internal/v1/operations/jobs/{job['id']}/complete",
        token,
        {"claimToken": job["claimToken"], "success": success, "result": result},
    )
    print(json.dumps({"jobId": job["id"], "type": job["type"], "success": success}, ensure_ascii=False))
    return True


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--once", action="store_true", help="Process at most one job and exit")
    parser.add_argument("--interval", type=int, default=10, help="Polling interval in seconds")
    parser.add_argument("--agent-id", default=f"{socket.gethostname()}-{os.getpid()}")
    args = parser.parse_args()
    values = load_env()
    token = os.getenv("LMSPILOT_INTERNAL_TOKEN") or values.get("LMSPILOT_INTERNAL_TOKEN")
    if not token:
        print("Missing LMSPILOT_INTERNAL_TOKEN in environment or .env", file=sys.stderr)
        return 2
    while True:
        try:
            processed = process_once(token, args.agent_id)
        except Exception as exc:
            print(f"operations-agent error: {exc}", file=sys.stderr)
            processed = False
            if args.once:
                return 1
        if args.once:
            return 0
        if not processed:
            time.sleep(max(2, args.interval))


if __name__ == "__main__":
    raise SystemExit(main())
