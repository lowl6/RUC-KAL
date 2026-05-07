import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  /* ========== 用户端 ========== */
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { layout: 'plain', title: '登录' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/projects/new',
    name: 'PublishProject',
    component: () => import('@/views/PublishProject.vue'),
    meta: { title: '发布项目卡', requireAuth: true }
  },
  {
    path: '/projects/:id',
    name: 'ProjectDetail',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { title: '项目详情' }
  },
  {
    path: '/personal-cards/edit',
    name: 'PublishProfile',
    component: () => import('@/views/PublishProfile.vue'),
    meta: { title: '我的个人卡', requireAuth: true }
  },
  {
    path: '/competitions',
    name: 'Competitions',
    component: () => import('@/views/Competitions.vue'),
    meta: { title: '比赛中心' }
  },
  {
    path: '/competitions/:id',
    name: 'competition-detail',
    component: () => import('@/views/CompetitionDetail.vue'),
    meta: { title: '比赛详情' }
  },
  {
    path: '/news/:id',
    name: 'news-detail',
    component: () => import('@/views/NewsDetail.vue'),
    meta: { title: '资讯详情' }
  },
  {
    path: '/forum',
    name: 'Forum',
    component: () => import('@/views/Forum.vue'),
    meta: { title: '创坊论坛' }
  },
  {
    path: '/forum/:id',
    name: 'forum-post-detail',
    component: () => import('@/views/ForumPostDetail.vue'),
    meta: { title: '帖子详情' }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/Messages.vue'),
    meta: { title: '私信', requireAuth: true }
  },
  {
    path: '/me',
    name: 'MyCenter',
    component: () => import('@/views/MyCenter.vue'),
    meta: { title: '我的中心', requireAuth: true }
  },

  /* ========== 通用静态页 ========== */
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/static/AboutKal.vue'),
    meta: { title: '关于' }
  },
  {
    path: '/help',
    name: 'HelpGuide',
    component: () => import('@/views/static/HelpGuide.vue'),
    meta: { title: '使用指南' }
  },
  {
    path: '/privacy',
    name: 'Privacy',
    component: () => import('@/views/static/Privacy.vue'),
    meta: { title: '隐私协议' }
  },
  {
    path: '/contact',
    name: 'Contact',
    component: () => import('@/views/static/Contact.vue'),
    meta: { title: '联系我们' }
  },

  /* ========== 管理员端 ========== */
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { layout: 'plain', title: '管理后台登录' }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requireAdmin: true },
    children: [
      { path: '', redirect: '/admin/competitions' },
      { path: 'dashboard',    name: 'AdminDashboard',    component: () => import('@/views/admin/AdminDashboard.vue'),    meta: { title: '仪表盘' } },
      { path: 'competitions', name: 'AdminCompetitions', component: () => import('@/views/admin/AdminCompetitions.vue'), meta: { title: '赛事发布' } },
      { path: 'news',         name: 'AdminNews',         component: () => import('@/views/admin/AdminNews.vue'),         meta: { title: '资讯发布' } },
      { path: 'forum',        name: 'AdminForum',        component: () => import('@/views/admin/AdminForum.vue'),        meta: { title: '论坛治理' } },
      { path: 'projects',     name: 'AdminProjects',     component: () => import('@/views/admin/AdminProjects.vue'),     meta: { title: '项目卡治理' } },
      { path: 'users',        name: 'AdminUsers',        component: () => import('@/views/admin/AdminUsers.vue'),        meta: { title: '账号管理', requireSuperAdmin: true } },
      { path: 'audit',        name: 'AdminAudit',        component: () => import('@/views/admin/AdminAudit.vue'),        meta: { title: '审计日志' } },
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    return saved || { top: 0, behavior: 'smooth' }
  }
})

const publicRouteNames = new Set(['Login', 'AdminLogin'])

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn && !publicRouteNames.has(to.name)) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  if (to.meta?.requireAdmin) {
    if (!auth.isLoggedIn) return next({ path: '/admin/login', query: { redirect: to.fullPath } })
    if (!auth.isAdmin)    return next({ path: '/admin/login', query: { redirect: to.fullPath } })
    if (to.meta?.requireSuperAdmin && !auth.isSuperAdmin) return next('/admin/dashboard')
    return next()
  }
  if (to.meta?.requireAuth && !auth.isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  next()
})

router.afterEach((to) => {
  const base = '知行创坊 KNOWACT Lab'
  document.title = to.meta?.title ? `${to.meta.title} · ${base}` : base
})

export default router
