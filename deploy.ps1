<#
.SYNOPSIS
  一键把本地代码同步到 GitHub，再让线上服务器从 GitHub 拉取最新代码并重启。

.DESCRIPTION
  多人协作友好：始终以 GitHub origin/<branch> 为唯一可信源。
  - 本地：自动 git add/commit/push（如有改动），先 pull --rebase 避免和别人冲突；
  - 服务器：默认 git fetch + git reset --hard origin/<branch>（强制对齐 GitHub，丢弃服务器本地任意改动）；
  - 可选：把 SMTP 授权码下发到服务器 .runtime.env，并把 SMTP 锁到 465+SSL（解决大陆云出网超时）。

.EXAMPLE
  # 一键：commit + push + 服务器同步 + 重启（用 .env 里的 SMTP 账号）
  .\deploy.ps1 -Message "fix: smtp + cors"

.EXAMPLE
  # 队友已经 push，只让服务器拉取并重启
  .\deploy.ps1 -NoCommit

.EXAMPLE
  # 第一次下发 SMTP 凭据并强制走 465
  .\deploy.ps1 -SmtpUser ruc_kal@163.com -SmtpPass HRvSibsgJsMDsmuE
#>
param(
  [string]$Server = '39.106.213.32',
  [string]$User = 'root',
  [string]$RemotePath = '/root/ruc-kal',
  [string]$Branch = 'master',
  [string]$Message,
  [switch]$NoCommit,
  [switch]$NoServer,
  [switch]$FrontendOnly,
  [switch]$BackendOnly,
  [string]$SmtpUser,
  [string]$SmtpPass,
  [switch]$NoSmtpFix,
  # 让服务器走 git pull --ff-only 而不是 hard reset（一般不要）
  [switch]$SoftPull,
  [switch]$SkipPull,
  [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Info($msg)  { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Ok  ($msg)  { Write-Host "[OK]   $msg" -ForegroundColor Green }
function Write-Warn2($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err ($msg)  { Write-Host "[ERR]  $msg" -ForegroundColor Red }

function Get-EnvValue([string]$file, [string]$key) {
  if (-not (Test-Path -LiteralPath $file)) { return $null }
  foreach ($line in Get-Content -LiteralPath $file -Encoding UTF8) {
    $trim = $line.Trim()
    if ([string]::IsNullOrEmpty($trim) -or $trim.StartsWith('#')) { continue }
    $eq = $line.IndexOf('=')
    if ($eq -le 0) { continue }
    $name = $line.Substring(0, $eq).Trim()
    if ($name -eq $key) {
      return $line.Substring($eq + 1)
    }
  }
  return $null
}

if ($BackendOnly -and $FrontendOnly) {
  throw "-FrontendOnly 与 -BackendOnly 不能同时使用"
}

foreach ($cmd in @('git', 'ssh')) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    throw "需要本地安装 $cmd（PowerShell 能直接调用）"
  }
}

# ---------- 1. 本地 commit + push ----------
Push-Location $Root
try {
  if (-not (Test-Path -LiteralPath (Join-Path $Root '.git'))) {
    throw "$Root 不是一个 git 仓库"
  }

  if (-not $NoCommit) {
    Write-Info '检查本地 git 状态...'
    $statusLines = git status --porcelain
    $hasChange = -not [string]::IsNullOrWhiteSpace(($statusLines -join "`n"))

    if ($hasChange) {
      git -P status --short
      if (-not $Message) {
        $Message = Read-Host '请输入本次 commit 信息（留空则跳过 commit，仅 push 已存在的提交）'
      }
      if ([string]::IsNullOrWhiteSpace($Message)) {
        Write-Warn2 '没有提供 commit 信息，跳过 commit。'
      }
      else {
        Write-Info 'git add -A'
        git add -A | Out-Host
        Write-Info "git commit -m `"$Message`""
        git commit -m $Message | Out-Host
      }
    }
    else {
      Write-Info '工作区干净，无需 commit。'
    }

    Write-Info "git fetch + rebase origin/$Branch（避免覆盖别人的提交）..."
    git fetch origin $Branch | Out-Host
    try {
      git pull --rebase origin $Branch | Out-Host
    } catch {
      Write-Err 'rebase 出现冲突，需要你手动解决（git rebase --continue / --abort）后再运行。'
      throw
    }

    Write-Info "git push origin $Branch ..."
    git push origin $Branch | Out-Host
    Write-Ok 'GitHub 已同步。'
  }
  else {
    Write-Info '-NoCommit：跳过本地提交，直接推动服务器拉取。'
  }
}
finally {
  Pop-Location
}

if ($NoServer) {
  Write-Ok '-NoServer：完成。'
  return
}

# ---------- 2. 自动从 .env 读 SMTP 凭据（用户没传时） ----------
if (-not $SmtpUser) {
  $envSmtpUser = Get-EnvValue (Join-Path $Root '.env') 'KAL_SMTP_USER'
  if ($envSmtpUser) { $SmtpUser = $envSmtpUser }
}
if (-not $SmtpPass) {
  $envSmtpPass = Get-EnvValue (Join-Path $Root '.env') 'KAL_SMTP_PASS'
  if ($envSmtpPass) { $SmtpPass = $envSmtpPass }
}
if ($SmtpPass) {
  $masked = if ($SmtpPass.Length -ge 4) { $SmtpPass.Substring(0,2) + ('*' * ($SmtpPass.Length - 4)) + $SmtpPass.Substring($SmtpPass.Length - 2) } else { '****' }
  Write-Info "将下发 SMTP: user=$SmtpUser pass=$masked"
}

# ---------- 3. 生成服务器端 bash 并 ssh 执行 ----------
$frontendOnlyFlag = if ($FrontendOnly) { '1' } else { '0' }
$backendOnlyFlag  = if ($BackendOnly)  { '1' } else { '0' }
$skipPullFlag     = if ($SkipPull)     { '1' } else { '0' }
$softPullFlag     = if ($SoftPull)     { '1' } else { '0' }
$smtpFixFlag      = if ($NoSmtpFix)    { '0' } else { '1' }

$remoteScript = @'
set -euo pipefail
FRONTEND_ONLY=__FRONTEND_ONLY__
BACKEND_ONLY=__BACKEND_ONLY__
SKIP_PULL=__SKIP_PULL__
SOFT_PULL=__SOFT_PULL__
SMTP_FIX=__SMTP_FIX__
BRANCH='__BRANCH__'
SMTP_USER_NEW='__SMTP_USER__'
SMTP_PASS_NEW='__SMTP_PASS__'
cd '__REMOTE_PATH__'

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 is required on the server" >&2
    exit "$2"
  fi
}

# 把 KEY=VALUE 写进 / 替换文件中已有同名 key；不存在就追加。VALUE 原样写入（不再加引号）。
# 用 awk 实现，避免 sed 被 / & = 等特殊字符坑，也不依赖 python3 / perl。
upsert_env() {
  local file="$1"
  local key="$2"
  local value="$3"
  if [ ! -f "$file" ]; then
    : > "$file"
  fi
  local tmp
  tmp="$(mktemp)"
  awk -v k="$key" -v v="$value" '
    BEGIN { found = 0; klen = length(k) }
    {
      if (substr($0, 1, klen + 1) == k "=") {
        print k "=" v
        found = 1
      } else {
        print
      }
    }
    END {
      if (!found) {
        print k "=" v
      }
    }
  ' "$file" > "$tmp"
  mv "$tmp" "$file"
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

require_cmd git 3
require_cmd curl 5

# ---------- 与 GitHub 强一致 ----------
if [ "$SKIP_PULL" != "1" ]; then
  git fetch --all --prune
  if [ "$SOFT_PULL" = "1" ]; then
    echo ">>> git pull --ff-only origin/$BRANCH"
    git checkout "$BRANCH"
    git pull --ff-only origin "$BRANCH"
  else
    echo ">>> git reset --hard origin/$BRANCH (确保和 GitHub 一字不差，丢弃服务器本地改动)"
    git checkout "$BRANCH"
    git fetch origin "$BRANCH"
    git reset --hard "origin/$BRANCH"
    git clean -fdx -- frontend-vue/dist frontend-vue/dist-ssr || true
  fi
fi

# ---------- runtime.env：可选下发 SMTP 凭据 + 修正大陆云出网最稳的 465+SSL ----------
ENV_FILE=""
if [ -f .runtime.env ]; then
  ENV_FILE=.runtime.env
elif [ -f .env ]; then
  ENV_FILE=.env
else
  echo "Neither .runtime.env nor .env exists under __REMOTE_PATH__; creating .runtime.env"
  : > .runtime.env
  ENV_FILE=.runtime.env
fi

if [ "$SMTP_FIX" = "1" ]; then
  echo ">>> patching $ENV_FILE: KAL_SMTP_PORT=465 / SSL=true / STARTTLS=false / KAL_MAIL_ENABLED=true"
  upsert_env "$ENV_FILE" KAL_MAIL_ENABLED true
  upsert_env "$ENV_FILE" KAL_SMTP_HOST   smtp.163.com
  upsert_env "$ENV_FILE" KAL_SMTP_PORT   465
  upsert_env "$ENV_FILE" KAL_SMTP_SSL    true
  upsert_env "$ENV_FILE" KAL_SMTP_STARTTLS false
fi
if [ -n "$SMTP_USER_NEW" ]; then
  upsert_env "$ENV_FILE" KAL_SMTP_USER "$SMTP_USER_NEW"
  upsert_env "$ENV_FILE" KAL_MAIL_FROM "$SMTP_USER_NEW"
fi
if [ -n "$SMTP_PASS_NEW" ]; then
  upsert_env "$ENV_FILE" KAL_SMTP_PASS "$SMTP_PASS_NEW"
fi

# ---------- 前端构建 ----------
if [ "$BACKEND_ONLY" != "1" ]; then
  require_cmd npm 4
  require_cmd nginx 6
  cd frontend-vue
  if [ ! -d node_modules ] || [ package.json -nt node_modules/.kal-install-stamp ]; then
    npm install
    touch node_modules/.kal-install-stamp
  fi
  # 有些服务器上 node_modules/.bin/vite 会丢执行权限，先兜底修复。
  if [ -f node_modules/.bin/vite ] && [ ! -x node_modules/.bin/vite ]; then
    chmod +x node_modules/.bin/vite || true
  fi
  # 先走 package.json 脚本；若遇到 Permission denied，再退回 node 直调 vite.js。
  if ! npm run build; then
    echo "WARN: npm run build failed, retrying via node node_modules/vite/bin/vite.js build" >&2
    node node_modules/vite/bin/vite.js build
  fi

  install -d /usr/share/nginx/html/kal
  rm -rf /usr/share/nginx/html/kal/*
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
fi

# ---------- 后端构建 + 重启 ----------
if [ "$FRONTEND_ONLY" != "1" ]; then
  require_cmd mvn 7
  require_cmd java 8

  cd backend
  mvn -DskipTests package
  cd ..

  pkill -f 'kal-backend-app.jar' || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    if ! ss -lnt 2>/dev/null | grep -q ':8080 '; then break; fi
    sleep 1
  done

  load_env_file "$ENV_FILE"
  nohup java -jar backend/target/kal-backend-app.jar >/root/kal-backend.log 2>&1 &
  for i in $(seq 1 30); do
    sleep 1
    code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:8080/api/v1/public/news?page=1&pageSize=1" || true)
    if [ "$code" = "200" ] || [ "$code" = "204" ]; then
      echo ">>> backend ready after ${i}s"
      break
    fi
  done
fi

echo "----- smoke -----"
echo "ROOT=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/)"
echo "LOGIN=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/login)"
echo "API_NEWS=$(curl -s -o /dev/null -w '%{http_code}' 'http://127.0.0.1/api/v1/public/news?page=1&pageSize=1' || true)"
echo "API_CAPTCHA=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/api/v1/public/captcha || true)"
echo "GIT_HEAD=$(git rev-parse --short HEAD)"
'@

$remoteScript = $remoteScript.Replace('__FRONTEND_ONLY__', $frontendOnlyFlag)
$remoteScript = $remoteScript.Replace('__BACKEND_ONLY__',  $backendOnlyFlag)
$remoteScript = $remoteScript.Replace('__SKIP_PULL__',     $skipPullFlag)
$remoteScript = $remoteScript.Replace('__SOFT_PULL__',     $softPullFlag)
$remoteScript = $remoteScript.Replace('__SMTP_FIX__',      $smtpFixFlag)
$remoteScript = $remoteScript.Replace('__BRANCH__',        $Branch)
$remoteScript = $remoteScript.Replace('__SMTP_USER__',     ([string]$SmtpUser))
$remoteScript = $remoteScript.Replace('__SMTP_PASS__',     ([string]$SmtpPass))
$remoteScript = $remoteScript.Replace('__REMOTE_PATH__',   $RemotePath)
$remoteScript = $remoteScript.Replace("`r`n", "`n")

if ($DryRun) {
  Write-Output $remoteScript
  return
}

Write-Info "ssh $User@$Server -> 拉取最新代码、重新构建并重启..."
$remoteScript | ssh -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new "$User@$Server" "bash -s"
if ($LASTEXITCODE -ne 0) {
  throw "远端部署失败（ssh exit code = $LASTEXITCODE）。请查看上方日志定位错误。"
}
Write-Ok '部署完成。'
