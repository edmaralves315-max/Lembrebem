#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
python3 scripts/verify_resources.py
(cd backend && npm test)
