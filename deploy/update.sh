#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/ruc-kal}"
BRANCH="${BRANCH:-master}"

cd "$APP_DIR"
git fetch origin
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"
docker compose up -d --build
docker compose ps
