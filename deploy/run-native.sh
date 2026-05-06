#!/usr/bin/env bash
# 不使用 Docker：apt 安装 JDK / MySQL / Nginx，本机构建前后端。
# 适用 Ubuntu 22.04+（root）。项目目录默认 /opt/ruc-kal。
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/ruc-kal}"
NODE_VER="${NODE_VER:-20.18.1}"
NODE_MIRROR="${NODE_MIRROR:-https://npmmirror.com/mirrors/node}"

echo "[1/7] 安装系统依赖（OpenJDK 21、Maven、MySQL、Nginx）..."
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends \
  ca-certificates curl git gnupg rsync \
  openjdk-21-jdk maven nginx mysql-server
systemctl enable --now mysql

echo "[2/7] 安装 Node ${NODE_VER}（二进制包，默认 npmmirror，避免依赖 Docker Hub）..."
ARCH="$(uname -m)"
case "$ARCH" in
  x86_64) NODE_ARCH=x64 ;;
  aarch64) NODE_ARCH=arm64 ;;
  *) echo "不支持的架构: $ARCH"; exit 1 ;;
esac
NODE_TAR="node-v${NODE_VER}-linux-${NODE_ARCH}.tar.xz"
TMP_NODE="/tmp/${NODE_TAR}"
if ! command -v node >/dev/null 2>&1 || ! node -e "process.exit(Number(process.versions.node.split('.')[0])>=20?0:1)" 2>/dev/null; then
  set +e
  curl -fsSL "${NODE_MIRROR}/v${NODE_VER}/${NODE_TAR}" -o "$TMP_NODE"
  EC=$?
  set -e
  if [[ $EC -ne 0 ]] || [[ ! -s "$TMP_NODE" ]]; then
    curl -fsSL "https://nodejs.org/dist/v${NODE_VER}/${NODE_TAR}" -o "$TMP_NODE"
  fi
  tar -xJf "$TMP_NODE" -C /usr/local --strip-components=1
fi
node -v
npm -v

echo "[3/7] 准备数据库与用户..."
if [[ ! -f "${APP_DIR}/.env.native" ]]; then
  if [[ -f "${APP_DIR}/.env" ]]; then
    cp "${APP_DIR}/.env" "${APP_DIR}/.env.native"
  else
    cp "${APP_DIR}/.env.example" "${APP_DIR}/.env.native"
    MYSQL_PW="$(openssl rand -base64 24 | tr -d '\n')"
    JWT_SEC="$(openssl rand -base64 48 | tr -d '\n')"
    sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=${MYSQL_PW}|" "${APP_DIR}/.env.native"
    sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=${MYSQL_PW}|" "${APP_DIR}/.env.native"
    sed -i "s|^KAL_JWT_SECRET=.*|KAL_JWT_SECRET=${JWT_SEC}|" "${APP_DIR}/.env.native"
    echo "已生成 ${APP_DIR}/.env.native ，请按需修改 KAL_CORS_ALLOWED_ORIGINS / KAL_WEB_BASE_URL。"
  fi
fi

sed -i 's/\r$//' "${APP_DIR}/.env.native" || true

env_get () {
  local key="$1"
  local file="$2"
  local line
  line="$(grep -E "^${key}=" "$file" | tail -n 1 || true)"
  printf '%s' "${line#*=}"
}

MYSQL_APP_PW="$(env_get MYSQL_PASSWORD "${APP_DIR}/.env.native")"
MYSQL_ROOT_PASSWORD_VAL="$(env_get MYSQL_ROOT_PASSWORD "${APP_DIR}/.env.native")"
DB_USER="$(env_get MYSQL_USER "${APP_DIR}/.env.native")"
DB_USER="${DB_USER:-kal}"

if [[ -z "${MYSQL_APP_PW}" ]]; then
  echo "请在 ${APP_DIR}/.env.native 设置 MYSQL_PASSWORD。"
  exit 1
fi

run_mysql_sql () {
  "$@" <<EOSQL
CREATE DATABASE IF NOT EXISTS kal CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${MYSQL_APP_PW}';
GRANT ALL PRIVILEGES ON kal.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOSQL
}

if mysql -uroot -e "SELECT 1" &>/dev/null; then
  run_mysql_sql mysql -uroot
elif [[ -n "${MYSQL_ROOT_PASSWORD_VAL}" ]] && MYSQL_PWD="${MYSQL_ROOT_PASSWORD_VAL}" mysql -uroot -e "SELECT 1" &>/dev/null; then
  run_mysql_sql env MYSQL_PWD="${MYSQL_ROOT_PASSWORD_VAL}" mysql -uroot
else
  echo "无法连接本机 MySQL root。请在 ${APP_DIR}/.env.native 填写 MYSQL_ROOT_PASSWORD，或确保 root 可用 socket 登录。"
  exit 1
fi

echo "[4/7] 构建后端..."
cd "${APP_DIR}/backend"
mvn -q -DskipTests package

echo "[5/7] 构建前端..."
cd "${APP_DIR}/frontend-vue"
NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com}"
npm ci --registry="${NPM_REGISTRY}"
VITE_API_BASE=/api/v1 npm run build

echo "[6/7] 部署静态资源与 Nginx..."
install -d /var/www/kal
rsync -a --delete "${APP_DIR}/frontend-vue/dist/" /var/www/kal/
install -m 0644 "${APP_DIR}/deploy/nginx-native.conf" /etc/nginx/sites-available/kal
ln -sf /etc/nginx/sites-available/kal /etc/nginx/sites-enabled/kal
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

echo "[7/7] systemd 后端服务..."
install -m 0644 "${APP_DIR}/deploy/kal-backend.service" /etc/systemd/system/kal-backend.service
systemctl daemon-reload
systemctl enable kal-backend
systemctl restart kal-backend

systemctl --no-pager -l status kal-backend --lines=15 || true
echo
echo "本机部署完成。前端: http://<本机IP>/   API 反代: /api/v1"
echo "查看后端日志: journalctl -u kal-backend -f"
echo "更新代码后: cd ${APP_DIR} && git pull && bash deploy/update-native.sh"
