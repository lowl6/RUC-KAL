#!/usr/bin/env bash
# 在校园网 / 境内线路访问 Docker Hub 超时时，给 Docker Engine 配置 registry mirror。
# 用法（root）：bash deploy/configure-docker-mirror.sh
set -euo pipefail

MIRRORS_JSON="${MIRRORS_JSON:-}"

if [[ -z "${MIRRORS_JSON}" ]]; then
  # 可按环境替换；多个地址会依次尝试（具体行为取决于 Docker 版本）。
  MIRRORS_JSON='["https://docker.m.daocloud.io","https://dockerproxy.cn","https://mirror.ccs.tencentyun.com"]'
fi

mkdir -p /etc/docker

if [[ -f /etc/docker/daemon.json ]] && [[ -s /etc/docker/daemon.json ]] && [[ "${FORCE_MIRROR:-0}" != "1" ]]; then
  echo "已存在非空 /etc/docker/daemon.json，未覆盖。"
  echo "请在里面加入或合并 registry-mirrors，例如："
  echo "  \"registry-mirrors\": ${MIRRORS_JSON}"
  echo "保存后执行: systemctl restart docker"
  echo "若确认可覆盖：FORCE_MIRROR=1 bash deploy/configure-docker-mirror.sh"
  exit 1
fi

if [[ "${FORCE_MIRROR:-0}" == "1" ]] && [[ -f /etc/docker/daemon.json ]]; then
  cp -a /etc/docker/daemon.json "/etc/docker/daemon.json.bak.$(date +%s)"
fi

cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": ${MIRRORS_JSON}
}
EOF

if systemctl is-active --quiet docker 2>/dev/null; then
  systemctl restart docker
fi

echo "已写入 /etc/docker/daemon.json 并尝试重启 docker。"
docker info 2>/dev/null | grep -A3 'Registry Mirrors' || true
