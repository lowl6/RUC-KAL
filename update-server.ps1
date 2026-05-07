param(
  [string]$Server = "39.106.213.32",
  [string]$User = "root",
  [string]$RemotePath = "/root/ruc-kal",
  [switch]$FrontendOnly,
  [switch]$SkipPull,
  [switch]$DryRun
)

$frontendOnlyFlag = if ($FrontendOnly) { "1" } else { "0" }
$skipPullFlag = if ($SkipPull) { "1" } else { "0" }

$remoteScript = @'
set -euo pipefail
FRONTEND_ONLY=__FRONTEND_ONLY__
SKIP_PULL=__SKIP_PULL__
cd '__REMOTE_PATH__'

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 is required on the server" >&2
    exit "$2"
  fi
}

load_env_file() {
  local env_file="$1"
  while IFS= read -r raw_line || [ -n "$raw_line" ]; do
    raw_line="${raw_line%$'\r'}"
    case "$raw_line" in
      ''|'#'*)
        continue
        ;;
    esac

    local key="${raw_line%%=*}"
    local value="${raw_line#*=}"
    export "$key=$value"
  done < "$env_file"
}

ENV_FILE=""
if [ -f .runtime.env ]; then
  ENV_FILE=.runtime.env
elif [ -f .env ]; then
  ENV_FILE=.env
else
  echo "Neither .runtime.env nor .env exists under __REMOTE_PATH__" >&2
  exit 2
fi

require_cmd git 3
require_cmd npm 4
require_cmd curl 5
require_cmd nginx 6

if [ "$SKIP_PULL" != "1" ]; then
  git pull --ff-only
fi

cd frontend-vue
if [ ! -d node_modules ] || [ package.json -nt node_modules/.kal-install-stamp ]; then
  npm install
  touch node_modules/.kal-install-stamp
fi
npm run build

install -d /usr/share/nginx/html/kal
cp -r dist/. /usr/share/nginx/html/kal/
chown -R root:root /usr/share/nginx/html/kal
chmod -R a+rX /usr/share/nginx/html/kal
cd ..

cat > /etc/nginx/conf.d/ruckal.conf <<'EOF'
server {
  listen 80;
  server_name ruckal.asia www.ruckal.asia 39.106.213.32;

  root /usr/share/nginx/html/kal;
  index index.html;
  client_max_body_size 20m;

  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  location / {
    try_files $uri $uri/ /index.html;
  }
}
EOF

nginx -t
systemctl reload nginx

if [ "$FRONTEND_ONLY" != "1" ]; then
  require_cmd mvn 7
  require_cmd java 8

  cd backend
  mvn -DskipTests package
  cd ..

  pkill -f 'kal-backend-app.jar' || true
  load_env_file "$ENV_FILE"
  nohup java -jar backend/target/kal-backend-app.jar >/root/kal-backend.log 2>&1 &
  sleep 6
fi

echo "ROOT=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/)"
echo "LOGIN=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/login)"
echo "API=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/api/v1/news || true)"
'@

$remoteScript = $remoteScript.Replace('__FRONTEND_ONLY__', $frontendOnlyFlag)
$remoteScript = $remoteScript.Replace('__SKIP_PULL__', $skipPullFlag)
$remoteScript = $remoteScript.Replace('__REMOTE_PATH__', $RemotePath)
$remoteScript = $remoteScript.Replace("`r`n", "`n")

if ($DryRun) {
  Write-Output $remoteScript
  exit 0
}

$remoteScript | ssh -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new "$User@$Server" "bash -s"