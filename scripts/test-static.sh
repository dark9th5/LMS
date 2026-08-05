#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
python scripts/validate-repository.py
python scripts/validate-service-ports.py
node scripts/check-typescript-syntax.js
for script in scripts/*.sh infrastructure/postgres/init.sh; do bash -n "$script"; done
python -m unittest discover -s tests -p 'test_*.py' -v
echo "STATIC TEST SUITE PASSED"
