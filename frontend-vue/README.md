# 知行创坊 KAL · Vue 3 前端

> 完整的、可独立运行的 Vue 3 前端展示项目（mock 数据，无后端依赖）。
> 设计语言统一、字体优雅、组件可复用，可直接作为后续接入真实 API 的基础。

mvn -q -DskipTests -o package 2>&1 | Select-Object -Last 30

cd backend; java -jar target\kal-backend-app.jar
cd frontend-vue; npm run dev

$pids = (netstat -ano | Select-String ':8080\s+.*LISTENING' | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique); if ($pids) { $pids | ForEach-Object { Stop-Process -Id ([int]$_) -Force; Write-Host "stopped backend pid $_" } } else { Write-Host 'no backend on 8080' }

超级管理员：admin@ruc.edu.cn / Admin@2026
普通管理员：ops@ruc.edu.cn / Admin@2026
学生 / 教师：li.yan@ruc.edu.cn 等 / Kal@2026

超级管理员
kal-superadmin@ruc.edu.cn
Kal#Super-2026@Admin
运营管理员
kal-ops@ruc.edu.cn
Kal#Ops-2026@Console

## 技术栈

| 维度        | 选型                                                |
|-------------|-----------------------------------------------------|
| 构建        | **Vite 6**                                          |
| 框架        | **Vue 3** + Composition API + `<script setup>`      |
| 路由        | **Vue Router 4**                                    |
| 状态管理    | **Pinia 2**                                         |
| UI 组件     | **手写组件 + 设计令牌**（无 UI 框架，可控可改）     |
| 字体        | Inter（英文 / 数字） + Noto Serif SC（中文标题）    |
| 图标        | 内联 SVG（lucide / heroicons 风格）                 |
| 动画        | Vue `<Transition>` + 自定义 cubic-bezier            |

## 快速开始

```bash
cd frontend-vue

# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 生产构建
npm run build

# 预览生产构建
npm run preview
```

启动后浏览器自动打开 `http://localhost:5173`，会看到登录页：

1. **姓名**任意填，**邮箱**必须以 `@ruc.edu.cn` 结尾（前端校验，无后端验证）；
2. 登录后进入首页，可逐页查看：找项目 / 找队友 / 发布项目卡 / 发布个人卡 / 项目详情 / 比赛中心 / 创坊论坛 / 私信 / 我的中心。

## 项目结构

```
frontend-vue/
├── index.html                  # SPA 入口
├── package.json
├── vite.config.js
├── jsconfig.json
├── public/
│   └── favicon.svg
└── src/
    ├── main.js                 # 应用启动
    ├── App.vue                 # 根组件
    ├── router/index.js         # 路由
    ├── stores/user.js          # 用户 store（Pinia）
    ├── styles/
    │   ├── tokens.css          # 设计令牌（颜色 / 字号 / 间距 / 阴影 / 动效）
    │   └── global.css          # reset + 全局组件类
    ├── mock/index.js           # 全部 mock 数据
    ├── components/
    │   ├── layout/
    │   │   ├── AppHeader.vue   # 顶部导航 + 发布菜单 + 用户菜单
    │   │   └── AppFooter.vue
    │   ├── ProjectCard.vue     # 项目卡组件
    │   └── PersonalCard.vue    # 个人卡组件
    └── views/
        ├── Login.vue           # 登录 / 注册
        ├── Home.vue            # 首页（找项目 / 找队友双 Tab）
        ├── PublishProject.vue  # 发布项目卡
        ├── PublishProfile.vue  # 发布 / 编辑个人卡（带实时预览）
        ├── ProjectDetail.vue   # 项目详情
        ├── Competitions.vue    # 比赛中心 + 资讯
        ├── Forum.vue           # 创坊论坛
        ├── Messages.vue        # 私信（双面板）
        └── MyCenter.vue        # 我的中心（项目卡 / 个人卡 / 设置）
```

## 设计系统

所有颜色、字号、阴影、动效都在 [`src/styles/tokens.css`](src/styles/tokens.css) 中定义，命名规范 `--kal-{category}-{step}`。修改一处全站生效。

**核心调色板：**

| Token              | 颜色      | 用途                       |
|--------------------|-----------|----------------------------|
| `--kal-primary-600`| `#861a12` | 主品牌色（人大红）         |
| `--kal-primary-50` | `#fdf3f1` | 浅色背景                   |
| `--kal-gold`       | `#c8a26d` | 暖金，强调点缀（如技能标签）|
| `--kal-bg`         | `#f8f6f4` | 全站底色（暖灰）           |
| `--kal-surface`    | `#ffffff` | 卡片表面                   |

**字体策略：**

- **正文 / UI**：Inter + PingFang SC 系统栈，紧凑现代；
- **标题 / 数字**：Noto Serif SC，中文宋体感，体现"人大"学院气质。

**通用组件类（在 `global.css`）：**
`.kal-card` `.kal-btn` `.kal-input` `.kal-select` `.kal-textarea` `.kal-tag` `.kal-avatar` `.kal-empty` `.kal-skeleton` `.kal-container` 等，所有页面统一调用，避免重复定义。

## 可见的"美学"细节

- **顶部导航毛玻璃**：`backdrop-filter: blur(14px)`，滚动时与内容融合
- **Hero 区背景**：`radial-gradient` 双层叠加，模拟暖光晕
- **品牌色渐变文字**：`-webkit-background-clip: text` 实现红→金渐变
- **悬停态**：所有卡片 `translateY(-2px)` + 阴影上扬
- **统一焦点环**：`:focus-visible` 主色光晕，无障碍友好
- **过渡动画**：路由切换 / 弹窗 / 抽屉用统一 `cubic-bezier(0.22, 1, 0.36, 1)`
- **Mock "立体卡片"装饰**：首页右侧三张飘浮卡，营造产品调性
- **个人卡实时预览**：右侧卡片随表单输入实时变化

## 与后端对接

当前所有数据来自 [`src/mock/index.js`](src/mock/index.js)。**未来接入真实后端**只需：

1. 新建 `src/api/` 目录，对每个模块封装 axios 请求；
2. 在每个 view 中把 `import { ... } from '@/mock'` 替换为对应 API 调用；
3. 由于 store / 组件 / 表单结构与字段名严格对齐 [`../ReadeMe.md` 模块字段](../ReadeMe.md) 与 [`../docs/er-diagram.puml`](../docs/er-diagram.puml)，无需改 UI。

## 浏览器兼容

- 现代浏览器（Chrome/Edge ≥ 110、Safari ≥ 16、Firefox ≥ 110）
- 使用了 `backdrop-filter`、`color-mix`、CSS `inset` 等较新特性，不支持 IE

## 后续计划

- [ ] 接入真实 API（参考 `docs/design.md` §5 的接口定义）
- [ ] 单独打包管理后台（参考 `docs/html-modifications.md` §10）
- [ ] 加入 Element Plus 用于复杂场景（如富文本编辑、ECharts 看板）
- [ ] 对接 WebSocket 实现真实私信
- [ ] 移动端体验抛光（虚拟列表、骨架屏）

---

> **License**: MIT，仅用于知行创坊 (KAL) 项目展示。
