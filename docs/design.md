# 知行创坊 KAL 系统架构与技术选型设计

> 版本 v1.0｜2026-05｜规模目标：注册 ~ 1000 人 / DAU 200~400 / 同时在线 50~100。
> 本文档**只描述设计与流程**，不包含具体代码实现。配套交付：
>
> - 数据模型 → [`er-diagram.puml`](./er-diagram.puml)
> - 功能与字段 → [`../ReadeMe.md`](../ReadeMe.md)
> - 前端改造接入说明 → [`html-modifications.md`](./html-modifications.md)

---

## 一、设计目标与原则

### 1.1 业务体量预估

| 指标                 | 估算值       | 说明                                    |
|----------------------|--------------|-----------------------------------------|
| 注册用户             | 1000         | 在校全规模覆盖（学生 + 部分教师）       |
| 日活 DAU             | 200~400      | 比赛季峰值，可能短时翻倍至 800          |
| 同时在线             | 50~100       | 私信场景峰值                            |
| QPS（普通接口峰值）  | < 100        | 推荐流、详情、列表读                    |
| QPS（私信接口峰值）  | < 50         | 长连接 / 轮询                           |
| 私信总写入           | 5k~10k 条/日 | 含系统消息                              |
| 论坛帖子写入         | < 100 条/日  |                                         |
| 项目卡 / 个人卡总量  | < 5000       | 含历史归档                              |

**核心结论：** 这是一个**典型的"轻量校园应用"**，单机就能扛峰值；架构上**优先简洁、可观测、可横向扩展**，避免过度设计。

### 1.2 设计原则

1. **务实优先**：1000 人规模 ≠ 微服务，单体应用 + 关键缓存 + 一台云主机即可；为后续可扩展，**分层和接口契约**做到位。
2. **前后端分离**：HTML 页面渐进迁移到 Vue3 / React + 路由，配 RESTful API。
3. **领域分包**：后端按功能模块分包（user / project / personal / message / forum / competition / admin），便于将来抽离微服务。
4. **隐私先行**：校园场景对真实姓名 / 院系 / 微信号敏感，所有 API 默认走授权 + 字段级隐私规则。
5. **可观测**：日志 / 监控 / 链路 / 慢 SQL 报警从 Day 1 接入；运营看板使用 SQL 即席查询。
6. **零运维优先**：用云厂商的 RDS / Redis / OSS / ELB，不要自己搭。

---

## 二、整体架构

### 2.1 部署架构（C4 部署视图）

```
+----------------------+        +-------------------------+
|        浏览器         |  HTTPS  |         Nginx           |
|  Vue3/React SPA       |<--+--->|  反向代理 + 静态资源 +  |
|  (PC + 移动端)        |   |    |  WAF + Gzip + HTTPS     |
+----------------------+   |    +-----------+-------------+
                           |                |
                           | (WebSocket)    | /api/*
                           |                v
                +----------+--------+   +-----------------+
                |  消息长连接服务   |   |  应用层（单体）  |
                | (Spring Boot WS / |   | Spring Boot 3 / |
                |  Node.js Socket.IO|   | NestJS（任选）  |
                |  二选一)          |   | REST + GraphQL  |
                +----------+--------+   +--------+--------+
                           |                     |
                           +----------+----------+
                                      |
        +-----------+------------+----+----+----------+-------------+
        |           |            |         |          |             |
        v           v            v         v          v             v
  +---------+  +--------+  +-----------+  +-----+ +--------+  +------------+
  |  MySQL  |  | Redis  |  | OSS/COS   |  | MQ* | | ES*    |  | XXL-JOB    |
  | 8.x RDS |  | 6.x    |  | 头像/封面 |  |     | | 全文* |  | 定时任务   |
  +---------+  +--------+  +-----------+  +-----+ +--------+  +------------+

       *：MQ / ES 二期才接入；一期用 MySQL FULLTEXT + 内部线程池即可。
```

**关键容量预估（一期）：**

| 资源        | 规格                          | 说明                            |
|-------------|-------------------------------|---------------------------------|
| 应用服务器  | 4 vCPU / 8 GB / 100 GB SSD     | 单台即可，跑应用 + Nginx        |
| MySQL RDS   | 2 vCPU / 4 GB / 100 GB         | 主从一主一从                    |
| Redis       | 1 GB / 单实例                  | 缓存 + 限流 + 排行榜            |
| OSS         | 按量付费                       | 头像、封面、附件                |
| 域名 + HTTPS| 1 个二级域名 + 免费证书        | 例如 `kal.ruc.edu.cn`           |

> 这套规格**月成本 < 200 元**，符合 docx 中"服务器与域名约 100 元"的预算（按部分时段闲置抵扣）。

### 2.2 架构层次（应用内）

```
┌─────────────────────────────────────────────────────────┐
│  Web 层  : Controller / WebSocketHandler / Filter       │  REST + WS
├─────────────────────────────────────────────────────────┤
│  应用层  : ApplicationService（编排事务，DTO 装配）     │
├─────────────────────────────────────────────────────────┤
│  领域层  : DomainService / Aggregate / Domain Event     │
├─────────────────────────────────────────────────────────┤
│  基础设施: Repository / Cache / MQ / 外部网关 / 文件    │
└─────────────────────────────────────────────────────────┘
```

每个**业务模块独立子包**（user / project / personal / message / forum / competition / admin / common），子包之间**只允许通过 Application 层的 Facade 接口调用**，避免日后拆服务时跨模块依赖混乱。

---

## 三、技术选型

| 维度          | 一期选型                                         | 备选 / 升级路径                          | 选型理由                                                   |
|---------------|--------------------------------------------------|------------------------------------------|------------------------------------------------------------|
| 前端框架      | **Vue 3 + Vite + Pinia + Vue-Router + Element Plus** | React 18 + AntD                          | 中文生态好、上手快、与现有 HTML 风格匹配、组件库现成        |
| HTTP 客户端   | Axios + 拦截器                                    | Fetch                                    | 统一鉴权、错误码、loading 拦截                              |
| 后端语言      | **Java 17 + Spring Boot 3.2**                     | Node.js + NestJS                         | 校园项目人才储备多、生态稳、IDEA 好用                       |
| 数据访问      | MyBatis-Plus + Druid 连接池                       | JPA / JOOQ                               | 轻量 + SQL 透明、利于上线后调优                             |
| 数据库        | **MySQL 8.x（云 RDS 一主一从）**                  | PostgreSQL                               | 全文索引（ngram）开箱即用，云厂商一键备份                   |
| 缓存          | **Redis 6.x**                                     | -                                        | 推荐流缓存、JWT Refresh、限流、排行榜                       |
| 鉴权          | **JWT（access 30min + refresh 7d）+ httpOnly Cookie** | OAuth2 + 人大 CAS                    | 二期接 CAS 单点登录                                         |
| 实时私信      | **WebSocket（Spring Boot starter-websocket）**     | SSE / 长轮询                             | 双向、低延迟，1000 人规模单实例足够                         |
| 任务调度      | XXL-JOB 单机版                                    | Spring @Scheduled                        | 私信过期、统计、热度计算                                    |
| 全文检索      | 一期 MySQL FULLTEXT + ngram                       | 二期 OpenSearch / Elasticsearch          | 一期数据量 < 5w 行，FULLTEXT 完全够                         |
| 富文本        | Markdown（论坛 / 资讯）+ marked.js + DOMPurify    | TipTap / 富文本 HTML                     | 安全、易迁移、便于反作弊                                    |
| 对象存储      | **阿里云 OSS / 腾讯云 COS**                        | 自建 MinIO                               | 头像 / 封面 / 资讯图，CDN 加速                              |
| 监控          | Prometheus + Grafana + 阿里云 ARMS（按量）         | SkyWalking                               | 应用指标 + JVM + 慢 SQL                                     |
| 日志          | Spring Boot Logging + Loki + Grafana              | ELK                                      | 结构化日志，按 traceId 追溯                                 |
| CI / CD       | GitHub Actions / Gitee Go + 服务器 SSH 发布        | Jenkins                                  | 学生项目轻量化                                              |
| 测试          | JUnit5 + Mockito + Testcontainers                 | -                                        | 业务核心 + Repository 集成测试                              |

---

## 四、目录结构（推荐）

```
kal/
├── frontend/                        Vue3 SPA（由现有 HTML 改造）
│   ├── public/
│   ├── src/
│   │   ├── api/                     Axios 封装 & 接口定义
│   │   ├── assets/icons/            图标（保留现有 PNG）
│   │   ├── components/
│   │   │   ├── layout/              Header / Footer / NavMenu
│   │   │   ├── card/                ProjectCard, PersonalCard, ForumPostCard
│   │   │   └── form/
│   │   ├── views/
│   │   │   ├── Login.vue            ← login.html
│   │   │   ├── Home.vue             ← index.html
│   │   │   ├── PublishProject.vue   ← publish-project.html
│   │   │   ├── PublishProfile.vue   ← publish-profile.html
│   │   │   ├── ProjectDetail.vue    ← project-detail.html
│   │   │   ├── Competitions.vue     ← competitions.html
│   │   │   ├── Forum.vue            ← forum.html
│   │   │   ├── PostDetail.vue       新增（论坛帖子详情）
│   │   │   ├── Messages.vue         ← messages.html
│   │   │   ├── MyCenter.vue         ← my-center.html
│   │   │   └── admin/               后台
│   │   ├── stores/                  Pinia
│   │   ├── router/
│   │   ├── styles/
│   │   │   ├── tokens.css           颜色 / 圆角 / 阴影 token（提取自 :root）
│   │   │   └── reset.css
│   │   └── utils/
│   ├── vite.config.ts
│   └── package.json
│
├── backend/                         Spring Boot 单体应用
│   ├── src/main/java/cn/ruc/kal
│   │   ├── KalApplication.java
│   │   ├── common/                  通用：异常 / 拦截器 / 工具
│   │   │   ├── auth/                JWT / @LoginUser / RBAC
│   │   │   ├── exception/
│   │   │   ├── response/            统一响应封装
│   │   │   ├── audit/               操作审计
│   │   │   └── util/
│   │   ├── infrastructure/
│   │   │   ├── persistence/         MyBatis Plus 配置 / 通用 Repo
│   │   │   ├── cache/               RedisTemplate 封装
│   │   │   ├── storage/             OSS 客户端
│   │   │   └── ws/                  WebSocketHandler
│   │   ├── modules/
│   │   │   ├── user/                注册 / 登录 / 设置
│   │   │   ├── project/             项目卡 / 申请
│   │   │   ├── personal/            个人卡
│   │   │   ├── recommendation/      推荐流（聚合查询）
│   │   │   ├── message/             私信 + WebSocket
│   │   │   ├── competition/         比赛 + 资讯
│   │   │   ├── forum/               论坛 + 评论 + 点赞
│   │   │   └── admin/               审核 / 看板
│   │   └── job/                     XXL-JOB Handler
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-prod.yml
│   │   ├── mapper/                  MyBatis XML
│   │   └── db/migration/            Flyway 脚本
│   └── pom.xml
│
└── docs/
    ├── design.md                    本文件
    ├── er-diagram.puml
    ├── html-modifications.md
    └── api/                         OpenAPI yaml
```

---

## 五、API 设计规范

### 5.1 统一原则

- **风格**：RESTful + JSON。资源使用复数名词，如 `/api/v1/projects/{id}`。
- **版本**：URL 前缀 `v1`，重大变更升 `v2`。
- **统一响应封装**：

  ```jsonc
  // 成功
  { "code": 0, "message": "ok", "data": { ... }, "trace_id": "..." }
  // 失败
  { "code": 40001, "message": "项目不存在", "data": null, "trace_id": "..." }
  ```

- **错误码段**：4xxxx 业务异常（按模块分段：user 41xxx / project 42xxx / message 43xxx ...）；5xxxx 服务器异常。
- **分页**：统一 `page`（从 1 开始）、`page_size`（默认 10、上限 50）；返回 `{ list, total, page, page_size }`。
- **鉴权**：除登录、注册、健康检查外，全部走 `Authorization: Bearer <jwt>`。
- **时间戳**：返回 ISO-8601（`2026-05-06T10:30:00+08:00`），数据库统一 `DATETIME` UTC。
- **隐私字段**：响应中默认隐藏真实姓名 / 微信号；只有匹配权限规则（自己 / 卡主授权 / 管理员）时才返回。

### 5.2 关键接口分组（节选）

| 模块         | Method | 路径                                             | 说明                                  |
|--------------|--------|--------------------------------------------------|---------------------------------------|
| 用户         | POST   | `/api/v1/auth/login`                             | 邮箱登录 / 自动注册                   |
| 用户         | POST   | `/api/v1/auth/refresh`                           | 刷新 token                            |
| 用户         | GET    | `/api/v1/users/me`                               | 当前用户摘要                          |
| 用户         | PATCH  | `/api/v1/users/me/settings`                      | 隐私 + 通知设置                       |
| 项目卡       | POST   | `/api/v1/projects`                               | 发布                                  |
| 项目卡       | GET    | `/api/v1/projects?...`                           | 推荐流（含筛选 / 排序）               |
| 项目卡       | GET    | `/api/v1/projects/{id}`                          | 详情                                  |
| 项目卡       | PATCH  | `/api/v1/projects/{id}`                          | 编辑 / 更新状态                       |
| 项目卡       | POST   | `/api/v1/projects/{id}/applications`             | 申请加入                              |
| 项目卡       | PATCH  | `/api/v1/applications/{id}`                      | 同意 / 拒绝                           |
| 个人卡       | PUT    | `/api/v1/personal-cards`                         | 创建 / 更新（一人一卡）               |
| 个人卡       | GET    | `/api/v1/personal-cards?...`                     | 推荐流                                |
| 私信         | GET    | `/api/v1/conversations`                          | 会话列表                              |
| 私信         | GET    | `/api/v1/conversations/{id}/messages`            | 拉取消息（游标分页）                  |
| 私信         | POST   | `/api/v1/conversations/{id}/messages`            | 发送                                  |
| 私信         | WS     | `/ws/chat?token=...`                             | 长连接                                |
| 比赛         | GET    | `/api/v1/competitions`                           | 列表                                  |
| 比赛         | GET    | `/api/v1/competitions/{id}/related-projects`     | 关联项目                              |
| 论坛         | POST   | `/api/v1/forum/posts`                            | 发帖                                  |
| 论坛         | GET    | `/api/v1/forum/posts?tab=latest|hot|essence`     | 列表                                  |
| 论坛         | GET    | `/api/v1/forum/posts/{id}`                       | 详情 + 评论                           |
| 论坛         | POST   | `/api/v1/forum/posts/{id}/comments`              | 评论                                  |
| 论坛         | POST   | `/api/v1/forum/posts/{id}/likes`                 | 点赞                                  |
| 后台-认证    | POST   | `/api/v1/admin/auth/login`                       | 管理员登录（独立速率限制 + 失败计数）  |
| 后台-账户    | GET    | `/api/v1/admin/users?...`                        | 用户列表（搜索 / 筛选）               |
| 后台-账户    | POST   | `/api/v1/admin/users`                            | 手动新增账号（普通 / 管理员）         |
| 后台-账户    | PATCH  | `/api/v1/admin/users/{id}`                       | 编辑用户资料 / 角色 / 权限            |
| 后台-账户    | POST   | `/api/v1/admin/users/{id}/disable`               | 停用                                  |
| 后台-账户    | POST   | `/api/v1/admin/users/{id}/ban`                   | 封禁（携带 reason / expire_at）       |
| 后台-账户    | POST   | `/api/v1/admin/users/{id}/reset-password`        | 重置密码                              |
| 后台-内容    | POST   | `/api/v1/admin/projects/{id}/moderate`           | 项目卡下架 / 隐藏 / 删除（带 reason） |
| 后台-内容    | POST   | `/api/v1/admin/personal-cards/{id}/moderate`     | 个人卡治理                            |
| 后台-内容    | POST   | `/api/v1/admin/forum/posts/{id}/moderate`        | 帖子置顶 / 加精 / 隐藏 / 删除         |
| 后台-内容    | POST   | `/api/v1/admin/forum/comments/{id}/moderate`     | 评论治理                              |
| 后台-比赛    | POST   | `/api/v1/admin/competitions`                     | 比赛 CRUD                             |
| 后台-资讯    | POST   | `/api/v1/admin/news`                             | 资讯 CRUD                             |
| 后台-审核    | GET    | `/api/v1/admin/reports?status=pending`           | 举报队列                              |
| 后台-审核    | POST   | `/api/v1/admin/reports/{id}/handle`              | 处理 / 驳回                           |
| 后台-审核    | GET / POST | `/api/v1/admin/keywords`                     | 敏感词管理                            |
| 后台-审核    | GET    | `/api/v1/admin/audit-logs?...`                   | 审计日志查询                          |
| 后台-看板    | GET    | `/api/v1/admin/dashboard`                        | 看板数据                              |
| 后台-系统    | GET / PUT | `/api/v1/admin/settings`                      | 系统配置（KV）                        |
| 后台-系统    | GET / POST | `/api/v1/admin/role-templates`               | 权限模板（仅 super_admin）            |

> 完整 OpenAPI 契约后续放在 `docs/api/*.yaml`。
> **所有 `/api/v1/admin/**` 接口由独立 `AdminAuthFilter` + `@RequiresPerm` 注解校验，详见 §6.4。**

---

## 六、关键流程与时序

### 6.1 申请加入项目（项目卡 → 私信）

```
用户A           前端                后端              DB              用户B
  |--点击"申请"->|                   |                |               |
  |              |--POST /applications|                |               |
  |              |                   |--开启事务------>|               |
  |              |                   | 1. 写 Application                |
  |              |                   | 2. 写/复用 Conversation          |
  |              |                   | 3. 写 Message(系统:申请加入)     |
  |              |                   |<---提交--------|                |
  |              |                   |--推 WebSocket(若 B 在线)-------->|
  |              |                   |--写 Notification--->            |
  |              |<--200 OK + ids----|                |                |
  |<--toast------|                   |                |                |
```

### 6.2 推荐流读取（项目卡）

1. 网关 → 应用层 `RecommendationService.listProjects(...)`
2. 命中 Redis：`recommend:projects:{filterHash}:{page}` （TTL 60s）
3. 未命中 → MySQL 联合 `ProjectCard + ProjectRole + ProjectTag + User` 查询，分页
4. 写回 Redis（**前 3 页**才缓存，避免内存浪费）
5. 在应用层做隐私过滤（按 `UserSettings.privacy_*`）

> 1000 人规模无需复杂的"个性化推荐算法"，按 `urgency / latest` 简单排序即可；二期可加协同过滤。

### 6.3 私信过期与清理（XXL-JOB）

| 任务                   | 频率           | 行为                                                         |
|------------------------|---------------|--------------------------------------------------------------|
| `expire-conversations` | 每 30 分钟     | `WHERE last_message_at < now()-7d AND status=active` → 标记 expired，发送系统提示消息 |
| `delete-conversations` | 每天 03:00     | `WHERE status=expired AND now() > final_delete_at` → 物理删除 |
| `remind-24h`           | 每 1 小时      | 过期前 24h 推站内通知                                        |
| `recompute-hot-posts`  | 每 30 分钟     | 论坛热度重算，写 Redis Sorted Set                            |
| `recompute-top-skills` | 每天 02:00     | 看板"紧缺技能 Top 20"                                        |
| `purge-activation`     | 每天 04:00     | 清理过期 `ActivationToken`                                    |
| `summarize-audit`      | 每天 05:00     | 聚合 `AuditLog` → 月度合规报表                                |

---

### 6.4 管理员认证与权限校验

```
浏览器(Admin SPA)
   │
   │ 1. POST /api/v1/admin/auth/login (email + password + captcha)
   ▼
AdminAuthController
   │ 2. 校验 password_hash + role∈{admin,super_admin} + status=active
   │ 3. 写 AdminLoginLog（含 IP / UA）
   │ 4. 失败 5 次锁定 15 分钟（Redis Bucket）
   │ 5. 签发 JWT（claims: user_id, role, perms[], is_admin=true）
   ▼
浏览器                     Authorization: Bearer <jwt>
   │
   │ 6. GET /api/v1/admin/users
   ▼
GlobalFilter
   │ 7. JwtFilter 解析 → SecurityContext
   ▼
AdminAuthFilter（仅作用 /admin/**）
   │ 8. 拒绝 is_admin=false 请求；限 IP 段（可选）；记录 ip
   ▼
@RequiresPerm("user:read") 注解切面
   │ 9. 校验当前用户 perms 数组包含该权限码
   ▼
Controller → Service
   │ 10. 写操作进入 @Auditable 切面：序列化 before → 执行 → after → 落 AuditLog
   ▼
返回响应
```

**关键决策：**

| 维度              | 设计                                                                                |
|-------------------|-------------------------------------------------------------------------------------|
| 认证入口隔离      | C 端 `/api/v1/auth/login`、后台 `/api/v1/admin/auth/login` 是**两个不同接口**，限流策略独立 |
| Token 隔离        | 同一份 JWT 但 `is_admin` claim 决定是否放行 `/admin/**`；可配置不同有效期（admin 默认 8 小时） |
| 权限粒度          | `@RequiresPerm("xxx")` 注解 + 应用启动扫描白名单。super_admin 持有全部权限码（运行时合并） |
| 二次确认          | 高危动作（删除、封禁、改 super_admin）后端要求请求体携带 `confirmation_token`（前端弹密码确认弹窗后请求 `/admin/auth/confirm` 拿到，5 分钟有效） |
| 越权防护          | 不允许 admin 修改自己 → super_admin；不允许 admin 创建 super_admin（仅 super_admin 可） |
| 速率限制          | `/api/v1/admin/auth/login` 每 IP 每分钟 5 次；其他写接口每用户每秒 20 次             |
| 审计              | 所有写操作经 `@Auditable` 切面强制落 `AuditLog`，不接受应用代码绕过                   |
| 双因素认证（二期）| 首次登录强制绑定 TOTP；登录时除密码外校验 6 位动态码                                  |
| IP 白名单（可选） | 通过 `SystemConfig.admin.ip_whitelist` 控制，不配置则不限制                          |

### 6.5 手动添加账号（admin → 用户激活）

```
super_admin
   │ 1. POST /api/v1/admin/users { email, name, role, perms[], init_method }
   ▼
UserAdminService.createUser()
   │ 2. 校验 role 与权限：admin 只能创建 student/teacher；super_admin 可创建任意
   │ 3. 校验 email 域名：role=student/teacher 时若不强制 @ruc.edu.cn → 警告并要求二次确认
   │ 4. 写 User（status=pending_first_login）
   │ 5. init_method=email_link → 生成 ActivationToken（24h 有效）→ 发邮件
   │    init_method=set_password → 直接 bcrypt 入库（不发邮件，admin 自行通知）
   │ 6. 写 AuditLog(action=create_user)
   ▼
被创建用户
   │ 7. 收到邮件，点击链接 → /activate?token=xxx
   ▼
ActivationController
   │ 8. 校验 token、未使用、未过期
   │ 9. 引导用户设置密码 → User.password_hash + status=active
   │ 10. 标记 token used_at；签发 JWT，跳首页
```

---

## 七、性能与扩展性

### 7.1 一期容量推演

- 单实例 Spring Boot（4C8G）QPS 上限约 1500（Spring Boot + JDBC，CPU 50% 时），**远高于 100 QPS 预估值**。
- MySQL 单库行数：项目卡 < 5k、个人卡 < 1k、私信 ≈ 200w（10k/日 × 200天累积），**完全在单库可控范围**。
- WebSocket 同时在线 100 ≈ 几 MB 堆，单 JVM 完全没问题。

### 7.2 容易踩的坑

| 风险点                                | 缓解方案                                              |
|---------------------------------------|-------------------------------------------------------|
| 推荐流"全表扫"                        | 关键索引：`(status, team_deadline)`、`(competition_id)`、`(updated_at)`，加 Redis 60s 缓存 |
| 论坛"最新"被 N+1                      | 列表接口 join `User` 取昵称 / 院系；在应用层 batchLoad |
| 私信热点 conversation                 | `Message.(conversation_id, sent_at)` 联合索引；游标分页 |
| 突发流量（开学第一周）                | Nginx 限速（IP / 用户）+ Redis Bucket Token 限流      |
| 越权访问（看别人的私信 / 编辑）       | `@LoginUser` + 资源所有权校验拦截器，统一封装          |

### 7.3 扩展路径

```
阶段一(<1k 用户)  阶段二(1k~5k)         阶段三(>5k)
单体 + 单库 -->  单体 + 读写分离  -->  按模块拆服务 + ES + MQ
                 + Redis 缓存          + 多机房备份
```

**演进的"骨架接口"**：当前 `messageService` / `forumService` 等 Facade，未来直接拆为微服务，调用方零改动。

---

## 八、安全设计

### 8.1 通用安全

| 维度       | 措施                                                                   |
|------------|------------------------------------------------------------------------|
| 传输       | 全站 HTTPS（Let's Encrypt 免费证书自动续签）                            |
| 鉴权       | JWT（HS256，密钥放环境变量 / KMS）；refresh 单点撤销（Redis 黑名单）    |
| 越权       | 统一 `@RequireOwnership` 注解，校验资源 owner / admin                    |
| XSS        | 后端转义；论坛 Markdown 渲染走 DOMPurify 白名单                          |
| SQL 注入   | 全部参数绑定（MyBatis `#{}`），禁用拼接 `${}`                            |
| CSRF       | SPA + JWT Bearer（不依赖 Cookie auth）天然免疫；如果回退到 Cookie，启用 SameSite=Lax + CSRF Token |
| 敏感数据   | 真实姓名 / 微信号入库不加密但**响应层屏蔽**；手机号脱敏展示             |
| 限流       | Nginx 漏桶 + 应用层 Redis Bucket（按用户 + 接口）                        |
| 内容安全   | 关键词词典（敏感、广告、色情）+ 人工举报队列；命中即 `auto_filter_result=block` |
| 备份       | RDS 每日全量 + binlog；OSS 跨 Region 复制                                |
| 审计       | 关键操作（删除、状态变更、管理员动作）写 `ModerationLog` / `AuditLog`    |
| 隐私合规   | 私信彻底删除策略 + 用户可下载 / 注销账号                                  |

### 8.2 管理员账号与后台专项

| 维度                | 措施                                                                                  |
|---------------------|---------------------------------------------------------------------------------------|
| 账号来源            | 管理员**只能由 super_admin 在后台创建**，不开放自助注册                                |
| 密码策略            | 至少 10 位 + 大小写 + 数字 + 符号；首次登录必须改密；90 天提示更换                     |
| 登录失败            | 同账号 5 次失败锁定 15 分钟（Redis）；同 IP 30 次锁定 1 小时；通过 `AdminLoginLog` 监控 |
| Captcha             | 后台登录默认开启图形验证码（hCaptcha / Turnstile）                                      |
| 二次确认            | 删除 / 封禁 / 改 super_admin / 编辑权限模板需"密码 / TOTP" 二次验证（confirmation_token） |
| TOTP（二期）        | super_admin / 管理员强制启用                                                           |
| Session 防共享      | JWT 包含 `device_id` claim，与登录设备绑定；切设备需重新登录                            |
| IP 白名单（可选）    | 通过 `SystemConfig.admin.ip_whitelist` 限制后台访问网段                                |
| 审计                | `AuditLog` 表只读不可改不可删；**应用账号仅有 INSERT 权限**（DB 层强制）                |
| 越权防护            | admin 不能编辑/封禁 super_admin；admin 不能为他人 / 自己提升权限至超出自己已有的权限码  |
| 内容查看权限        | 管理员**默认看不到任何私信**；只有被举报的会话可巡查 ± 10 条上下文，全程审计           |
| 异常登录告警        | 异地 / 新设备首次登录 → 邮件 + 站内通知告警                                             |
| 弱密码扫描          | 定时扫 password_hash 是否在弱密码字典（HIBP top 1k）→ 强制改密                          |
| 操作审计可视化      | `/admin/audit-logs` 仅 `audit:read` 可见；前端隐藏不该看到的字段                         |

---

## 九、可观测性

| 维度    | 工具                            | 覆盖                                          |
|---------|---------------------------------|-----------------------------------------------|
| Metrics | Micrometer + Prometheus + Grafana | QPS / RT / Error rate / JVM / 慢 SQL          |
| Logs    | Logback JSON + Loki              | trace_id 串联 Web → Service → DB              |
| Trace   | OpenTelemetry / SkyWalking（二期）| 关键链路                                      |
| 业务看板 | Grafana 自建仪表板                | DAU / 私信数 / 项目卡数 / 申请数 / 申请成功率 |
| 告警    | PrometheusAlertmanager → 钉钉 / 邮箱 | 5 分钟错误率 > 1%、CPU > 80%、磁盘 > 80% |

---

## 十、上线节奏（与 docx 中"6 + 4 个半天"匹配）

> docx 估算：6 个半天完成 75%、4 个半天完成 25%。下表是把功能切成两期，与时间盒对齐。

### 一期（MVP，6 个半天 ≈ 24 小时）

| 半天 | 主要工作                                                                                                    |
|------|-------------------------------------------------------------------------------------------------------------|
| 1    | 基础工程：前端 Vite + Vue3 + 双子站点架构（C 端 + Admin）；后端 Spring Boot；Nginx + HTTPS                    |
| 2    | 用户：邮箱登录 + RBAC + JWT；**管理后台账户管理（用户列表 / 新增账号 / 启停 / 改角色 / 审计）**；Flyway 迁移   |
| 3    | 项目卡：发布 / 推荐流 / 详情 / 编辑；**后台项目卡治理（下架 / 隐藏 / 删除）**                                  |
| 4    | 个人卡：发布 / 推荐流；**后台个人卡治理**                                                                    |
| 5    | 私信：会话 + WebSocket + 申请触发会话；过期任务；**后台私信巡查（仅被举报）**                                  |
| 6    | 比赛中心 + 资讯（**管理员录入 = 后台 CRUD**）+ 我的中心 + **数据看板 v1**                                     |

### 二期（4 个半天 ≈ 16 小时）

| 半天 | 主要工作                                                                                          |
|------|---------------------------------------------------------------------------------------------------|
| 7    | 论坛：发帖 / 评论 / 点赞 / 话题；热度计算定时任务；**后台论坛治理（置顶 / 加精 / 隐藏）**           |
| 8    | 后台：举报队列 + 关键词管理 + 审计日志 + 角色权限模板 + 系统配置                                    |
| 9    | 移动端响应式优化 + 全文搜索（MySQL FULLTEXT）+ 双因素认证 / IP 白名单 + 体验抛光                    |
| 10   | （可选）AI 助手：比赛攻略 RAG / 个人卡润色；CAS 单点登录对接；导出 / 高级看板                       |

---

## 十一、数据库设计要点

> 详细字段以 `er-diagram.puml` 为准。这里列**索引、约束与冷热分离**关键决策。

### 11.1 关键索引

| 表             | 索引                                                                                |
|----------------|-------------------------------------------------------------------------------------|
| `User`         | UNIQUE(`ruc_email`); INDEX(`status`, `last_login_at`)                                 |
| `ProjectCard`  | INDEX(`status`, `team_deadline`); INDEX(`competition_id`); INDEX(`creator_id`); INDEX(`updated_at`) |
| `ProjectRole`  | INDEX(`project_id`)                                                                  |
| `ProjectTag`   | INDEX(`project_id`); INDEX(`tag`)                                                    |
| `Application`  | INDEX(`project_id`, `status`); INDEX(`applicant_id`, `created_at`)                   |
| `PersonalCard` | UNIQUE(`user_id`); INDEX(`is_visible`, `updated_at`); INDEX(`target_role`)           |
| `PersonalSkill`| INDEX(`card_id`); INDEX(`skill`)                                                     |
| `Conversation` | INDEX(`participant_a_id`, `last_message_at`); INDEX(`participant_b_id`, `last_message_at`); INDEX(`status`, `expires_at`) |
| `Message`      | INDEX(`conversation_id`, `sent_at`)                                                  |
| `ForumPost`    | INDEX(`status`, `last_reply_at`); INDEX(`is_essence`, `last_reply_at`); FULLTEXT(`title`, `content_md`) WITH PARSER ngram |
| `ForumComment` | INDEX(`post_id`, `created_at`)                                                        |
| `Notification` | INDEX(`user_id`, `is_read`, `created_at`)                                              |
| `User`         | UNIQUE(`email`); INDEX(`role`, `status`); INDEX(`source`); INDEX(`created_by_admin_id`); INDEX(`status`, `last_login_at`) |
| `ActivationToken` | UNIQUE(`token`); INDEX(`user_id`, `purpose`); INDEX(`expires_at`)                  |
| `AuditLog`     | INDEX(`actor_id`, `created_at`); INDEX(`target_type`, `target_id`); INDEX(`action`, `created_at`) |
| `AdminLoginLog`| INDEX(`admin_id`, `created_at`); INDEX(`ip`, `created_at`)                          |
| `KeywordRule`  | INDEX(`enabled`)                                                                      |
| `RoleTemplate` | UNIQUE(`name`)                                                                        |
| `SystemConfig` | PRIMARY(`config_key`)                                                                 |
| `Report`       | INDEX(`status`, `created_at`); INDEX(`content_type`, `content_id`); INDEX(`moderator_id`) |

### 11.2 命名与约束

- 主键 ID 用 `VARCHAR(32)`，由雪花算法 / `ULID` 生成（保证趋势递增 + 全局唯一）。
- 时间字段统一 `DATETIME(0)` UTC 存，前端按时区显示。
- 软删除：业务表加 `deleted_at`，构造 partial unique index 时考虑。
- 大文本字段（`content_md`、`project_detail`）用 `TEXT`，列表查询不 SELECT 整列。

### 11.3 数据归档策略

| 表             | 策略                                                              |
|----------------|-------------------------------------------------------------------|
| `Message`      | `final_delete_at` 触发硬删；按月分表（二期，> 200w 行后）         |
| `ProjectCard`  | `closed` 状态保留 1 年后归档到冷表 `project_card_archive`         |
| `Notification` | 90 天前已读通知物理删除                                            |
| `News`         | `archived` 状态保留，不删                                          |

---

## 十二、风险与开放问题

| 风险 / 问题                              | 应对                                                                 |
|------------------------------------------|----------------------------------------------------------------------|
| 校园用户对"换微信即跑路"导致平台留存差   | 设计积分 / 排行榜 / 论坛激励，鼓励经验回流；私信中保留链接埋点统计   |
| 关键词敏感词维护成本                     | 一期用静态词表 + 人工举报双轨，二期接入云厂商内容安全 API            |
| 真实姓名 / 学工号合规                    | 不展示给同学；登录时一次性最小化采集；提供注销 / 数据下载入口         |
| AI 助手接入 LLM 成本                     | 限流 + 缓存常见问答；二期再上                                         |
| 人大 CAS 接入                            | 一期先 `@ruc.edu.cn` 白名单，二期再做 OAuth2/CAS                      |

---

## 十三、技术选型一览（决策记录）

> 记录关键决策（ADR 风格摘要）。

1. **不引入微服务**：1000 人规模微服务收益 < 复杂度；按模块分包预留拆分边界即可。
2. **Vue 3 而非 React**：与现有 HTML 风格 / 国内中文生态对接更顺；Element Plus 与 RUC 红配色一致。
3. **Spring Boot 而非 Node**：团队 Java 储备多 + 事务模型清晰，私信 / 申请等组合事务好写。
4. **WebSocket 而非纯轮询**：私信是核心场景，5s 轮询体验差且服务端 QPS 浪费严重。
5. **MySQL 全文索引而非 ES**：一期数据量 < 5w，FULLTEXT + ngram 足够，少一个组件意味着少一份运维。
6. **JWT 而非 Session**：SPA + 后续多端（H5 / 公众号）一致；Redis 黑名单解决"立即登出"问题。

---

## 十四、参考交付物

- 数据 ER 图：[`er-diagram.puml`](./er-diagram.puml)
- 前端改造接入指引（HTML → Vue 路由）：[`html-modifications.md`](./html-modifications.md)
- 功能字段规范：[`../ReadeMe.md`](../ReadeMe.md)
- API 契约（待补）：`docs/api/*.yaml`

---

> **下一步建议**：
> 1. 与团队对齐 README + 本 design.md，决定是否使用 Vue3 + Spring Boot；
> 2. 锁定一期 MVP 功能边界（即上文"一期"6 个半天）；
> 3. 在 GitHub / Gitee 开仓，按本文目录结构初始化空工程，加 CI；
> 4. 数据库 DDL 用 Flyway 迁移脚本管理（每张表一个文件）；
> 5. 把现有 9 个 HTML 按 [`html-modifications.md`](./html-modifications.md) 一一改造为 Vue 路由页面。
