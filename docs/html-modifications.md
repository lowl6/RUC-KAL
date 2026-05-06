# 前端 HTML 改造接入指引（HTML → Vue 3 SPA）

> 本文档**只描述"应该怎么改"**，不给具体代码。配套：[`design.md`](./design.md)、[`../ReadeMe.md`](../ReadeMe.md)。
>
> 目标读者：负责把现有 9 个静态 HTML 页面（`frontend/*.html`）迁移到 Vue 3 + Vite + Pinia + Element Plus SPA 的开发者。

---

## 一、改造总思路

1. **保留视觉资产**：现有 HTML 已经把"人大红配色 + 卡片 + 圆角 + Roboto"风格定型，整体设计语言**直接复用**，避免重做 UI。
2. **只改"骨架"，不改"皮肤"**：把每个 HTML 拆解成 Vue 组件 + 路由页面；CSS 全局变量 (`:root`) 抽取为 `tokens.css`，组件级 CSS 走 `<style scoped>`。
3. **统一交互层**：所有 `alert / confirm / location.href` 全部替换为：
   - 路由跳转 → `router.push`
   - 提示反馈 → `ElMessage` / `ElMessageBox`
   - 真实数据 → Axios 请求；先用 Mock 接口（MSW / Mock.js）平滑过渡。
4. **接口先行**：每个页面在动手前**先在 `src/api/` 定义好该页面用到的接口契约**，参考 `design.md §5`，前后端按 OpenAPI 并行开发。
5. **状态分层**：
   - 跨页全局：当前用户 / 通知红点 → Pinia Store（`useUserStore` / `useNotifyStore`）
   - 单页局部：表单数据、筛选器 → 组件 `ref / reactive`
   - 缓存型：推荐流分页 → Pinia 内可选 `keepAlive` 或 `nuxt-img`-like 策略

---

## 二、共性改造（所有页面通用）

### 2.1 全局样式抽取

把 9 个 HTML 中**重复出现的 `:root` 变量**提取到 `frontend/src/styles/tokens.css`：

| 变量                | 用途                  |
|---------------------|-----------------------|
| `--ruc-red`         | 主品牌色（`#861a12`） |
| `--ruc-red-light`   | 副品牌色              |
| `--ruc-red-dark`    | 强调色                |
| `--ruc-red-bg`      | 浅背景                |
| `--ruc-red-hover`   | hover 态              |
| `--bg-light`        | 全局浅灰底            |
| `--text-primary`    | 主文本                |
| `--text-secondary`  | 副文本                |
| `--border-color`    | 边框                  |

并在 `vite.config.ts` 配置 `css.preprocessorOptions` 注入；在 `App.vue` 入口 `import './styles/tokens.css'`。
组件层 CSS 类名统一带 `kal-` 前缀（如 `.kal-card`、`.kal-tag`）防止与 Element Plus 冲突。

### 2.2 顶部导航 / 页脚组件化

现状：每个 HTML 页面都重复了 `<header class="header"> ... </header>`。

改造：抽出 `components/layout/AppHeader.vue` + `AppFooter.vue`（如有）：

- `AppHeader` 接受 `props: { activeMenu: 'home' | 'competitions' | 'forum' | 'my-center' }`，根据 `route.name` 自动高亮；
- 右上角"+ 发布"按钮改为 `ElDropdown`，去除现有的 `confirm` hack；
- 消息红点订阅 `useNotifyStore`，离开 messages 页面时自动重新拉取。

### 2.3 路由表

| 现有 HTML                    | 路由 path                  | name              | 组件                |
|------------------------------|----------------------------|-------------------|---------------------|
| `login.html`                 | `/login`                   | `Login`           | `views/Login.vue`   |
| `index.html`                 | `/`                        | `Home`            | `views/Home.vue`    |
| `publish-project.html`       | `/projects/new`            | `PublishProject`  | `views/PublishProject.vue` |
| `project-detail.html`        | `/projects/:id`            | `ProjectDetail`   | `views/ProjectDetail.vue`  |
| `publish-profile.html`       | `/personal-cards/edit`     | `EditPersonalCard`| `views/PublishProfile.vue` |
| `competitions.html`          | `/competitions`            | `Competitions`    | `views/Competitions.vue`   |
| -（新增）                    | `/competitions/:id`        | `CompetitionDetail`| `views/CompetitionDetail.vue` |
| `forum.html`                 | `/forum`                   | `Forum`           | `views/Forum.vue`   |
| -（新增）                    | `/forum/posts/:id`         | `PostDetail`      | `views/PostDetail.vue` |
| -（新增）                    | `/forum/posts/new`         | `PublishPost`     | `views/PublishPost.vue` |
| `messages.html`              | `/messages`                | `Messages`        | `views/Messages.vue` |
| `messages.html` (右栏)       | `/messages/:conversationId`| `Conversation`    | （同上，子路由）     |
| `my-center.html`             | `/me`                      | `MyCenter`        | `views/MyCenter.vue` |
| -（新增）                    | `/admin/...`               | `Admin*`          | `views/admin/*.vue` |

**路由守卫**：`router.beforeEach` 检查 `useUserStore().token`，未登录访问受保护页面重定向到 `/login?redirect=...`。

### 2.4 全局拦截

- Axios 请求拦截：自动注入 `Authorization` + `trace_id`
- Axios 响应拦截：统一处理 `code !== 0`（`ElMessage.error(message)`）、401 → 刷 token / 跳登录、5xx → 上报
- 全局 loading：`pinia` 的 `useUiStore.loading` 计数器；`v-loading="store.loading > 0"`

---

## 三、逐页改造说明

### 3.1 `login.html` → `views/Login.vue`

| 项                           | 现状                                              | 改造目标                                                |
|------------------------------|---------------------------------------------------|---------------------------------------------------------|
| 字段                         | 姓名 + `@ruc.edu.cn` 邮箱                          | 保持；前端校验邮箱后缀；密码（首次自动设置）             |
| 提交                         | `simulateLogin` alert                             | `POST /api/v1/auth/login` → 写 `useUserStore`，跳 `/`    |
| 错误处理                     | 无                                                | 邮箱后缀校验、网络错误、限流提示                         |
| 登录态                       | 无                                                | JWT access 入 `httpOnly Cookie`；refresh 入内存 + 持久化 |
| 二期                         | -                                                 | 增加"使用 RUC 单点登录"按钮（CAS）                       |

### 3.2 `index.html` → `views/Home.vue`

**关键改造点：**

| 区域                | 现状                              | 改造目标                                                                  |
|---------------------|-----------------------------------|---------------------------------------------------------------------------|
| Tab 切换            | `switchTab(...)` + `display:none` | `<el-tabs v-model="activeTab">`，路由 query `?tab=projects|teammates`     |
| 筛选器              | 静态 `<select>`                   | 双向绑定 `filterForm`，`watch` 触发请求；改 URL query 利于分享              |
| 项目卡列表          | 4 个写死的 `<div class="project-card">` | `v-for` + `<ProjectCard :data="item">` 子组件；`GET /api/v1/projects?...` |
| 个人卡列表          | 4 个写死                           | 同上：`<PersonalCard>` 组件 + `GET /api/v1/personal-cards`                 |
| 分页                | 静态                               | `<el-pagination>` 双向绑定                                                 |
| "+ 发布"            | `confirm` 提示二选一               | `<el-dropdown>` 菜单：发布项目卡 / 发布个人卡                              |
| 私信红点            | 写死 `3`                           | `useNotifyStore().unreadMessages`，登录后定时拉 / WS 推                    |
| 卡片点击 → 详情     | `location.href='project-detail.html'` | `router.push({ name: 'ProjectDetail', params: { id }})`                  |

**子组件抽取：**
- `components/card/ProjectCard.vue`（列表项）
- `components/card/PersonalCard.vue`（列表项）
- `components/filter/ProjectFilterBar.vue`
- `components/filter/PersonalFilterBar.vue`

### 3.3 `publish-project.html` → `views/PublishProject.vue`

| 项                | 现状                                | 改造目标                                                            |
|-------------------|-------------------------------------|---------------------------------------------------------------------|
| 表单结构          | 原生 `<form>` + 多 `section`         | `<el-form ref="formRef">` + `el-form-item` + `rules`                |
| 字符计数          | `maxlength`                         | 保留；同时加 `el-input` `show-word-limit` 字数实时显示              |
| 角色动态增删      | `addRole()` / `removeRole()`        | `v-for` 渲染 `roles` 数组，`<el-button>` 增删；最少 1 行            |
| 截止日期联动       | 无                                   | 选择 `competition_deadline` 自动填 `team_deadline = -14d`，可手改  |
| 提交              | alert 模拟                          | `POST /api/v1/projects` → 成功 `router.push('/projects/'+id)`        |
| 草稿              | 无                                   | 二期：`localStorage` 保留草稿（防意外刷新）                          |
| 路由参数          | -                                   | 支持 `?prefill_competition=internet`（来自比赛中心的"我也要组队"）   |

### 3.4 `publish-profile.html` → `views/PublishProfile.vue`

| 项                  | 现状                          | 改造目标                                                                 |
|---------------------|-------------------------------|--------------------------------------------------------------------------|
| 一人一卡            | 无逻辑                         | 进入页面前 `GET /api/v1/personal-cards/me`，存在则切换为"编辑模式"      |
| 技能标签输入        | 自定义 input + 添加按钮        | 改用 `<el-tag closable>` + `<el-input v-model="skillInput">`，回车添加 |
| 比赛兴趣多选        | 多个 checkbox                  | `<el-checkbox-group v-model="interestedCompetitions">`                  |
| 隐私 toggle         | 普通 checkbox                  | `<el-switch>`，绑定 `UserSettings.privacy_*`                            |
| 提交                | alert                         | `PUT /api/v1/personal-cards`（幂等）→ 跳 `/me`                          |

### 3.5 `project-detail.html` → `views/ProjectDetail.vue`

| 区域                  | 现状                                | 改造目标                                                                       |
|-----------------------|-------------------------------------|--------------------------------------------------------------------------------|
| 基本信息              | 静态                                 | `GET /api/v1/projects/:id`，loading / 404 / 已下架三种状态                      |
| 所需人员              | 静态                                 | 渲染 `roles[]`，每行展示"已招到 X / 计划 Y"进度条                                |
| 技术标签              | 静态                                 | 渲染 `tags[]`                                                                   |
| 项目负责人            | 静态                                 | 受 `creator.privacy_*` 控制；展示昵称 + 院系 + 头像                              |
| 项目统计              | 静态                                 | 浏览次数：进入详情即 `POST /api/v1/projects/:id/view`（去重 30min）             |
| 截止提醒              | 静态                                 | 根据 `team_deadline` 计算 `days_left`，< 7 天显示橙色 / < 3 天红色             |
| 相关项目推荐          | 静态                                 | `GET /api/v1/projects?related_to=:id`                                           |
| "申请加入"按钮        | 无                                   | 当 `can_apply=true` 时显示；点击 → `POST /:id/applications` → 跳私信会话        |
| "编辑 / 结束组队"     | 无                                   | 当 `can_edit=true` 时显示菜单                                                    |

### 3.6 `competitions.html` → `views/Competitions.vue` + `CompetitionDetail.vue`

| 项                | 现状                                          | 改造目标                                                                  |
|-------------------|-----------------------------------------------|---------------------------------------------------------------------------|
| 比赛卡列表        | 6 个静态卡                                    | `GET /api/v1/competitions?status=upcoming|active|urgent` 三组             |
| 比赛状态徽章      | CSS class                                     | 后端返回 `status`，前端 `:class` 映射                                      |
| 关联项目数        | 静态                                          | 后端 `project_count` 字段（冗余统计）                                     |
| 相关资讯          | 静态 6 条                                     | `GET /api/v1/news?page_size=10`，分页 / 加载更多                           |
| 比赛卡点击        | `viewCompetitionDetail('xxx')` alert          | `router.push({ name: 'CompetitionDetail', params: { id } })`              |
| 详情页（新建）    | 无                                            | 比赛信息 + 官方链接 + "我也要组队"按钮（带 `prefill_competition`）+ 关联项目列表 |

### 3.7 `forum.html` → `views/Forum.vue` + `PostDetail.vue` + `PublishPost.vue`

| 项                | 现状                              | 改造目标                                                                      |
|-------------------|-----------------------------------|-------------------------------------------------------------------------------|
| 热门话题          | 静态 7 个                         | `GET /api/v1/forum/topics/hot`（Redis 排行榜）                                |
| 帖子列表          | 8 个静态                           | `GET /api/v1/forum/posts?tab=latest|hot|essence&topic=xxx&page=1`             |
| Tab 切换          | `tab-btn` class                   | `<el-tabs>` 改 URL query                                                      |
| 徽章（置顶/精华/新/热门）| 静态                       | 后端 `status` / `is_essence` / `created_at` / `view_count` 计算               |
| 帖子点击          | `viewPost(id)` alert              | `router.push('/forum/posts/'+id)`                                             |
| "+ 发帖"          | 跳 `publish-post.html`（不存在！）| 跳 `/forum/posts/new`，富文本用 Markdown 编辑器（`md-editor-v3`）             |
| 详情页（新建）    | 无                                 | 标题 / 作者 / 正文（Markdown 渲染 + DOMPurify）/ 评论楼层 / 点赞 / 收藏       |
| 楼中楼            | -                                  | 评论支持 `parent_id` 一层折叠                                                 |

### 3.8 `messages.html` → `views/Messages.vue`

> 私信是**最重要的双面板交互**，建议大改：

| 项                  | 现状                                | 改造目标                                                                              |
|---------------------|-------------------------------------|---------------------------------------------------------------------------------------|
| 左栏会话列表        | 5 个静态                            | `GET /api/v1/conversations` + 滚动加载；当前会话用 `route.params.conversationId` 高亮 |
| 右栏聊天            | 1 段静态                            | `GET /api/v1/conversations/:id/messages?cursor=...`，反向滚动加载                     |
| 实时性              | 无                                   | 建立 `/ws/chat` WebSocket，订阅当前 conversation；新消息推送                          |
| 在线状态            | 无                                   | 二期：`presence` 通道                                                                  |
| 输入框              | `<input>`                           | `<el-input type="textarea" :autosize>` + Enter 发送 / Shift+Enter 换行                |
| 快捷按钮            | 4 个静态文案                         | 后端定义模板，前端拉取；点击发送对应 `msg_type`                                       |
| 已读 / 未读         | 无                                   | 进入会话发 `POST /:id/read`，消息列表渲染已读 √                                        |
| 会话过期 7 天提示    | 无                                   | `is_expired=true` 时禁用输入框 + Banner 提示                                          |
| 路由                | 无                                   | `/messages` 默认打开第一条；`/messages/:conversationId` 直链分享                      |

### 3.9 `my-center.html` → `views/MyCenter.vue`

| Tab           | 现状                                  | 改造目标                                                                  |
|---------------|---------------------------------------|---------------------------------------------------------------------------|
| 我的项目卡    | 2 张静态卡 + 申请记录                   | `GET /api/v1/users/me/projects?status=...`，按状态过滤                     |
| 申请记录处理  | 无                                     | 同意 / 拒绝按钮 → `PATCH /api/v1/applications/:id`                        |
| 我的个人卡    | 1 张静态                                | `GET /api/v1/personal-cards/me`，编辑 / 隐藏 / 删除                         |
| 私信入口      | iframe / 提示                           | 跳 `/messages`                                                            |
| 设置          | `showSettings()` alert                 | 抽屉 `<el-drawer>`：通知 / 隐私 / 微信号 / 退出登录                        |

### 3.10 管理员后台

> 详见 [§10 管理后台前端方案](#十管理后台前端方案)。

---

## 四、`<form>` 与字段校验统一规则

> 现有 HTML 用了 `required / maxlength` 原生校验，配合 alert，体验不一致。

| 字段类型      | 推荐组件                          | 校验规则                                                       |
|---------------|-----------------------------------|----------------------------------------------------------------|
| 文本（短）    | `el-input`                        | `required`、`min/max length`                                   |
| 文本（长）    | `el-input type="textarea"`        | `show-word-limit`、`max length`                                |
| 数字          | `el-input-number`                 | `min`/`max`、整数                                              |
| 日期          | `el-date-picker`                  | `disabledDate` 禁选过去日期                                    |
| 单选          | `el-select`                       | `required`                                                     |
| 多选          | `el-checkbox-group`               | -                                                              |
| 标签          | `el-tag` + `el-input`             | 重复标签去重；最多 8 个                                        |
| 邮箱          | `el-input` + `validator`          | 必须以 `@ruc.edu.cn` 结尾                                      |
| 微信号        | `el-input`                        | 字母数字 - _，6~20 位                                          |

校验失败统一用 `el-form` rules + 表单级 `validate()`，不再 `alert`。

---

## 五、数据 Mock 与 API 同步

> 让前后端**并行开发**：

1. 后端发布 OpenAPI yaml 到 `docs/api/`；
2. 前端用 [`openapi-typescript`](https://github.com/drwpow/openapi-typescript) 生成类型；
3. 前端用 MSW（Mock Service Worker）提供本地 mock，**与真实 API 零差异切换**；
4. CI 校验：`pnpm typecheck` 必须通过 OpenAPI 生成的 schema。

---

## 六、性能与可访问性

| 维度       | 改造点                                                                |
|------------|-----------------------------------------------------------------------|
| 首屏       | Vite 路由懒加载（`() => import(...)`）；按页面切包                     |
| 列表       | `vue-virtual-scroller` 仅在私信 / 论坛超长列表时启用                  |
| 图片       | `<el-image lazy>` + WebP 化（OSS 图像处理）                            |
| 缓存       | 推荐流走 `useFetch` + `keepAlive`；返回时不重新请求                    |
| 移动端     | 现有 `@media (max-width: 768px)` 保留并完善；导航折叠为汉堡菜单        |
| 可访问性   | 所有图标补 `aria-label`；色对比度 ≥ AA；表单 `<label for>` 关联        |
| i18n       | 一期不做；所有文本走 `i18n.t('xxx')` 钩子，预留二期接入                 |

---

## 七、迁移顺序建议

按"低风险 → 高价值"渐进：

1. **基础工程 + tokens 抽取**（半天）
2. **AppHeader / AppFooter / 路由 / Pinia 骨架**（半天）
3. **Login → Home（找项目）**（1 天，链路最重要）
4. **PublishProject / ProjectDetail**（1 天）
5. **Home（找队友）+ PublishProfile**（半天）
6. **Messages（含 WebSocket）**（1 天）
7. **Competitions + Detail**（半天）
8. **Forum 三页**（1 天）
9. **MyCenter**（半天）
10. **Admin 后台**（1 天）

> 每完成一页，对应 HTML 文件移入 `frontend/_legacy/`，仅作截图对照保留，不再维护。

---

## 八、检查清单（Definition of Done）

每个页面改造完成需满足：

- [ ] 路由可达，刷新不丢状态
- [ ] 接口接通真实后端（或 MSW Mock），异常 / 空 / 加载三态完整
- [ ] 登录态校验、字段权限正确（隐私字段不暴露）
- [ ] 表单 `validate()` 全通过；提交后跳转 / 反馈一致
- [ ] 移动端 ≤ 480px 可正常使用
- [ ] Lighthouse 性能分 ≥ 80（移动端）
- [ ] 关键交互埋点（页面 PV、按钮 Click）已上报
- [ ] 单元测试 / 组件快照覆盖核心组件

---

## 九、参考

- Element Plus：<https://element-plus.org/>
- Vue Router：<https://router.vuejs.org/>
- Pinia：<https://pinia.vuejs.org/>
- MSW：<https://mswjs.io/>
- md-editor-v3：<https://imzbf.github.io/md-editor-v3/>

> **结论**：现有 9 个 HTML 是"高保真原型"，UI / 信息架构基本不动；改造的核心工作在**抽组件、对接接口、加路由 / 状态、做校验与权限**。按本指引执行，一期 6 个半天能跑通主链路。

---

## 十、管理后台前端方案

> 对应 README [模块九](../ReadeMe.md#模块九管理后台) 与 design.md §6.4 / §6.5 / §8.2。
> 角色：admin / super_admin；C 端用户**完全看不到该子站点**。

### 10.1 子站点结构与隔离

| 维度          | C 端                              | 管理后台                                                 |
|---------------|-----------------------------------|----------------------------------------------------------|
| URL           | `https://kal.ruc.edu.cn/`         | `https://kal.ruc.edu.cn/admin/`（同域，独立路由前缀）    |
| 入口模板      | `index.html`                      | `admin.html`（独立 Vite entry，单独打包）                |
| 路由前缀      | `/`                               | `/admin/*`                                               |
| 视觉风格      | 人大红 + 卡片流                    | 人大红 + **左侧菜单 + 顶部面包屑**（标准 console 模板）  |
| 登录入口      | `/login`                          | `/admin/login`（独立页面、独立 captcha）                 |
| 鉴权要求      | role ∈ {student,teacher} 或不需要 | **role ∈ {admin,super_admin} + 权限码白名单**            |
| 路由守卫      | 检查 `token`                      | 检查 `token` + `role` + `RequiresPerm` 权限码            |
| Session 时效  | access 30min                       | **access 8h，但每次写操作都过 confirmation_token**       |

**为什么用同域子路径而不是子域名？**
省去跨域 / 单独证书 / Cookie 配置的复杂度；通过 Nginx 把 `/admin` 路径下的资源指向独立打包产物（`dist-admin/`）即可。

### 10.2 目录结构（前端）

```
frontend/
├── public/
├── src/
│   ├── apps/
│   │   ├── client/                  C 端入口（已有，对应 main.ts）
│   │   │   ├── App.vue
│   │   │   ├── main.ts
│   │   │   └── router.ts
│   │   └── admin/                   管理后台入口（新增）
│   │       ├── App.vue              整体布局（AdminLayout）
│   │       ├── main.ts              独立 Pinia / Element Plus 注册
│   │       └── router.ts            /admin/* 路由 + 守卫
│   ├── api/
│   │   ├── client/                  C 端接口
│   │   └── admin/                   后台接口（独立 Axios 实例 + 拦截器）
│   ├── components/
│   │   ├── client/...
│   │   └── admin/                   后台专用组件
│   │       ├── AdminLayout.vue      左菜单 + 顶栏 + 主内容
│   │       ├── AdminSidebar.vue     菜单（按 perms 渲染）
│   │       ├── AdminBreadcrumb.vue  面包屑
│   │       ├── AdminUserCell.vue    用户单元格（头像 + 名称 + 状态徽标）
│   │       ├── ResourceTable.vue    通用资源表格（搜索 / 筛选 / 分页）
│   │       ├── ConfirmDialog.vue    高危动作 reason + 二次确认
│   │       ├── PermSelector.vue     权限码多选（按模块分组）
│   │       └── AuditViewer.vue      审计日志查看器
│   ├── views/
│   │   ├── client/...
│   │   └── admin/                   后台页面
│   │       ├── Login.vue
│   │       ├── Dashboard.vue
│   │       ├── users/
│   │       │   ├── UserList.vue
│   │       │   ├── UserNew.vue
│   │       │   ├── UserDetail.vue   抽屉，可独立路由
│   │       │   └── AdminList.vue
│   │       ├── content/
│   │       │   ├── ProjectList.vue
│   │       │   ├── PersonalCardList.vue
│   │       │   ├── ForumPostList.vue
│   │       │   ├── ForumCommentList.vue
│   │       │   └── MessageInspect.vue
│   │       ├── competitions/
│   │       │   ├── CompetitionList.vue
│   │       │   ├── CompetitionForm.vue
│   │       │   ├── NewsList.vue
│   │       │   └── NewsForm.vue
│   │       ├── moderation/
│   │       │   ├── ReportList.vue
│   │       │   ├── KeywordList.vue
│   │       │   └── AuditLogList.vue
│   │       └── system/
│   │           ├── RoleTemplateList.vue
│   │           ├── SystemSettings.vue
│   │           └── Profile.vue
│   ├── stores/
│   │   ├── userStore.ts             共用：当前用户 + perms[]
│   │   ├── notifyStore.ts
│   │   └── admin/                   后台专用 store
│   │       ├── permStore.ts         perms / hasPerm(code)
│   │       └── tableStore.ts        列表条件持久化
│   └── styles/
│       ├── tokens.css
│       └── admin.css                后台专用变量（更紧凑表格 / 行高）
├── vite.config.ts                   配置 multi-page
└── package.json
```

`vite.config.ts` 使用 `build.rollupOptions.input = { client: 'index.html', admin: 'admin.html' }`，输出 `dist/index.html` 与 `dist/admin/index.html`，由 Nginx 分别 try_files。

### 10.3 路由表（管理后台）

| path                                  | name                  | 组件                              | 权限码                  |
|---------------------------------------|-----------------------|-----------------------------------|-------------------------|
| `/admin/login`                        | `AdminLogin`          | `admin/Login.vue`                 | （无）                   |
| `/admin`                              | redirect → dashboard  |                                   |                         |
| `/admin/dashboard`                    | `AdminDashboard`      | `admin/Dashboard.vue`             | `dashboard:read`        |
| `/admin/users`                        | `AdminUsers`          | `admin/users/UserList.vue`        | `user:read`             |
| `/admin/users/new`                    | `AdminUserNew`        | `admin/users/UserNew.vue`         | `user:create`           |
| `/admin/users/:id`                    | `AdminUserDetail`     | `admin/users/UserDetail.vue`      | `user:read`             |
| `/admin/admins`                       | `AdminList`           | `admin/users/AdminList.vue`       | `user:read`（仅 super） |
| `/admin/projects`                     | `AdminProjects`       | `admin/content/ProjectList.vue`   | `project:moderate`      |
| `/admin/personal-cards`               | `AdminPersonalCards`  | `admin/content/PersonalCardList.vue` | `personal:moderate`  |
| `/admin/forum/posts`                  | `AdminForumPosts`     | `admin/content/ForumPostList.vue` | `forum:moderate`        |
| `/admin/forum/comments`               | `AdminForumComments`  | `admin/content/ForumCommentList.vue` | `forum:comment_moderate` |
| `/admin/messages`                     | `AdminMessages`       | `admin/content/MessageInspect.vue`| `message:audit`         |
| `/admin/competitions`                 | `AdminCompetitions`   | `admin/competitions/CompetitionList.vue` | `competition:write` |
| `/admin/competitions/:id/edit`        | `AdminCompetitionEdit`| `admin/competitions/CompetitionForm.vue` | `competition:write` |
| `/admin/news`                         | `AdminNews`           | `admin/competitions/NewsList.vue` | `news:write`            |
| `/admin/news/:id/edit`                | `AdminNewsEdit`       | `admin/competitions/NewsForm.vue` | `news:write`            |
| `/admin/reports`                      | `AdminReports`        | `admin/moderation/ReportList.vue` | `report:handle`         |
| `/admin/keywords`                     | `AdminKeywords`       | `admin/moderation/KeywordList.vue`| `keyword:write`         |
| `/admin/audit-logs`                   | `AdminAuditLogs`      | `admin/moderation/AuditLogList.vue` | `audit:read`          |
| `/admin/roles`                        | `AdminRoles`          | `admin/system/RoleTemplateList.vue` | `role:write`          |
| `/admin/settings`                     | `AdminSettings`       | `admin/system/SystemSettings.vue` | `system:write`          |
| `/admin/profile`                      | `AdminProfile`        | `admin/system/Profile.vue`        | （登录即可）             |

**路由守卫流程：**

1. 进入 `/admin/**`：未登录 → `/admin/login?redirect=...`
2. 已登录但 `role∉{admin,super_admin}` → `/403`
3. 已登录但缺少目标路由 `meta.perm` → `/403`
4. 进入 `/admin/login` 时若已是 admin 登录 → 跳 `/admin/dashboard`

### 10.4 通用 UX 约定

| 场景               | 约定                                                                                          |
|--------------------|-----------------------------------------------------------------------------------------------|
| 列表页             | 顶部筛选栏（搜索 / 下拉 / 日期范围） + 表格 + 分页 + 右上角批量操作；筛选条件写 URL query     |
| 高危操作           | `<ConfirmDialog>` 弹窗，必须填 `reason`（≥ 5 字）+ 二次输入密码；提交后 toast"已记录到审计"    |
| 状态颜色           | active → 绿；disabled → 灰；banned → 红；pending → 橙                                          |
| 隐藏 / 软删除      | 视觉上保留行，但行尾加 `<el-tag type="info">已隐藏</el-tag>`，提供"恢复"按钮                 |
| 搜索去抖           | 300ms debounce，避免每次按键触发请求                                                          |
| 无权限按钮         | 用 `v-perm="'forum:moderate'"` 指令直接 `display:none`，**不要**只 disable                     |
| 表格列冻结         | "操作"列右侧冻结；用户列左侧冻结                                                              |
| 文本截断           | `el-tooltip` 包裹长文本，鼠标悬浮显示完整                                                     |
| 数据空态           | 统一 `<EmptyState>` 组件，提供"刷新"或"创建"快捷                                                |
| 错误兜底           | 接口错误用 `<ElNotification>` 而非 `ElMessage`，便于查看堆栈 / 复制 trace_id                  |

### 10.5 关键页面交互细节

#### 10.5.1 仪表盘 `/admin/dashboard`

布局：

```
┌──────────────────────────────────────────────────────────────────┐
│  KPI 卡片 4 列：DAU、本日新增用户、新项目卡、待处理举报          │
├──────────────────────────────────────────────────────────────────┤
│   折线图：近 30 日 DAU / 私信数             │  饼图：比赛分布     │
│                                              │                   │
├──────────────────────────────────────────────┴───────────────────┤
│  待办列表：举报队列 Top 5 / 关键词命中待审 / 弱密码账户          │
└──────────────────────────────────────────────────────────────────┘
```

技术：ECharts；数据来源 `GET /api/v1/admin/dashboard?range=30d`，60 秒前端刷新。

#### 10.5.2 用户列表 `/admin/users`

- 顶部 `ResourceTable` 通用组件：搜索关键字 + 角色 / 状态 / 来源 / 院系 / 注册时间下拉
- 行操作：详情（抽屉）/ 编辑（抽屉）/ 启停 / 重置密码 / 改角色（仅有 `user:write` 显示）/ 封禁（仅有 `user:write`）/ 删除（仅 super_admin）
- 改角色弹窗：选择 role + role_template + 在 `<PermSelector>` 上微调权限码
- 批量动作：批量启停 / 批量重置密码（生成 reset 链接列表 → 下载 CSV）
- "新增账号"按钮 → 路由 `/admin/users/new`

#### 10.5.3 新增账号 `/admin/users/new`

`<el-steps>` 三步式表单：

| Step          | 字段                                              | 校验                                                                  |
|---------------|---------------------------------------------------|-----------------------------------------------------------------------|
| 1. 基本信息   | email / name / display_name / dept_code / grade   | role=student/teacher 默认要求 `@ruc.edu.cn`，可勾选"豁免"（admin only）|
| 2. 角色与权限 | role + role_template + PermSelector（admin 时）   | 创建 admin 需 `user:create_admin`；不能授予自己没有的权限码            |
| 3. 激活方式   | radio：发送一次性激活链接 / 直接设置初始密码      | set_password 时校验密码强度；发送链接时确认邮箱                       |

末尾"提交并继续创建" / "提交并查看用户"双按钮。

#### 10.5.4 内容治理通用页面（项目卡 / 个人卡 / 论坛帖子 / 评论）

统一用 `<ResourceTable>`：

- 列：内容预览（点击进入原页面，加 `?admin=1` 查看作者真实信息）/ 作者 / 状态 / 关联实体 / 创建时间 / 操作
- 操作：隐藏 / 删除 / 恢复；论坛额外有 置顶 / 加精
- 任意操作 → 弹 `ConfirmDialog`，必填 reason，写 AuditLog

#### 10.5.5 私信巡查 `/admin/messages`

- 默认空列表 + 提示"仅展示已被举报的会话上下文"
- 入口来自举报队列"查看原文"
- 展示窗口为该消息前后 ± 10 条；右侧固定显示"举报理由 / 举报人"
- **不允许搜索任意会话**，前端干脆不渲染搜索框
- 进入页面即调 `POST /api/v1/admin/audit-events { event=admin_view_messages }`

#### 10.5.6 举报队列 `/admin/reports`

- Tab：待处理 / 已处理 / 已驳回
- 表格行展开：显示举报内容预览、举报人摘要、举报理由
- "处理"按钮 → 弹窗选择动作（hide / delete / warn / ban），ban 时填到期时间
- 多人协作：行加"分配给我"按钮；同一举报被分配后其他人显示锁图标

#### 10.5.7 敏感词管理 `/admin/keywords`

- 表格：keyword / match_type / action / scope（多 tag） / enabled / hit_count / 创建时间
- 顶部"新增规则"按钮：弹窗表单
- 行尾"测试"按钮：弹窗输入示例文本，前端调 `POST /api/v1/admin/keywords/test` 高亮命中片段

#### 10.5.8 审计日志 `/admin/audit-logs`

- 顶部筛选：操作人 / 动作 / 目标类型 / 时间范围
- 表格：时间 / 操作人 / 动作 / 目标 / 摘要 / IP
- 行展开：`<AuditViewer>` 显示 before / after JSON 差异（diff 高亮）
- 顶部 "导出 CSV"（异步任务）

#### 10.5.9 角色与权限 `/admin/roles`

- 表格：模板名 / 描述 / 权限数 / 是否内置 / 引用人数 / 操作
- 编辑弹窗：`<PermSelector>` 按模块分组的复选框矩阵；保存后弹"将影响 N 名管理员，是否同步更新？"

#### 10.5.10 系统设置 `/admin/settings`

- 左侧锚点导航：认证 / 私信 / 论坛 / 通知 / AI / 高级
- 右侧表单：每项配置带 description tooltip 与 `<el-switch>` / `<el-input>` 组件
- 顶部"保存所有"按钮带 diff 预览

### 10.6 权限指令与组件

新增全局指令 `v-perm`：

| 用法                              | 行为                                          |
|-----------------------------------|-----------------------------------------------|
| `v-perm="'user:write'"`           | 当前用户无该权限码 → 移除 DOM                 |
| `v-perm:any="['x', 'y']"`         | 任一权限码满足即可                            |
| `v-perm:all="['x', 'y']"`         | 必须全部满足                                  |

`hasPerm()` 在 `permStore` 里实现，路由守卫与组件复用同一个判断函数。

### 10.7 安全加固（前端层）

| 措施                                   | 说明                                                                       |
|----------------------------------------|----------------------------------------------------------------------------|
| 后台子站点禁用浏览器密码自动填充       | `autocomplete="new-password"`                                              |
| 退出登录立即清空 localStorage / store  | 通过 `pinia.reset()` + 调用 `/admin/auth/logout` 让 refresh token 加黑名单 |
| Idle 超时                              | 30 分钟无操作自动登出（监听 mousemove / keydown，倒计时弹窗提示）          |
| 防截图 / 防复制（敏感页面）             | 用户列表 / 私信巡查页禁用右键、复制（仅前端弱保护，主要靠后端审计）        |
| 错误页统一                             | 401 → 跳登录；403 → `/admin/403`；5xx → 兜底 `<ErrorPage>`                  |
| 操作埋点                               | 每次列表查询 / 写操作上报 `trace_id` + `action`，便于客服排障               |

### 10.8 改造工作量估算

| 模块                      | 工作量（小时）                |
|---------------------------|-------------------------------|
| 后台基础布局 / 路由 / 守卫 / 权限指令 | 4                       |
| 用户管理（列表 / 详情 / 新增 / 编辑 / 启停） | 6                |
| 内容治理（4 页通用表格） | 4                              |
| 比赛 + 资讯 CRUD          | 4                              |
| 举报 / 敏感词 / 审计日志  | 6                              |
| 角色权限 / 系统配置       | 4                              |
| 仪表盘 + ECharts          | 4                              |
| 安全细节（idle / captcha / confirmation） | 3                |
| **合计**                   | **≈ 35h（约 4 个半天）**         |

---

> **总结**：管理后台与 C 端**逻辑共用、视觉独立**；目录、路由、组件、权限四件套对齐 `design.md` §6.4；高危操作均通过 `ConfirmDialog` + `reason` 强制审计。完成本节后，整个 KAL 平台的产品线（C 端 9 页 + 后台 22 页）即可闭环上线。
