#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/ruc-kal}"
BRANCH="${BRANCH:-master}"

compose_run () {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
  else
    echo "ERROR: 未找到 docker compose / docker-compose。" >&2
    exit 1
  fi
}

cd "$APP_DIR"
git fetch origin
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"
compose_run up -d --build
compose_run ps
