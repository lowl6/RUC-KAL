#!/usr/bin/env bash
set -euo pipefail
APP_DIR="${APP_DIR:-/opt/ruc-kal}"
cd "$APP_DIR"
git pull --ff-only
sed -i 's/\r$//' deploy/run-native.sh deploy/update-native.sh 2>/dev/null || true

cd "${APP_DIR}/backend"
mvn -q -DskipTests package

cd "${APP_DIR}/frontend-vue"
NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com}"
npm ci --registry="${NPM_REGISTRY}"
VITE_API_BASE=/api/v1 npm run build

rsync -a --delete "${APP_DIR}/frontend-vue/dist/" /var/www/kal/
systemctl restart kal-backend
echo "update-native 完成。"
