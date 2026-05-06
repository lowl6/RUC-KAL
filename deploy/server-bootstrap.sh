#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/ruc-kal}"
REPO_URL="${REPO_URL:-https://github.com/lowl6/RUC-KAL.git}"
BRANCH="${BRANCH:-master}"

echo "[1/6] Installing Docker / Git if needed..."
if ! command -v git >/dev/null 2>&1; then
  apt-get update
  apt-get install -y git
fi

if ! command -v docker >/dev/null 2>&1; then
  apt-get update
  apt-get install -y ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  . /etc/os-release
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

echo "[2/6] Cloning or updating repository..."
if [ ! -d "$APP_DIR/.git" ]; then
  mkdir -p "$(dirname "$APP_DIR")"
  git clone "$REPO_URL" "$APP_DIR"
fi

cd "$APP_DIR"
git fetch origin
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"

echo "[3/6] Preparing .env..."
if [ ! -f .env ]; then
  cp .env.example .env
  MYSQL_PASSWORD="$(openssl rand -base64 24 | tr -d '\n')"
  MYSQL_ROOT_PASSWORD="$(openssl rand -base64 24 | tr -d '\n')"
  KAL_JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
  sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=${MYSQL_PASSWORD}|" .env
  sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}|" .env
  sed -i "s|^KAL_JWT_SECRET=.*|KAL_JWT_SECRET=${KAL_JWT_SECRET}|" .env
  sed -i "s|^KAL_CORS_ALLOWED_ORIGINS=.*|KAL_CORS_ALLOWED_ORIGINS=http://47.238.235.51:3000|" .env
  sed -i "s|^KAL_WEB_BASE_URL=.*|KAL_WEB_BASE_URL=http://47.238.235.51:3000|" .env
  echo "Created .env with generated DB/JWT secrets. Edit .env if you want to enable SMTP."
fi

echo "[4/6] Opening firewall ports if ufw exists..."
if command -v ufw >/dev/null 2>&1; then
  ufw allow 22/tcp || true
  ufw allow 3000/tcp || true
  ufw allow 8080/tcp || true
fi

echo "[5/6] Building and starting services..."
docker compose up -d --build

echo "[6/6] Current services:"
docker compose ps

echo
echo "Deployment done."
echo "Frontend: http://47.238.235.51:3000/"
echo "Admin:    http://47.238.235.51:3000/admin/login"
