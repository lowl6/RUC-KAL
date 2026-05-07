# 知行创坊 KNOWACT Lab (KAL)

知行创坊（KNOWACT Lab，简称 KAL）是面向中国人民大学师生的轻量级"三创"（创新 / 创造 / 创业）组队与交流平台。
平台核心解决"项目找人"与"个人找项目"的双向匹配问题，并提供比赛资讯、经验分享与站内私信能力：

- **找项目**：项目负责人发布项目卡（含比赛目标、缺人角色、技能要求、时间投入），有意向的同学浏览推荐流后通过站内私信沟通；
- **找队友**：有技能的同学发布个人卡（含技能标签、可投入时间、感兴趣的比赛），项目负责人浏览推荐流后定向邀请；
- **比赛中心**：聚合校内外可报名的"三创"类比赛信息与官方资讯；
- **创坊论坛**：分享比赛经验、答辩技巧、技术资源与提问交流的社区空间；
- **私信**：站内文字沟通，支持快捷换微信、附带项目卡邀请；过期机制保障隐私；
- **个人中心**：管理自己的项目卡 / 个人卡 / 私信 / 设置；
- **后台管理**：运营审核违规内容、维护比赛与资讯、查看运营看板。

> 设计目标：服务规模 **约 1000 名师生**（在校规模 + 长尾活跃），日活预估 200~400，
> 同时在线 50~100，私信 / 推荐流为最高频访问场景。系统架构需覆盖「读多写少 + 长尾活跃」的负载特征，详见 [`docs/design.md`](docs/design.md)。

---

### 一键操作（Windows · PowerShell）

整个项目只需要两个脚本：`local-dev.ps1`（本地）+ `deploy.ps1`（部署）。

```powershell
# === 脚本 1：本地一键启动 / 停止 / 重启 / 状态 ===
# 端口被占会交互式询问：杀进程 / 换端口 / 放弃；自动注入本地 CORS。
powershell -ExecutionPolicy Bypass -File local-dev.ps1                  # 启动（默认）
powershell -ExecutionPolicy Bypass -File local-dev.ps1 -Action Restart  # 重启（重新构建）
powershell -ExecutionPolicy Bypass -File local-dev.ps1 -Action Stop     # 停止
powershell -ExecutionPolicy Bypass -File local-dev.ps1 -Action Status   # 状态
powershell -ExecutionPolicy Bypass -File local-dev.ps1 -Force           # 占端口直接 kill 不询问

# === 脚本 2：本地 → GitHub → 服务器同步 → 重启（多人协作友好） ===
# 自动 commit/push，然后 ssh 到 39.106.213.32 走 git reset --hard origin/master，
# 重建前端 + 后端，并按需把 SMTP 授权码下发到服务器 .runtime.env。
powershell -ExecutionPolicy Bypass -File deploy.ps1 -Message "你的提交信息"
powershell -ExecutionPolicy Bypass -File deploy.ps1 -NoCommit            # 仅同步服务器
powershell -ExecutionPolicy Bypass -File deploy.ps1 -FrontendOnly        # 只更新前端
powershell -ExecutionPolicy Bypass -File deploy.ps1 -BackendOnly         # 只重启后端
powershell -ExecutionPolicy Bypass -File deploy.ps1 -SmtpUser ruc_kal@163.com -SmtpPass <163客户端授权码>
```

> 注：`deploy.ps1` 不传 `-SmtpUser/-SmtpPass` 时会**自动**从本地 `.env` 读 `KAL_SMTP_USER` / `KAL_SMTP_PASS` 下发到服务器，所以改完本地 `.env` 直接 `deploy.ps1 -NoCommit` 就能把新授权码同步过去。

## 项目目录

```
KAL/src/
├── frontend/                      早期静态原型（HTML，仅作设计参考）
├── frontend-vue/                  ⭐ Vue 3 前端（含用户端 + 管理员后台）
│   ├── public/icons/              图标 / 人大 logo
│   ├── src/
│   │   ├── api/                   axios 客户端 + 各业务 API（auth / projects / admin ...）
│   │   ├── components/            Icon、卡片等通用组件
│   │   ├── stores/                Pinia 状态：auth（真实登录态） / user（旧 mock）
│   │   ├── views/                 用户端页面（首页 / 项目 / 比赛 / 论坛 / 私信 / 我的）
│   │   └── views/admin/           ⭐ 管理员后台（Login / Layout / Dashboard / Users / Projects / Forum / Competitions / Audit）
│   └── package.json               依赖：vue 3 / pinia / vue-router / axios
├── backend/                       ⭐ Spring Boot 3 后端（H2 内存库，开箱即用）
│   ├── pom.xml                    Maven，JDK 21
│   └── src/main/java/cn/edu/ruc/kal/
│       ├── KalApplication.java    入口
│       ├── common/                通用：ApiResponse / BizException / GlobalExceptionHandler / PageResult
│       ├── security/              JWT：JwtUtil / JwtAuthFilter / SecurityConfig / CurrentUser
│       ├── auth/                  登录 / 注册 / 管理员登录 / /auth/me
│       ├── user/                  User 实体 + 仓储 + Controller
│       ├── project/               项目卡 + 角色子表
│       ├── personalcard/          个人卡
│       ├── competition/           比赛信息
│       ├── forum/                 论坛帖子
│       ├── message/               会话 + 私信
│       ├── admin/                 ⭐ 管理后台（仪表盘 / 用户 / 内容治理 / 比赛 / 审计日志）
│       └── seed/DataSeeder.java   ⭐ 启动时通过代码灌入种子数据（管理员 / 学生 / 项目 / 比赛 / 论坛）
├── docs/
│   ├── design.md                  系统架构与技术选型设计文档
│   ├── html-modifications.md      前端改造指引
│   └── er-diagram.puml            数据库 ER 图（PlantUML）
├── 前端页面说明.md
├── 知行创坊.docx                   产品需求原始文档
└── ReadeMe.md                     本文件（含运行说明）
```

---

## 快速运行（Windows · PowerShell）

### 0. 环境

| 工具    | 版本要求            |
| ----- | --------------- |
| JDK   | 21（项目实测 21.0.6） |
| Maven | 3.9+            |
| Node  | 18+             |
| npm   | 9+              |

### 1. 启动后端（含 H2 内存数据库 + 自动灌种子）

```powershell
cd backend
mvn -DskipTests package
java -jar target/kal-backend-app.jar
```

启动后将看到：

```
[seed] inserting demo data...
[seed] done. users=9, projects=3, cards=3, forum=3, competitions=4
Started KalApplication in ~12 seconds
Tomcat started on port 8080
```

- 服务地址：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`
  - JDBC URL：`jdbc:h2:mem:kal`，用户名 `sa`，密码留空。
  - **注意**：H2 是内存库，**进程关闭后数据丢失**；下次启动会重新灌入相同的种子数据。
  - 切换到 MySQL：使用 `SPRING_PROFILES_ACTIVE=mysql`，详见下方「MySQL 与 Docker Compose」。

### 2. 启动前端

```powershell
cd frontend-vue
npm install
npm run dev
```

- 用户端：`http://localhost:5173/`
- 登录页：`http://localhost:5173/login`
- **管理员后台**：`http://localhost:5173/admin/login`

> 前端通过 `VITE_API_BASE` 环境变量可改后端地址，默认 `http://localhost:8080/api/v1`。

### 3. Windows 本地一键启停

用 `local-dev.ps1` 一个脚本就够（默认 Action = Start）：

```powershell
cd c:\PROGRAMING\KAL\src

.\local-dev.ps1                       # 启动（默认）
.\local-dev.ps1 -Action Restart       # 重新构建并重启
.\local-dev.ps1 -Action Stop          # 停止
.\local-dev.ps1 -Action Status        # 查看状态

# 可选参数
.\local-dev.ps1 -SkipBuild            # 跳过 mvn package
.\local-dev.ps1 -Force                # 端口被占自动 kill 不再交互询问
.\local-dev.ps1 -BackendPort 18080 -FrontendPort 15173
```

脚本做的事：

- 启动前检测 8080 / 5173 是否被占，被占则询问 `[k] 杀进程 / [n] 换端口 / [a] 放弃`；
- 自动把本地 CORS（含 5173/5174/3000 等）注入到后端，避免根目录 `.env` 的服务器 CORS 把本地验证码拦掉；
- 没有 boot jar 时自动回退到 `mvn spring-boot:run`；
- 日志写到 `.local-run/backend.log` / `frontend.log`；
- 等 `http://localhost:8080/api/v1/public/news` 和 `http://localhost:5173/login` 双双就绪才返回成功。

---

## MySQL 与 Docker Compose

### 服务器部署（Docker Compose）

仓库地址：`https://github.com/lowl6/RUC-KAL.git`

第一次部署到服务器：

```bash
curl -fsSL https://raw.githubusercontent.com/lowl6/RUC-KAL/master/deploy/server-bootstrap.sh -o /tmp/kal-bootstrap.sh
bash /tmp/kal-bootstrap.sh
```

部署完成后访问：

- 前台：`http://47.238.235.51:3000/`
- 管理后台：`http://47.238.235.51:3000/admin/login`

后续更新：

```bash
cd /opt/ruc-kal
bash deploy/update.sh
```

### 服务器部署（无 Docker）

适用于 **拉 Docker Hub 很慢或被墙**：用 apt 安装 JDK / MySQL / Nginx，Node 使用二进制包（默认从 npmmirror 下载），**不拉容器镜像**。

```bash
cd /opt/ruc-kal
git pull
sed -i 's/\r$//' deploy/run-native.sh deploy/update-native.sh
bash deploy/run-native.sh
```

- 静态站与反代：本机 **80** 端口（`http://<服务器IP>/`）
- 后端由 systemd 服务 `kal-backend` 管理；环境变量在 `/opt/ruc-kal/.env.native`
- 后续更新：`bash deploy/update-native.sh`

### 校园网 / 无 sudo 用户态部署

适用于 **校园网 Linux 主机 / 实验室机器 / 个人账号没有 root 权限** 的场景。该脚本会：

- 在 `$HOME/apps` 安装 JDK / Maven / Node；
- 后端仅监听 `127.0.0.1:${BACKEND_PORT:-18080}`；
- 前端监听 `0.0.0.0:${FRONTEND_PORT:-18000}`，并在同一端口下把 `/api/*` 反代到本机后端；
- 首次运行自动生成 `.env.user-space`，其中 `PUBLIC_WEB_BASE_URL` 需要改成校园服务器实际 IP 或域名。

```bash
cd ~/ruc-kal
sed -i 's/\r$//' deploy/user-space-deploy.sh
bash deploy/user-space-deploy.sh
```

完成后访问：

- 前台：`http://<校园服务器IP>:18000/`
- 管理后台：`http://<校园服务器IP>:18000/admin/login`

若首次生成的是本机回环地址，请编辑 `~/ruc-kal/.env.user-space` 中的 `PUBLIC_WEB_BASE_URL` 后重新运行脚本。对外通常只需要放行 `FRONTEND_PORT`，无需暴露后端端口。

生产密钥保存在服务器上，不要提交到 GitHub。若要开启真实邮件发送，编辑 `.env` 或 `.env.native`：

```bash
KAL_MAIL_ENABLED=true
KAL_SMTP_USER=<SMTP邮箱账号>
KAL_SMTP_PASS=<SMTP客户端授权码>
KAL_MAIL_FROM=KAL 知行创坊 <SMTP邮箱账号>
```

### 当前线上服务器快速更新（39.106.213.32）

当前线上环境已经改为：

- Nginx 直接托管前端静态文件到 `/usr/share/nginx/html/kal`
- `/api/*` 反代到本机 Spring Boot `127.0.0.1:8080`
- 代码仓库位于服务器 `/root/ruc-kal`

推荐：本地 Windows 直接执行 `deploy.ps1`（一键 commit + push + 服务器同步 + 重启）：

```powershell
cd c:\PROGRAMING\KAL\src

# 一键：commit + push to GitHub + 服务器从 GitHub 拉取并重启
.\deploy.ps1 -Message "fix: ..."

# 已经手动 push 过，只让服务器同步
.\deploy.ps1 -NoCommit

# 不真的执行，只看会下发到服务器的 bash 脚本
.\deploy.ps1 -DryRun
```

`deploy.ps1` 在服务器端默认执行：

- `git fetch && git reset --hard origin/master`（多人协作模式：服务器永远 = GitHub，丢弃服务器本地改动）；担心覆盖加 `-SoftPull` 改回 `git pull --ff-only`；
- 把本地 `.env` 里的 `KAL_SMTP_USER` / `KAL_SMTP_PASS` 写进服务器 `/root/ruc-kal/.runtime.env`，并把 `KAL_SMTP_PORT / SSL / STARTTLS` 锁到 `465 / true / false`（修复阿里云大陆机出网 587/25 被拦超时的问题）；不需要时加 `-NoSmtpFix`；
- 构建 `frontend-vue` → 发布到 Nginx 目录 → 重载 Nginx；
- 构建 `backend` → kill 旧 jar → 加载 env → `nohup java -jar` 重启；
- 末尾打印 `ROOT / LOGIN / API_NEWS / API_CAPTCHA` 状态码 + `GIT_HEAD` 短哈希做 smoke 测。

常用参数：

```powershell
.\deploy.ps1 -FrontendOnly    # 只更新前端 + Nginx
.\deploy.ps1 -BackendOnly     # 只重启后端
.\deploy.ps1 -NoSmtpFix       # 保留服务器现有邮件配置不动
.\deploy.ps1 -SkipPull        # 跳过服务器 git fetch/reset（仅本机已 push）
.\deploy.ps1 -SmtpUser ruc_kal@163.com -SmtpPass <新授权码>   # 显式下发凭据
```

前提：

- 本地装好 `git`、`ssh`，可以 `ssh root@39.106.213.32` 免密登录；
- 服务器装好 `git`、`npm`、`mvn`、`java`、`nginx`；
- 服务器仓库目录为 `/root/ruc-kal`，后端运行所需配置在 `/root/ruc-kal/.runtime.env` 或 `/root/ruc-kal/.env`。

### 本机 MySQL 运行

后端默认使用 H2 内存库；如需连接本机 MySQL，先创建数据库：

```sql
CREATE DATABASE kal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'kal'@'%' IDENTIFIED BY '<替换为强密码>';
GRANT ALL PRIVILEGES ON kal.* TO 'kal'@'%';
FLUSH PRIVILEGES;
```

然后启动：

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/kal?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:MYSQL_USER="kal"
$env:MYSQL_PASSWORD="<替换为上面的强密码>"
mvn spring-boot:run
```

### Docker Compose 一键启动

项目根目录已提供 `docker-compose.yml`，包含：

- `mysql`：MySQL 8.4，数据卷 `kal-mysql-data`
- `backend`：Spring Boot，使用 `mysql` profile
- `frontend`：Nginx 托管 Vue 构建产物，并将 `/api/*` 反向代理到后端

```powershell
cd c:\PROGRAMING\KAL\src
copy .env.example .env
# 编辑 .env，替换 MYSQL_PASSWORD / MYSQL_ROOT_PASSWORD / KAL_JWT_SECRET 等生产密钥
docker compose up --build
```

访问地址：

- 前台：`http://localhost:3000/`
- 管理后台：`http://localhost:3000/admin/login`
- 后端：`http://localhost:8080/`
- MySQL：`localhost:3306`，数据库 `kal`，用户由 `.env` 中的 `MYSQL_USER` 指定

停止并保留数据：

```powershell
docker compose down
```

停止并清空 MySQL 数据卷：

```powershell
docker compose down -v
```

---

## 默认账号（来自种子数据）

> 管理员入口在前端不再展示明显链接，需通过下列两种方式之一进入：
> 1. 直接访问 `http://localhost:5173/admin/login`；
> 2. 在登录页左侧的「KAL · 知行创坊」品牌标识上**连击 5 次**触发隐藏入口。
>
> 强烈建议**首次登录后立即修改下列默认管理员密码**。

| 角色            | 邮箱                              | 密码                       | 备注                           |
| ------------- | ------------------------------- | ------------------------ | ---------------------------- |
| **超级管理员**     | `kal-superadmin@ruc.edu.cn`     | `Kal#Super-2026@Admin`   | 全部权限，可改其他人角色 |
| **管理员**       | `kal-ops@ruc.edu.cn`            | `Kal#Ops-2026@Console`   | 内容治理 / 比赛维护 |
| 学生            | `li.yan@ruc.edu.cn`             | `Kal@2026`               | 信息学院 2023 级 |
| 学生            | `zhao.xin@ruc.edu.cn`    | `Kal@2026`  | 商学院 2023 级 |
| 学生            | `chen.yu@ruc.edu.cn`     | `Kal@2026`  | 新闻学院 2022 级 |
| 学生            | `wang.qi@ruc.edu.cn`     | `Kal@2026`  | 财政金融 2024 级 |
| 学生            | `sun.meng@ruc.edu.cn`    | `Kal@2026`  | 信息学院 2023 级 |
| 学生            | `liu.xin@ruc.edu.cn`     | `Kal@2026`  | 艺术学院 2023 级 |
| 教师            | `lin.shu@ruc.edu.cn`     | `Kal@2026`  | 商学院 |

---

## 安全 / 通知 / 邀请（v0.2 新增能力）

### 1. 图形验证码（人大红风格 SVG）

- 接口 `GET /api/v1/public/captcha` 返回 `{ id, svg }`，前端 `<Captcha v-model="captcha" />` 直接显示并支持点击换一张；
- 登录、管理员登录、注册、发送邮箱验证码 4 个写入端点都强制校验，防止脚本撞库。

### 2. 邮箱验证码注册

- 注册流程：填校内邮箱 → 完成图形验证码 → 点击「发送验证码」（60s 冷却） → 收到 6 位数字 → 提交注册；
- 后端通过 `MailService` 发送，生产环境通过 `.env` / 环境变量配置 SMTP 用户与客户端授权码；
- 当 SMTP 由于网络 / 端口 / TLS 等原因投递失败时，会自动**降级为日志输出**——`backend-run.log` 中的 `=========== EMAIL (FALLBACK) ===========` 段可看到完整邮件正文（含 6 位验证码），用于本地联调。

#### SMTP 端口与配置

| 场景 | 推荐端口 | 协议 | 备注 |
| --- | --- | --- | --- |
| 公网服务器 / 云主机 | `465` | SSL | 默认配置 |
| 出网 SMTP 受限环境 | `25` | STARTTLS | 设 `KAL_SMTP_PORT=25 KAL_SMTP_SSL=false KAL_SMTP_STARTTLS=true` |
| 家用 / 校园网常被屏蔽 | — | — | 多数 ISP / 校园网会拦截出站 25/465，需切换网络或部署到云端 |

可通过环境变量覆盖（不动代码即可改 SMTP）：

```powershell
$env:KAL_SMTP_HOST = 'smtp.163.com'
$env:KAL_SMTP_PORT = '465'
$env:KAL_SMTP_USER = '<SMTP邮箱账号>'
$env:KAL_SMTP_PASS = '<SMTP客户端授权码>'
$env:KAL_MAIL_ENABLED = 'true'
java -jar target/kal-backend-app.jar
```

如想观察完整 SMTP 协议交互，可在 `application.yml` 中放开 `debug: true`。

### 3. 私信通知（小红点 + 邮件）

- 顶栏消息图标会在有未读时显示红色徽标，登录后自动每 30 秒轮询；
- 进入对话页或调用 `POST /messages/conversations/{id}/read` 即标记已读；
- 在「我的中心 · 通知设置」里可开启**新私信邮件提醒**；开启后对方发来私信会触发邮件（默认走日志）。

### 4. 隐藏的管理员入口

- 前台不再展示「管理员入口」链接；
- 进入后台两种方式：直接访问 `/admin/login`，或在登录页左侧 KAL 品牌处**连击 5 次**触发跳转。

### 5. 管理员邀请校外用户

- 在「管理后台 → 账号管理 → 手动添加账号」可填**任意邮箱**（含 `@gmail.com` 等）；
- 留空初始密码时系统自动生成形如 `Kal-A7q9-Pn3K` 的强密码，并在创建后展示一次（带「复制」按钮）；
- 创建结果会在 `AuditLog` 中标注「（校外邀请）」前缀，便于审计。

---

## 主要 API 一览（节选）

| 方法     | 路径                                                | 说明                |
| ------ | ------------------------------------------------- | ----------------- |
| GET    | `/api/v1/public/captcha`                          | 取图形验证码（SVG，RUC 红色系）|
| POST   | `/api/v1/auth/email-code`                         | 发送邮箱验证码（需校内邮箱 + 图形验证码）|
| POST   | `/api/v1/auth/register`                           | @ruc.edu.cn 邮箱注册（需图形 + 邮箱验证码）|
| POST   | `/api/v1/auth/login`                              | 普通登录（需图形验证码）|
| POST   | `/api/v1/auth/admin-login`                        | 管理员登录（需图形验证码）|
| GET    | `/api/v1/auth/me`                                 | 当前用户信息           |
| PATCH  | `/api/v1/auth/notify-email`                       | 开关「新私信邮件提醒」    |
| GET    | `/api/v1/messages/unread-count`                   | 当前用户未读私信总数     |
| POST   | `/api/v1/messages/conversations/{id}/read`        | 将会话标记为已读        |
| GET    | `/api/v1/public/projects`                         | 项目卡列表（分页 / 搜索）   |
| POST   | `/api/v1/projects`                                | 创建项目卡（需登录）       |
| GET    | `/api/v1/public/personal-cards`                   | 个人卡列表            |
| POST   | `/api/v1/personal-cards`                          | 创建 / 更新自己的个人卡    |
| GET    | `/api/v1/public/competitions`                     | 比赛列表             |
| GET    | `/api/v1/public/forum/posts`                      | 论坛帖子列表           |
| POST   | `/api/v1/forum/posts`                             | 发帖              |
| GET    | `/api/v1/messages/conversations`                  | 我的会话             |
| POST   | `/api/v1/messages/conversations/{otherUserId}`    | 打开 / 创建会话       |
| POST   | `/api/v1/messages/conversations/{id}/send`        | 发私信             |
| GET    | `/api/v1/admin/stats`                             | 管理员仪表盘           |
| GET    | `/api/v1/admin/users`                             | 用户列表（搜索 / 筛选）   |
| POST   | `/api/v1/admin/users`                             | 手动添加账号           |
| PATCH  | `/api/v1/admin/users/{id}/status`                 | 改账号状态（封禁等）     |
| PATCH  | `/api/v1/admin/users/{id}/role`                   | 改角色（仅超级管理员）     |
| POST   | `/api/v1/admin/users/{id}/reset-password`         | 重置密码             |
| GET/PATCH | `/api/v1/admin/projects/**`                    | 项目卡治理            |
| GET/PATCH | `/api/v1/admin/forum/posts/**`                 | 论坛治理（隐藏 / 删除 / 置顶） |
| GET/POST/DELETE | `/api/v1/admin/competitions/**`          | 比赛 CRUD          |
| GET    | `/api/v1/admin/audit-logs`                        | 审计日志             |

> 所有响应统一为 `{ code, message, data }` 形态；`code = 0` 表示成功。
> 鉴权头：`Authorization: Bearer <jwt>`；JWT 有效期默认 480 分钟（8 小时）。

---

# 知行创坊 KAL 功能规范文档

按现有 9 个前端页面 + 管理后台重新组织为 **9 个一级模块**：

| #   | 模块             | 关键页面 / 路由                                                     | 用户角色          |
| --- | ---------------- | ------------------------------------------------------------------- | ----------------- |
| 1   | 用户与认证       | `login.html`                                                        | 全体              |
| 2   | 项目卡系统       | `publish-project.html` / `project-detail.html`                      | student / teacher |
| 3   | 个人卡系统       | `publish-profile.html`                                              | student / teacher |
| 4   | 推荐流与检索     | `index.html`（找项目 / 找队友 双 Tab）                              | 全体              |
| 5   | 私信系统         | `messages.html`                                                     | student / teacher |
| 6   | 比赛中心 & 资讯  | `competitions.html`                                                 | 全体只读 + admin 维护 |
| 7   | 创坊论坛         | `forum.html`                                                        | 全体              |
| 8   | 我的中心         | `my-center.html`                                                    | student / teacher |
| 9   | **管理后台**     | `admin/*`（独立子站点，Vue 单独路由，详见 [模块九](#模块九管理后台) 与 [`docs/html-modifications.md`](docs/html-modifications.md)） | admin / super_admin |

---

## 模块一：用户与认证

### 功能名称：注册 / 登录

**简述：**
平台支持 **两类账号来源**：

1. **普通用户（student / teacher）**：以 `@ruc.edu.cn` 校内邮箱在 `login.html` 登录，首次自动注册（白名单仅放行该后缀）。后续可扩展人大统一身份认证（CAS）。
2. **管理员账号（admin / super_admin）**：**不开放**自助注册；只能由 `super_admin` 在管理后台 [模块九](#模块九管理后台) 手动添加（也可由 super_admin 创建 / 邀请其他临时账号，如外部评委、合作老师等，邮箱后缀不限）。

> 同一个登录入口、同一套 `User` 表、同一份 JWT；登录成功后由 `role` 字段决定首页跳转：student / teacher → `/`；admin / super_admin → `/admin`。

**所需数据（统一 `User` 表）：**

| 字段名             | 类型      | 必填     | 说明                                                         |
|--------------------|-----------|----------|--------------------------------------------------------------|
| user_id            | String    | 系统生成 | 内部用户 ID（雪花 / UUID）                                   |
| email              | String    | 是       | 登录邮箱，唯一索引（普通用户必须 `@ruc.edu.cn`；admin-created 用户不限） |
| password_hash      | String    | 系统     | bcrypt（普通用户首次登录由"邮箱验证码 / 一次性链接"设置）    |
| name               | String    | 是       | 真实姓名（不公开展示）                                       |
| display_name       | String    | 否       | 平台展示昵称（个人卡 / 论坛 / 私信均使用此字段）             |
| ruc_id             | String    | 否       | 学工号（CAS 接入后回填；admin-created 用户可留空）           |
| dept_name          | String    | 否       | 院系名（冗余）                                               |
| grade              | String    | 否       | 年级，如"2022级"                                             |
| role               | Enum      | 是       | `student` / `teacher` / `admin` / `super_admin`              |
| admin_perms        | JSON      | 条件     | 仅 `role IN (admin, super_admin)` 时生效，权限码列表（见 [§9.2](#92-角色与权限模型rbac)） |
| source             | Enum      | 系统     | `self_register`（邮箱自助）/ `admin_created`（后台手动添加）/ `cas`（二期 CAS 单点） |
| created_by_admin_id| String    | 条件     | 当 `source=admin_created` 时记录创建者 admin 的 user_id（审计） |
| status             | Enum      | 系统     | `active` / `disabled`（管理员停用） / `banned`（违规封禁） / `pending_first_login`（admin 邀请待激活） |
| ban_reason         | String    | 否       | 封禁原因（关联 `Report` / `AuditLog`）                        |
| ban_expire_at      | Timestamp | 否       | 封禁到期时间（NULL = 永久）                                  |
| phone              | String    | 否       | 手机号                                                       |
| wechat_id          | String    | 否       | 预填微信号                                                   |
| avatar_url         | String    | 否       | 头像                                                         |
| last_login_at      | Timestamp | 系统     | 最近登录时间                                                 |
| last_login_ip      | String    | 系统     | 最近登录 IP（管理员审计用）                                   |
| created_at         | Timestamp | 系统     | 注册 / 创建时间                                              |
| updated_at         | Timestamp | 系统     |                                                              |

**登录态：** 服务端签发 JWT（access 30min + refresh 7d），refresh token 存 Redis 黑名单可立即吊销。
管理员 JWT 在 `claims` 内带 `is_admin=true` 与 `perms[]`，**敏感接口必须二次校验权限码**而非仅看 `role`，便于运行时回收某条权限。

**首登激活流程（admin-created 账号）：**

1. super_admin 在 `/admin/users/new` 填邮箱、姓名、角色、权限模板 → 系统生成 `User` 记录，`status=pending_first_login`；
2. 系统生成 24h 一次性激活链接（含 `activation_token`），通过邮件 / 平台外渠道发给本人；
3. 用户访问链接 → 设置初始密码 → `status` 转 `active`。

---

### 功能名称：用户设置（隐私 + 通知）

**所需数据：**

| 字段名                     | 类型      | 默认  | 说明                                      |
|----------------------------|-----------|-------|-------------------------------------------|
| user_id                    | String    | 系统  | 当前用户                                  |
| privacy_show_dept          | Boolean   | true  | 个人卡是否展示院系                        |
| privacy_show_grade         | Boolean   | true  | 个人卡是否展示年级                        |
| privacy_show_realname      | Boolean   | false | 是否展示真实姓名（默认仅展示昵称）        |
| notify_new_apply           | Boolean   | true  | 收到新申请推送                            |
| notify_new_message         | Boolean   | true  | 收到新私信推送                            |
| notify_deadline_remind     | Boolean   | true  | 组队 / 比赛截止提醒                       |
| notify_forum_reply         | Boolean   | true  | 论坛帖子被回复推送                        |
| wechat_id                  | String    | -     | 预填微信号（私信"发送微信"按钮用）        |

---

## 模块二：项目卡系统（项目找人）

### 功能名称：项目卡发布

**简述：** 项目负责人填写项目基本信息、比赛目标、所需角色，生成项目卡进入推荐流。

**所需数据：**

| 字段名                      | 类型           | 必填   | 说明                                                  |
|-----------------------------|----------------|--------|-------------------------------------------------------|
| project_id                  | String         | 系统   | 唯一标识                                              |
| creator_id                  | String         | 系统   | 发布人 user_id                                        |
| project_name                | String(20)     | 是     | 项目名称                                              |
| one_liner                   | String(30)     | 是     | 一句话介绍                                            |
| project_type                | Enum           | 是     | innovation / creation / entrepreneurship              |
| competition_id              | String         | 否     | 关联 `Competition` 主键（无则填"无特定比赛"）         |
| competition_target          | Enum           | 是     | dachuan / internet / challenge / jingcai / other / none |
| competition_deadline        | Date           | 是     | 比赛报名截止日期                                      |
| team_deadline               | Date           | 是     | 组队截止（默认 = 比赛截止前 14 天）                   |
| current_members             | Int            | 是     | 已有成员数（含自己），1~20                            |
| weekly_hours                | Int            | 否     | 预计每周投入小时（1~40）                              |
| project_detail              | String(500)    | 否     | 项目详情                                              |
| contact_wechat              | String         | 否     | 负责人微信号（可选填）                                |
| status                      | Enum           | 系统   | recruiting / completed / closed                       |
| view_count                  | Int            | 系统   | 浏览次数                                              |
| apply_count                 | Int            | 系统   | 申请数                                                |
| created_at / updated_at     | Timestamp      | 系统   |                                                       |

**子表 `ProjectRole`（所需角色，1:N）：**

| 字段名      | 类型        | 必填 | 说明                                |
|-------------|-------------|------|-------------------------------------|
| role_id     | String      | 系统 | 主键                                |
| project_id  | String      | 是   | 外键                                |
| role_name   | String      | 是   | 角色名称，如"后端开发"              |
| count       | Int         | 是   | 需要人数                            |
| skills      | String(100) | 否   | 技能要求，如"Python, Django"        |
| filled      | Int         | 系统 | 已招到人数                          |

**子表 `ProjectTag`（技术 / 业务标签，1:N，从池中选 / 自由输入）：**

| 字段名      | 类型     | 必填 | 说明                  |
|-------------|----------|------|-----------------------|
| project_id  | String   | 是   |                       |
| tag         | String   | 是   | 单个标签              |
| tag_type    | Enum     | 是   | role / skill          |

---

### 功能名称：项目卡编辑与状态管理

| 字段名      | 类型   | 必填 | 说明                                                  |
|-------------|--------|------|-------------------------------------------------------|
| project_id  | String | 是   | 目标项目                                              |
| operator_id | String | 系统 | 仅 `creator_id` 或管理员可改                          |
| edit_fields | Object | 条件 | 需修改的字段集合                                      |
| new_status  | Enum   | 否   | recruiting → completed / closed                       |
| edit_log    | String | 系统 | 写入审计日志                                          |

> 状态变更后该卡从推荐流下架，仅保留详情页可访问。

---

### 功能名称：项目详情页

**输出数据：** 在项目卡完整字段基础上额外返回：

| 字段名             | 类型          | 说明                                  |
|--------------------|---------------|---------------------------------------|
| creator_summary    | Object        | 创建者昵称 / 院系 / 年级（受隐私控制）|
| roles              | Array[Object] | 所需角色及已填进度                    |
| tags               | Array[String] | 技术 / 业务标签                       |
| stats              | Object        | view_count / apply_count / weekly_hours / current_members |
| related_projects   | Array[Object] | 相关项目推荐（同比赛 / 同标签 Top 4） |
| can_apply          | Boolean       | 当前用户是否可申请（自己 / 已申请则 false） |
| can_edit           | Boolean       | 当前用户是否为创建者                  |

---

### 功能名称：项目申请

**简述：** 用户在详情页或推荐流点击"私信负责人 / 申请加入"时，可生成一条"申请记录"，并自动开启一条会话。

**所需数据（`Application`）：**

| 字段名             | 类型      | 必填 | 说明                                              |
|--------------------|-----------|------|---------------------------------------------------|
| application_id     | String    | 系统 |                                                   |
| project_id         | String    | 是   |                                                   |
| applicant_id       | String    | 系统 | 申请人                                            |
| target_role_id     | String    | 否   | 期望角色（来自 ProjectRole）                      |
| message            | String    | 否   | 申请留言                                          |
| status             | Enum      | 系统 | pending / accepted / rejected / withdrawn         |
| conversation_id    | String    | 系统 | 自动创建的私信会话                                |
| created_at         | Timestamp | 系统 |                                                   |
| handled_at         | Timestamp | 否   | 处理时间                                          |

---

## 模块三：个人卡系统（个人找项目）

### 功能名称：个人卡发布

**简述：** 有技能的同学填写个人信息、技能标签、时间投入，生成个人卡。

**所需数据：**

| 字段名                  | 类型           | 必填   | 说明                                                  |
|-------------------------|----------------|--------|-------------------------------------------------------|
| card_id                 | String         | 系统   | 唯一标识                                              |
| user_id                 | String         | 系统   | 拥有者                                                |
| display_name            | String         | 是     | 展示昵称                                              |
| target_role             | Enum           | 是     | tech / product / design / business / operation / other|
| weekly_hours            | Int            | 是     | 每周可投入小时（1~40）                                |
| vacation_available      | Boolean        | 否     | 寒暑假可投入                                          |
| self_intro              | String(300)    | 否     | 自我介绍                                              |
| contact_wechat          | String         | 否     | 微信号                                                |
| is_visible              | Boolean        | 默认 true | 对外可见                                            |
| view_count              | Int            | 系统   | 浏览次数                                              |
| invite_count            | Int            | 系统   | 收到邀请数                                            |
| created_at / updated_at | Timestamp      | 系统   |                                                       |

**子表 `PersonalSkill`（1:N）：**

| 字段名     | 类型   | 必填 | 说明           |
|------------|--------|------|----------------|
| card_id    | String | 是   |                |
| skill      | String | 是   | 单个技能标签   |

**子表 `PersonalCompetitionInterest`（多对多兴趣，1:N）：**

| 字段名         | 类型 | 必填 | 说明                                                |
|----------------|------|------|-----------------------------------------------------|
| card_id        | -    | 是   |                                                     |
| competition    | Enum | 是   | dachuan / internet / challenge / jingcai / other    |

> 一个用户仅有 **一张** 个人卡，重复发布即编辑覆盖（`UNIQUE(user_id)`）。

---

### 功能名称：个人卡编辑、隐藏、删除

| 字段名      | 类型    | 必填 | 说明                                          |
|-------------|---------|------|-----------------------------------------------|
| card_id     | String  | 是   |                                               |
| user_id     | String  | 系统 | 仅卡主或管理员可改                            |
| edit_fields | Object  | 条件 |                                               |
| is_visible  | Boolean | 否   | 切换上下架（找到队伍后建议隐藏，减少打扰）    |

---

## 模块四：推荐流与检索

### 功能名称：项目卡推荐流（首页"找项目"Tab）

| 字段名             | 类型 | 必填 | 说明                                          |
|--------------------|------|------|-----------------------------------------------|
| user_id            | -    | 系统 |                                               |
| filter_competition | Enum | 否   | 筛选比赛                                      |
| filter_skill       | Str  | 否   | 技能关键字（在角色技能 / 标签 / 名称中模糊匹配）|
| filter_role        | Str  | 否   | 缺人角色关键字                                |
| filter_min_hours   | Int  | 否   | 周时间下限                                    |
| sort_by            | Enum | 否   | urgent（默认，组队截止近→远） / latest         |
| page / page_size   | Int  | 是   | 默认 page_size=10                             |

**列表项输出：** project_id / project_name / one_liner / competition_target / team_deadline / days_left / current_members / needed_count / first_role / tags / creator_name / creator_dept / urgency_badge。

---

### 功能名称：个人卡推荐流（首页"找队友"Tab）

| 字段名         | 类型 | 必填 | 说明                                  |
|----------------|------|------|---------------------------------------|
| user_id        | -    | 系统 |                                       |
| filter_role    | Enum | 否   | 目标角色                              |
| filter_skill   | Str  | 否   | 技能关键字                            |
| filter_hours   | Int  | 否   | 最低每周小时                          |
| sort_by        | Enum | 否   | latest（默认） / active               |
| page/page_size | Int  | 是   |                                       |

**列表项输出：** card_id / display_name / dept_name / grade（受隐私控制） / target_role / skills / weekly_hours / vacation_available / self_intro_preview / interested_competitions / is_new（3 天内新发布）。

---

### 功能名称：全站搜索（顶部搜索框，可选阶段）

| 字段名     | 类型 | 必填 | 说明                                                                    |
|------------|------|------|-------------------------------------------------------------------------|
| keyword    | Str  | 是   | 关键字                                                                  |
| scope      | Enum | 否   | all / project / personal / forum / competition                          |
| page_size  | Int  | 否   |                                                                         |

**实现：** 阶段一用 MySQL `MATCH ... AGAINST` 全文索引；阶段二切到 Elasticsearch / OpenSearch。

---

## 模块五：私信系统

### 功能名称：发起私信（项目卡 / 个人卡两入口）

| 字段名              | 类型      | 必填 | 说明                                             |
|---------------------|-----------|------|--------------------------------------------------|
| message_id          | String    | 系统 |                                                  |
| conversation_id     | String    | 系统 | 不存在则新建（按"发起人 + 接收人 + 关联实体"去重）|
| sender_id           | String    | 系统 |                                                  |
| receiver_id         | String    | 是   |                                                  |
| project_id          | String    | 否   | 由项目卡发起时填                                 |
| attached_card_id    | String    | 否   | 由个人卡发起时附带的项目 ID                      |
| message_type        | Enum      | 默认 | text / system / wechat / project_link            |
| content             | String(200)| 是  | 文本内容                                         |
| preset_template     | Enum      | 否   | 预设话术                                         |
| created_at          | Timestamp | 系统 |                                                  |

---

### 功能名称：会话列表（左栏）

**输出（单条会话）：**

| 字段名             | 类型      | 说明                                  |
|--------------------|-----------|---------------------------------------|
| conversation_id    | String    |                                       |
| counterpart        | Object    | 昵称 / 头像 / 院系                    |
| last_message       | String(30)| 最后消息预览                          |
| last_time          | Timestamp |                                       |
| unread_count       | Int       |                                       |
| context_label      | String    | "关于：xxx 项目" / "邀请加入项目"     |
| is_expired         | Boolean   | 7 天无新消息                          |

---

### 功能名称：会话详情（右栏聊天）

**消息结构：**

| 字段名     | 类型      | 说明                                                      |
|------------|-----------|-----------------------------------------------------------|
| message_id | String    |                                                           |
| sender_id  | String    |                                                           |
| content    | String    | 文本                                                      |
| msg_type   | Enum      | text / system（如"对话将于 X 天后过期"） / wechat / link  |
| sent_at    | Timestamp |                                                           |
| read_at    | Timestamp | 对方阅读时间                                              |

**快捷按钮：**

| 按钮             | 发送内容                                  |
|------------------|-------------------------------------------|
| 发送我的微信号   | "我的微信号是：[wechat_id]"               |
| 发送我的技能     | "我的技能：[PersonalSkill 拼接]"          |
| 发送项目链接     | "[project_name] 项目详情：/project/{id}"  |
| 自定义回复       | 弹窗输入（限 50 字）                      |

---

### 功能名称：私信生命周期与清理

| 字段名             | 说明                              |
|--------------------|-----------------------------------|
| expires_at         | 最后一条消息后 7 天                |
| remind_24h         | 过期前 24 小时推送提醒            |
| final_delete_at    | 过期后再保留 30 天彻底删除        |

> **隐私设计：** 平台不保存交换微信后的私聊；过期与删除采用定时任务（XXL-JOB / cron）批处理。

---

## 模块六：比赛中心 & 资讯

### 功能名称：比赛列表

| 字段名            | 类型      | 必填 | 说明                                                         |
|-------------------|-----------|------|--------------------------------------------------------------|
| competition_id    | String    | 系统 |                                                              |
| name              | String    | 是   | 全称                                                         |
| short_name        | String    | 是   | 简称                                                         |
| level             | Enum      | 是   | national / provincial / school                               |
| organizer         | String    | 是   | 主办方 / 承办方                                              |
| description       | Text      | 否   | 简介                                                         |
| register_start    | Date      | 是   |                                                              |
| register_end      | Date      | 是   |                                                              |
| official_url      | String    | 否   |                                                              |
| poster_url        | String    | 否   | 封面                                                         |
| status            | Enum      | 系统 | upcoming / active / urgent / closed（按当前日期计算）         |
| project_count     | Int       | 系统 | 关联组队项目数（冗余统计字段）                               |
| created_by        | String    | 系统 | 录入管理员                                                   |
| created_at        | Timestamp | 系统 |                                                              |

---

### 功能名称：比赛详情与"我也要组队"

进入详情页可：
1. 查看比赛完整信息 + 跳转官方链接；
2. 查看 `related_projects`（关联本比赛的全部项目卡，分页）；
3. 点击"我也要组队"→ 跳转 `publish-project.html` 并预填 `competition_id` + `team_deadline`。

---

### 功能名称：比赛资讯（News）

供管理员发布的图文短资讯，比赛详情页与首页右栏聚合展示。

| 字段名             | 类型      | 必填 | 说明                              |
|--------------------|-----------|------|-----------------------------------|
| news_id            | String    | 系统 |                                   |
| title              | String    | 是   |                                   |
| summary            | String(120)| 否  | 列表展示                          |
| content_md         | Text      | 是   | Markdown 正文                     |
| source             | String    | 否   | 来源，如"教务处"                  |
| related_competition| String    | 否   | 关联比赛                          |
| cover_url          | String    | 否   |                                   |
| view_count         | Int       | 系统 |                                   |
| status             | Enum      | 系统 | draft / published / archived      |
| author_id          | String    | 系统 | 管理员                            |
| created_at         | Timestamp | 系统 |                                   |

---

## 模块七：创坊论坛

### 功能名称：发帖

| 字段名         | 类型           | 必填 | 说明                                                      |
|----------------|----------------|------|-----------------------------------------------------------|
| post_id        | String         | 系统 |                                                           |
| author_id      | String         | 系统 |                                                           |
| title          | String(60)     | 是   |                                                           |
| content_md     | Text           | 是   | Markdown 正文                                             |
| topic          | Enum           | 是   | strategy（攻略） / team（组队经验） / tech / idea / news / qa / resource |
| topic_tags     | Array[String]  | 否   | 自由话题标签 `#互联网+大赛攻略` 等                        |
| status         | Enum           | 系统 | normal / pinned / hidden（管理员可置顶或下架） |
| is_essence     | Boolean        | 系统 | 精华帖（管理员标记）                                      |
| view_count     | Int            | 系统 |                                                           |
| reply_count    | Int            | 系统 |                                                           |
| like_count     | Int            | 系统 |                                                           |
| last_reply_at  | Timestamp      | 系统 | 用于"最新"排序                                            |
| created_at     | Timestamp      | 系统 |                                                           |

---

### 功能名称：帖子列表（Tab：最新 / 热门 / 精华，话题筛选）

**输出列表项：** post_id / title / topic / badges（pinned/essence/hot/new） / author（昵称、院系、头像） / excerpt（前 2 行） / view_count / reply_count / like_count / created_at。

**排序口径：**
- 最新：`pinned DESC, last_reply_at DESC`
- 热门：综合分 = `0.5 * view + 1.0 * like + 2.0 * reply`，近 7 天衰减
- 精华：`is_essence = true`

---

### 功能名称：帖子详情 & 评论

| 字段名     | 类型      | 必填 | 说明                              |
|------------|-----------|------|-----------------------------------|
| comment_id | String    | 系统 |                                   |
| post_id    | String    | 是   |                                   |
| parent_id  | String    | 否   | 楼中楼父级（仅一层）              |
| author_id  | String    | 系统 |                                   |
| content    | String(500)| 是  |                                   |
| like_count | Int       | 系统 |                                   |
| status     | Enum      | 系统 | normal / hidden                   |
| created_at | Timestamp | 系统 |                                   |

---

### 功能名称：互动（点赞 / 收藏）

`PostLike(post_id, user_id, created_at)` 唯一索引；`PostFavorite(post_id, user_id, created_at)`；`CommentLike(comment_id, user_id)`。

---

### 功能名称：热门话题与话题聚合

`Topic(topic_tag, post_count, last_used_at)`，每日凌晨重算 Top 10 写入 Redis 用于首页。

---

## 模块八：我的中心（普通用户）

### 功能名称：我的项目卡管理

按状态分组（recruiting / completed / closed）展示我发布的项目卡，含申请记录列表、统计数据、编辑入口。

**输出扩展：**

| 字段名                    | 类型           | 说明                              |
|---------------------------|----------------|-----------------------------------|
| project_card              | Object         | 项目卡完整信息                    |
| applications              | Array[Object]  | 申请记录                          |
| applications[].applicant  | Object         | 申请人摘要                        |
| applications[].status     | Enum           | pending / accepted / rejected     |

---

### 功能名称：我的个人卡管理

view / edit / toggle_visible / delete。

---

### 功能名称：未读消息红点

| 字段名              | 类型 | 说明                |
|---------------------|------|---------------------|
| unread_messages     | Int  | 私信未读            |
| unread_replies      | Int  | 论坛被回复未读      |
| unread_applications | Int  | 项目申请未读        |

---

## 模块九：管理后台

> **定位**：与 C 端共用同一套数据库与后端，但**前端独立子站点 / 独立路由前缀 `/admin`**，UI 风格沿用人大红主题，但布局采用"左侧菜单 + 顶部面包屑 + 主内容"的标准管理后台模板（详见 [`docs/html-modifications.md` §10 管理后台](docs/html-modifications.md)）。

### 9.1 管理后台总览

```
管理后台 /admin
├── 仪表盘            /admin/dashboard            ← 关键指标 + 待办（待审举报、最新注册等）
├── 账户管理
│   ├── 用户列表      /admin/users                ← 搜索 / 筛选 / 详情 / 启停 / 改角色 / 重置密码
│   ├── 新增账号      /admin/users/new            ← 手动添加普通 / 管理员账号（一次性激活链接）
│   └── 管理员管理    /admin/admins               ← 管理员单独视图（仅 super_admin 可见）
├── 内容治理
│   ├── 项目卡管理    /admin/projects             ← 全站项目卡：搜索 / 强制下架 / 隐藏
│   ├── 个人卡管理    /admin/personal-cards       ← 全站个人卡：搜索 / 隐藏 / 删除
│   ├── 论坛管理      /admin/forum/posts          ← 帖子：置顶 / 加精 / 隐藏 / 删除
│   ├── 评论管理      /admin/forum/comments       ← 评论隐藏 / 删除
│   └── 私信巡查      /admin/messages             ← 仅"被举报"私信对话可见，避免越权
├── 比赛中心
│   ├── 比赛管理      /admin/competitions         ← CRUD
│   └── 资讯管理      /admin/news                 ← CRUD（Markdown 编辑器）
├── 审核中心
│   ├── 举报队列      /admin/reports              ← 用户举报池，分配 / 处理
│   ├── 敏感词管理    /admin/keywords             ← 关键词词典 CRUD
│   └── 审核日志      /admin/audit-logs           ← 全部管理员操作可追溯
└── 系统
    ├── 角色与权限    /admin/roles                ← RBAC 配置（仅 super_admin）
    ├── 系统配置      /admin/settings             ← 全局开关、邮件、限流配置
    └── 个人设置      /admin/profile              ← 改密 / 退出
```

---

### 9.2 角色与权限模型（RBAC）

为兼顾**实现成本**与**可扩展性**，采用 **"角色 + 权限码白名单"** 两层模型：

**角色（`User.role`）：**

| role           | 说明                                              |
|----------------|---------------------------------------------------|
| `student`      | 学生，自助注册                                    |
| `teacher`      | 教师，可由 admin 邀请；C 端权限同 student          |
| `admin`        | 一般管理员，权限按 `admin_perms` 白名单控制       |
| `super_admin`  | 超级管理员，**默认拥有全部权限码**，可创建 admin / 编辑权限模板 / 系统配置 |

**权限码（`User.admin_perms` JSON 数组）：**
按"模块 : 动作"命名，前端菜单与按钮按权限码隐藏，后端接口 `@RequiresPerm("xxx")` 注解校验。

| 权限码                       | 说明                                |
|------------------------------|-------------------------------------|
| `user:read`                  | 查看用户列表 / 详情                 |
| `user:write`                 | 编辑用户资料 / 启停 / 改角色        |
| `user:create`                | 创建普通账号                        |
| `user:create_admin`          | 创建管理员账号（隐含 super 权限）   |
| `user:reset_password`        | 重置用户密码                        |
| `project:moderate`           | 项目卡下架 / 强制改状态             |
| `personal:moderate`          | 个人卡下架 / 删除                   |
| `forum:moderate`             | 论坛帖子置顶 / 加精 / 隐藏 / 删除   |
| `forum:comment_moderate`     | 评论隐藏 / 删除                     |
| `message:audit`              | 私信巡查（仅被举报对话）            |
| `competition:write`          | 比赛 CRUD                           |
| `news:write`                 | 资讯 CRUD                           |
| `report:handle`              | 举报队列处理                        |
| `keyword:write`              | 敏感词管理                          |
| `audit:read`                 | 审核日志查看                        |
| `dashboard:read`             | 仪表盘查看                          |
| `system:write`               | 系统配置（邮件 / 限流 / 开关）      |
| `role:write`                 | 角色权限模板编辑（super 专属）      |

**权限模板（预置 `RoleTemplate`，便于一键授权）：**

| 模板名              | 权限码集合                                                                |
|---------------------|---------------------------------------------------------------------------|
| `super_admin`       | 全部                                                                      |
| `content_moderator` | `forum:moderate`、`forum:comment_moderate`、`project:moderate`、`personal:moderate`、`report:handle`、`keyword:write`、`message:audit`、`audit:read`、`dashboard:read` |
| `competition_admin` | `competition:write`、`news:write`、`dashboard:read`                         |
| `user_admin`        | `user:read`、`user:write`、`user:create`、`user:reset_password`、`audit:read` |

> super_admin 可在 `/admin/roles` 自定义模板；保存后修改 `User.admin_perms` 即可批量生效。

---

### 9.3 功能名称：仪表盘 `/admin/dashboard`

权限：`dashboard:read`

**指标卡片：**

| 指标                     | 说明                                            |
|--------------------------|-------------------------------------------------|
| dau / wau / mau          | 活跃用户                                        |
| new_users_today          | 今日新增用户                                    |
| new_project_cards        | 新增项目卡                                      |
| new_personal_cards       | 新增个人卡                                      |
| matching_rate            | 申请被同意率 / 估算换微信率（含"微信"关键字）   |
| message_count            | 私信发起数                                      |
| forum_dau                | 论坛日活                                        |
| competition_distribution | 各比赛关联项目分布（饼图）                      |
| top_skills               | 紧缺技能 Top 20（柱状图）                       |
| pending_reports          | **待处理举报**（红点提醒）                      |
| pending_keywords_hits    | **关键词命中待审**                              |

---

### 9.4 功能名称：账户管理

#### 9.4.1 用户列表 `/admin/users`

权限：`user:read`

**筛选 / 搜索条件：**

| 字段          | 控件      | 说明                                       |
|---------------|-----------|--------------------------------------------|
| 关键字        | 搜索框    | email / name / display_name / ruc_id 模糊  |
| 角色          | 下拉      | student / teacher / admin / super_admin    |
| 状态          | 下拉      | active / disabled / banned / pending_first_login |
| 来源          | 下拉      | self_register / admin_created / cas        |
| 院系          | 下拉      | 院系字典                                   |
| 注册时间      | 日期范围  |                                            |

**列表列：** user_id（短）/ avatar / display_name / email / role / dept_name / grade / status / source / last_login_at / created_at / 操作。

**单行操作：** 详情 / 编辑 / 启停（disabled）/ 重置密码 / 改角色 / 封禁 / 删除（软删，仅 super_admin）。

**批量操作：** 批量启停 / 批量加 / 减 admin_perms（仅 `user:write`）。

#### 9.4.2 用户详情抽屉

切换右侧抽屉展示：基本信息 + 关联资源（已发布项目卡数、个人卡 1、论坛帖子数、私信会话数、举报记录、被举报记录、登录历史、操作审计）。

#### 9.4.3 新增账号 `/admin/users/new`

权限：`user:create`（仅 super_admin 可同时持有 `user:create_admin` 创建管理员）

**表单字段：**

| 字段             | 类型 | 必填 | 说明                                                                |
|------------------|------|------|---------------------------------------------------------------------|
| email            | Str  | 是   | 普通用户必须 `@ruc.edu.cn`；admin / 外部账户可任意域名               |
| name             | Str  | 是   | 真实姓名                                                            |
| display_name     | Str  | 否   | 默认 = name                                                         |
| role             | Enum | 是   | student / teacher / admin / super_admin（后两者需 `user:create_admin`）|
| role_template    | Enum | 条件 | 当 role=admin 时选择权限模板（content_moderator 等）                |
| admin_perms      | Multi| 条件 | 在模板基础上微调勾选权限码                                          |
| dept_code        | Str  | 否   | 院系                                                                |
| grade            | Str  | 否   | 年级                                                                |
| init_method      | Enum | 是   | `email_link`（一次性激活链接）/ `set_password`（管理员直接设置初始密码并告知本人）|
| init_password    | Str  | 条件 | init_method=set_password 时必填                                      |
| send_email       | Bool | 否   | 是否同时发送通知邮件                                                |
| remark           | Str  | 否   | 备注（仅 admin 内部可见）                                            |

**提交后：** 写 `User`（status=`pending_first_login` 或 `active`），写 `AuditLog(action=create_user)`，可选发邮件。

#### 9.4.4 重置密码

权限：`user:reset_password`

操作：选择"邮件重置链接"或"直接设置新密码"，写 `AuditLog(action=reset_password)`；目标用户所有 refresh token 立即吊销。

#### 9.4.5 启停 / 封禁

| 动作       | 修改                                                | 影响                                         |
|------------|-----------------------------------------------------|----------------------------------------------|
| 停用       | `status=disabled`                                   | 不能登录；已存在数据照常展示                 |
| 封禁       | `status=banned` + `ban_reason` + `ban_expire_at`     | 同上 + 项目卡 / 个人卡自动隐藏 + 论坛帖子 hidden |
| 解封 / 启用 | `status=active`                                      | 恢复访问                                     |

所有动作必填 `reason`，写 `AuditLog`。

---

### 9.5 功能名称：内容治理

#### 9.5.1 项目卡管理 `/admin/projects`

权限：`project:moderate`

筛选：状态 / 比赛 / 创建者 / 关键字。
操作：查看详情 / 强制改状态（recruiting → closed）/ 隐藏（不进推荐流，详情页 410）/ 恢复 / 删除（软删）。所有操作必填 `reason` → `AuditLog`。

#### 9.5.2 个人卡管理 `/admin/personal-cards`

权限：`personal:moderate`

筛选：可见性 / 目标角色 / 关键字。
操作：隐藏（强制 `is_visible=false`）/ 恢复 / 删除（软删）/ 重置违规字段（清空 self_intro 等）。

#### 9.5.3 论坛管理 `/admin/forum/posts`、`/admin/forum/comments`

权限：`forum:moderate`、`forum:comment_moderate`

帖子操作：置顶 / 取消置顶 / 加精 / 取消加精 / 隐藏 / 删除 / 移动话题。
评论操作：隐藏 / 删除。
操作均带 `reason`，被处理用户收到站内通知。

#### 9.5.4 私信巡查 `/admin/messages`

权限：`message:audit`

**重要边界**：管理员**默认看不到任何私信内容**；只有当某条 `Message` 或 `Conversation` 被举报后，对应会话才进入此页面，且仅展示"举报上下文 ± 10 条"消息，不展示完整历史。所有进入此页的查询动作都写 `AuditLog`，便于事后追溯。

---

### 9.6 功能名称：比赛 & 资讯管理

#### 9.6.1 比赛管理 `/admin/competitions`

权限：`competition:write`

CRUD 字段对应 [模块六](#模块六比赛中心--资讯) `Competition` 表。新增编辑表单；列表支持按级别 / 状态 / 创建时间筛选，支持"重新计算关联项目数"按钮。

#### 9.6.2 资讯管理 `/admin/news`

权限：`news:write`

字段对应 `News` 表。Markdown 编辑器 + 封面上传（OSS）+ 关联比赛下拉 + 状态切换（draft / published / archived）。

---

### 9.7 功能名称：审核中心

#### 9.7.1 举报队列 `/admin/reports`

权限：`report:handle`

**列表列：** report_id / content_type / 内容预览 / 举报理由 / 举报人 / 状态 / 提交时间 / 处理人 / 处理时间。

**单行操作：**
- "查看原文"（跳到对应内容详情，带管理员标志）
- "处理" → 选择动作（hide / delete / warn / ban），填 reason
- "驳回"（reject）→ 标记无效

**批量：** 批量驳回。

写 `Report.status=handled` + `AuditLog`。

#### 9.7.2 敏感词管理 `/admin/keywords`

权限：`keyword:write`

**`KeywordRule` 实体：**

| 字段名         | 类型 | 必填 | 说明                                                |
|----------------|------|------|-----------------------------------------------------|
| rule_id        | Str  | 系统 |                                                     |
| keyword        | Str  | 是   | 关键词 / 正则                                       |
| match_type     | Enum | 是   | `contains` / `regex`                                |
| action         | Enum | 是   | `warn`（仅记录） / `block`（直接阻断写入）          |
| scope          | Multi| 是   | project_card / personal_card / forum_post / forum_comment / private_message |
| enabled        | Bool | 是   |                                                     |
| created_by     | Str  | 系统 |                                                     |
| created_at     | TS   | 系统 |                                                     |
| hit_count      | Int  | 系统 | 命中累计                                            |

后端写入流程：用户提交内容 → KeywordRule 匹配 → `block` 直接 422 拒绝；`warn` 入 `ModerationLog` 等待人工。

#### 9.7.3 审核日志 `/admin/audit-logs`

权限：`audit:read`

`AuditLog` 全表查询：actor_id / actor_role / action / target_type / target_id / before(JSON) / after(JSON) / reason / ip / created_at。
支持按动作、操作人、时间范围筛选。**只读**，不可修改不可删除（合规要求）。

---

### 9.8 功能名称：系统设置（仅 super_admin）

#### 9.8.1 角色与权限 `/admin/roles`

权限：`role:write`

`RoleTemplate` 实体：

| 字段名     | 类型      | 必填 | 说明                          |
|------------|-----------|------|-------------------------------|
| template_id| Str       | 系统 |                               |
| name       | Str       | 是   | 模板名                        |
| description| Str       | 否   |                               |
| perms      | JSON      | 是   | 权限码数组                    |
| built_in   | Bool      | 系统 | 内置不可删除                  |
| created_at | Timestamp | 系统 |                               |

#### 9.8.2 系统配置 `/admin/settings`

`SystemConfig` 是 KV 表（`(config_key, config_value, scope, description, updated_by, updated_at)`），常用键：

| key                         | 默认值 | 说明                                  |
|-----------------------------|--------|---------------------------------------|
| `auth.allowed_email_domain` | `ruc.edu.cn` | 普通用户邮箱白名单（多个用逗号）  |
| `auth.allow_self_register`  | `true` | 是否允许邮箱自助注册                  |
| `message.expire_days`       | `7`    | 私信过期天数                          |
| `message.delete_days`       | `30`   | 过期后多少天物理删除                  |
| `forum.post_min_interval_s` | `30`   | 发帖冷却（防刷）                      |
| `feature.ai_assistant`      | `false`| AI 助手开关                           |

修改 `SystemConfig` 必写 `AuditLog`；变更后通过 Redis Pub/Sub 让应用层热更新缓存。

---

### 9.9 通用管理员能力

| 能力           | 实现要点                                                                      |
|----------------|-------------------------------------------------------------------------------|
| 全局搜索       | 顶栏搜索框跨用户 / 项目卡 / 帖子，结果中标识资源类型                          |
| 操作"二次确认" | 删除 / 封禁 / 改角色等高危动作弹窗 + reason 必填                              |
| IP / 设备追踪  | 管理员每次登录 / 关键操作记录 IP，异常登录推送告警                            |
| 双因素认证     | 管理员强制开启 TOTP（二期）                                                   |
| 操作审计       | 所有写操作经 `AuditInterceptor` 自动落 `AuditLog`，含 before/after 快照       |
| 数据导出       | 列表页支持导出 CSV（异步生成 → 邮件 / 平台下载）                              |

---

### 9.10 功能名称（可选）：AI 助手

> 二期能力，按 docx 设计延后落地。

- **比赛攻略问答**：对接 LLM API，结合 `Competition` + `News` + 论坛精华帖做 RAG；
- **项目卡 / 个人卡润色**：根据已填字段生成 `one_liner` / `self_intro` 草稿；
- **简历建议**：基于个人卡结构生成 STAR 化建议。
- 后台 `/admin/ai` 控制：开关、模型、Prompt 模板、月度调用配额。

---

## 附录：核心数据关系图（高层）

```
User（用户，含 admin/super_admin）
  ├─ UserSettings 1:1
  ├─ ProjectCard 1:N ─┬─ ProjectRole 1:N
  │                   ├─ ProjectTag  1:N
  │                   └─ Application 1:N ─ → 触发 Conversation
  ├─ PersonalCard 1:1 ─┬─ PersonalSkill 1:N
  │                    └─ PersonalCompetitionInterest 1:N
  ├─ Conversation M:N（参与方两人）
  │     └─ Message 1:N
  ├─ ForumPost 1:N ─┬─ ForumComment 1:N
  │                 ├─ PostLike     M:N
  │                 └─ PostFavorite M:N
  └─ Report 1:N（提交 / 处理）

管理后台相关：
  RoleTemplate（权限模板）─ 引用 → User.admin_perms[]
  KeywordRule（敏感词）  ─ 拦截 → 任意写操作
  ModerationLog          ← 自动审核命中
  AuditLog               ← 全部管理员写操作（不可删）
  SystemConfig           （KV 全局配置）
  Notification 1:N       （含 admin 推送的系统公告）

Competition（比赛）1:N → ProjectCard.competition_id
                     1:N → News
```

完整 ER 图见 [`docs/er-diagram.puml`](docs/er-diagram.puml)。

---

## 后续工作

- 系统架构与技术选型 → [`docs/design.md`](docs/design.md)
- 前端 HTML 改造接入指引 → [`docs/html-modifications.md`](docs/html-modifications.md)
- 数据库 ER 图 → [`docs/er-diagram.puml`](docs/er-diagram.puml)
