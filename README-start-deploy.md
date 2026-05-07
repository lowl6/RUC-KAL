# KAL 更新与启动手册

本项目日常只用两个脚本：

- `local-dev.ps1`：本地启动 / 停止 / 重启 / 状态；
- `deploy.ps1`：本地代码同步到 GitHub，再让服务器对齐 GitHub 并重启。

## 1. 本地开发启动

在仓库根目录执行：

```powershell
cd c:\PROGRAMING\KAL\src

# 启动（默认 Action=Start）
.\local-dev.ps1

# 重启（先停后启，会重新构建后端）
.\local-dev.ps1 -Action Restart

# 停止
.\local-dev.ps1 -Action Stop

# 查看状态
.\local-dev.ps1 -Action Status
```

### 常用参数

```powershell
# 端口占用时自动 kill，不交互询问
.\local-dev.ps1 -Force

# 跳过后端构建
.\local-dev.ps1 -SkipBuild

# 指定端口
.\local-dev.ps1 -BackendPort 18080 -FrontendPort 15173
```

## 2. 服务器更新部署（多人协作）

### 前提：本机必须能免密 SSH 到服务器

先确认：

```powershell
ssh -o BatchMode=yes root@39.106.213.32 "echo SSH_OK"
```

如果提示 `Permission denied`，先配置 SSH key；否则 `deploy.ps1` 会快速失败，避免像普通 `ssh` 一样卡在密码提示上。

### 标准方式（推荐）

```powershell
cd c:\PROGRAMING\KAL\src
.\deploy.ps1 -Message "你的提交信息"
```

行为：

1. 本地 `git add/commit/push`；
2. 服务器执行 `git fetch` + `git reset --hard origin/master`；
3. 构建前端并发布到 Nginx；
4. 构建后端并重启 jar；
5. 输出 smoke 检查（`API_NEWS` / `API_CAPTCHA` / `GIT_HEAD`）。

### 已有人推送时（只同步服务器）

```powershell
.\deploy.ps1 -NoCommit
```

### 只更新前端或后端

```powershell
.\deploy.ps1 -FrontendOnly
.\deploy.ps1 -BackendOnly
```

## 3. 邮件验证码（163）配置

如需更新 SMTP 授权码：

```powershell
.\deploy.ps1 -NoCommit -SmtpUser ruc_kal@163.com -SmtpPass <163客户端授权码>
```

脚本会把服务器 `.runtime.env`（或 `.env`）修正为：

- `KAL_SMTP_PORT=465`
- `KAL_SMTP_SSL=true`
- `KAL_SMTP_STARTTLS=false`
- `KAL_MAIL_ENABLED=true`

## 4. 故障排查

### `release version 21 not supported`（Maven 编译失败）

后端需要 **JDK 21**，且 **`java` 与 `javac` 都必须是 21**（仅 JRE 不行）。`deploy.ps1` 会：

1. 扫描 `/usr/lib/jvm`、优先使用 `/opt/java-21-temurin`；
2. 尝试 `dnf` / `yum` / `apt` 安装 `java-21-openjdk-devel` 或 `openjdk-21-jdk`；
3. 仍失败则从 Adoptium 下载压缩包（直连失败会再试 `mirror.ghproxy.com` 前缀）解压到 `/opt/java-21-temurin`。

构建阶段会执行 `env JAVA_HOME=... mvn ...`，避免系统默认仍指向 JDK8。

**注意**：若服务器 `.env` 里写了错误的 `JAVA_HOME`（例如 JDK8），脚本会在启动 jar 前恢复为本次检测到的 JDK21，避免运行时误用旧 Java。

若全自动仍失败，请 SSH 手动安装后重试 `deploy.ps1`：

```bash
ls /usr/lib/jvm
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk   # 以实际目录为准
export PATH="$JAVA_HOME/bin:$PATH"
java -version && javac -version   # 均需 21.x
```

或设置自定义包地址后重跑部署（在**服务器**上 `export` 后需要把逻辑写进脚本，更简单是手动解压到 `/opt/java-21-temurin`）：

```bash
export KAL_BOOTSTRAP_JDK_URL='https://.../OpenJDK21U-jdk_x64_linux_hotspot....tar.gz'
```

### `vite: Permission denied`

`deploy.ps1` 已内置修复：自动 `chmod +x node_modules/.bin/vite`，若仍失败会 fallback 到：

```bash
node node_modules/vite/bin/vite.js build
```

### `deploy.ps1` 显示成功但实际失败

已修复：脚本现在会检查 `ssh` 退出码，远端失败会直接抛错，不会误报“部署完成”。

