#!/usr/bin/env bash
# User-space deployment for RUC-KAL.
#
# No Docker, no sudo, no systemd required. It installs JDK/Maven/Node under
# $HOME/apps, builds the project, and runs backend/frontend with nohup.
#
# Usage on server:
#   bash user-space-deploy.sh
#
# Useful env overrides:
#   REPO_URL=https://github.com/lowl6/RUC-KAL.git
#   APP_DIR=$HOME/ruc-kal
#   BACKEND_PORT=18080
#   FRONTEND_PORT=18000
#   PUBLIC_API_BASE=http://127.0.0.1:18080/api/v1

set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/lowl6/RUC-KAL.git}"
BRANCH="${BRANCH:-master}"
APP_DIR="${APP_DIR:-$HOME/ruc-kal}"
APPS_DIR="${APPS_DIR:-$HOME/apps}"
RUN_DIR="${RUN_DIR:-$HOME/.kal-run}"
LOG_DIR="${LOG_DIR:-$HOME/kal-logs}"
DATA_DIR="${DATA_DIR:-$HOME/kal-data}"

BACKEND_PORT="${BACKEND_PORT:-18080}"
FRONTEND_PORT="${FRONTEND_PORT:-18000}"
PUBLIC_API_BASE="${PUBLIC_API_BASE:-http://127.0.0.1:${BACKEND_PORT}/api/v1}"

JDK_VERSION="${JDK_VERSION:-21.0.6+7}"
JDK_DIR="${JDK_DIR:-$APPS_DIR/jdk-21}"
MAVEN_VERSION="${MAVEN_VERSION:-3.9.9}"
MAVEN_DIR="${MAVEN_DIR:-$APPS_DIR/apache-maven-${MAVEN_VERSION}}"
NODE_VERSION="${NODE_VERSION:-20.18.1}"
NODE_DIR="${NODE_DIR:-$APPS_DIR/node-v${NODE_VERSION}-linux-x64}"
NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com}"

mkdir -p "$APPS_DIR" "$RUN_DIR" "$LOG_DIR" "$DATA_DIR"

log () {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

download () {
  local url="$1"
  local out="$2"
  curl -fL --connect-timeout 20 --retry 3 --retry-delay 2 "$url" -o "$out"
}

ensure_jdk () {
  if [[ -x "$JDK_DIR/bin/java" ]]; then
    return
  fi

  log "Installing JDK 21 into $JDK_DIR"
  local tmp="$APPS_DIR/jdk21.tar.gz"
  local enc_ver="${JDK_VERSION/+/%2B}"
  local primary="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${enc_ver}/OpenJDK21U-jdk_x64_linux_hotspot_${JDK_VERSION/+/_}.tar.gz"
  local fallback="https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz"

  if ! download "$primary" "$tmp"; then
    log "Adoptium download failed, trying Oracle JDK fallback"
    download "$fallback" "$tmp"
  fi

  rm -rf "$JDK_DIR"
  mkdir -p "$JDK_DIR"
  tar -xzf "$tmp" -C "$JDK_DIR" --strip-components=1
}

ensure_maven () {
  if [[ -x "$MAVEN_DIR/bin/mvn" ]]; then
    return
  fi

  log "Installing Maven $MAVEN_VERSION into $MAVEN_DIR"
  local tmp="$APPS_DIR/maven.tar.gz"
  download "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" "$tmp"
  rm -rf "$MAVEN_DIR"
  tar -xzf "$tmp" -C "$APPS_DIR"
}

ensure_node () {
  if [[ -x "$NODE_DIR/bin/node" ]]; then
    return
  fi

  log "Installing Node $NODE_VERSION into $NODE_DIR"
  local tmp="$APPS_DIR/node.tar.xz"
  local mirror="https://npmmirror.com/mirrors/node/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.xz"
  local fallback="https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.xz"

  if ! download "$mirror" "$tmp"; then
    log "npmmirror download failed, trying nodejs.org fallback"
    download "$fallback" "$tmp"
  fi

  rm -rf "$NODE_DIR"
  tar -xJf "$tmp" -C "$APPS_DIR"
}

write_shell_profile () {
  local marker="# KAL_TOOLCHAIN"
  if ! grep -q "$marker" "$HOME/.bashrc" 2>/dev/null; then
    cat >> "$HOME/.bashrc" <<EOF

${marker}
export JAVA_HOME="$JDK_DIR"
export MAVEN_HOME="$MAVEN_DIR"
export NODE_HOME="$NODE_DIR"
export PATH="\$JAVA_HOME/bin:\$MAVEN_HOME/bin:\$NODE_HOME/bin:\$PATH"
EOF
  fi
}

export JAVA_HOME="$JDK_DIR"
export MAVEN_HOME="$MAVEN_DIR"
export NODE_HOME="$NODE_DIR"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$NODE_HOME/bin:$PATH"

stop_pid () {
  local pid_file="$1"
  local name="$2"
  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      log "Stopping $name pid=$pid"
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
}

log "Preparing toolchain"
ensure_jdk
ensure_maven
ensure_node
write_shell_profile

log "Tool versions"
java -version
mvn -version | head -n 1
node -v
npm -v

log "Cloning or updating repository"
if [[ -d "$APP_DIR/.git" ]]; then
  cd "$APP_DIR"
  git fetch origin
  git checkout "$BRANCH"
  git pull --ff-only origin "$BRANCH"
else
  git clone --branch "$BRANCH" "$REPO_URL" "$APP_DIR"
  cd "$APP_DIR"
fi

log "Building backend"
cd "$APP_DIR/backend"
mvn -q -DskipTests package

log "Building frontend with API base: $PUBLIC_API_BASE"
cd "$APP_DIR/frontend-vue"
npm ci --registry="$NPM_REGISTRY"
VITE_API_BASE="$PUBLIC_API_BASE" npm run build

log "Restarting services"
stop_pid "$RUN_DIR/backend.pid" "backend"
stop_pid "$RUN_DIR/frontend.pid" "frontend"

cd "$APP_DIR/backend"
nohup java \
  -Dserver.port="$BACKEND_PORT" \
  -Dspring.datasource.url="jdbc:h2:file:$DATA_DIR/kal;MODE=MySQL;DB_CLOSE_DELAY=-1" \
  -Dspring.datasource.driver-class-name=org.h2.Driver \
  -Dspring.datasource.username=sa \
  -Dspring.datasource.password= \
  -Dspring.jpa.hibernate.ddl-auto=update \
  -Dkal.cors.allowed-origins="http://127.0.0.1:${FRONTEND_PORT},http://localhost:${FRONTEND_PORT}" \
  -Dkal.web.base-url="http://127.0.0.1:${FRONTEND_PORT}" \
  -Dkal.mail.enabled=false \
  -jar target/kal-backend-app.jar \
  > "$LOG_DIR/backend.log" 2>&1 &
echo $! > "$RUN_DIR/backend.pid"

cd "$APP_DIR/frontend-vue"
nohup npx vite preview --host 0.0.0.0 --port "$FRONTEND_PORT" \
  > "$LOG_DIR/frontend.log" 2>&1 &
echo $! > "$RUN_DIR/frontend.pid"

log "Waiting for services"
sleep 8

log "Health checks"
set +e
curl -I "http://127.0.0.1:${BACKEND_PORT}/api/v1/public/competitions"
BACKEND_OK=$?
curl -I "http://127.0.0.1:${FRONTEND_PORT}"
FRONTEND_OK=$?
set -e

echo
echo "Deployment finished."
echo "Backend:  http://127.0.0.1:${BACKEND_PORT}/api/v1"
echo "Frontend: http://127.0.0.1:${FRONTEND_PORT}/"
echo "Logs:"
echo "  tail -f $LOG_DIR/backend.log"
echo "  tail -f $LOG_DIR/frontend.log"
echo "Stop:"
echo "  kill \$(cat $RUN_DIR/backend.pid) \$(cat $RUN_DIR/frontend.pid)"

if [[ "$BACKEND_OK" -ne 0 || "$FRONTEND_OK" -ne 0 ]]; then
  echo
  echo "One or more health checks failed. Recent logs:"
  echo "--- backend.log ---"
  tail -n 80 "$LOG_DIR/backend.log" || true
  echo "--- frontend.log ---"
  tail -n 80 "$LOG_DIR/frontend.log" || true
  exit 1
fi
