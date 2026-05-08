<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'

const router = useRouter()
const route  = useRoute()
const auth = useAuthStore()

const menus = [
  { key: 'competitions', label: '赛事发布',   icon: 'trophy',   to: '/admin/competitions' },
  { key: 'news',         label: '资讯发布',   icon: 'edit',     to: '/admin/news' },
  { key: 'dashboard',    label: '运营总览',   icon: 'compass',  to: '/admin/dashboard' },
  { key: 'tickets',      label: '工单工作台', icon: 'message',  to: '/staff/tickets' },
  { key: 'forum',        label: '论坛治理',   icon: 'message',  to: '/admin/forum' },
  { key: 'projects',     label: '项目卡治理', icon: 'document', to: '/admin/projects' },
  { key: 'users',        label: '账号管理',   icon: 'user',     to: '/admin/users', superOnly: true },
  { key: 'audit',        label: '审计日志',   icon: 'shield',   to: '/admin/audit' },
]

const visibleMenus = computed(() => menus.filter(m => !m.superOnly || auth.isSuperAdmin))

const breadcrumb = computed(() => {
  const m = menus.find(x => route.path.startsWith(x.to))
  return m?.label || ''
})

function logout () {
  auth.logout()
  router.replace('/admin/login')
}
</script>

<template>
  <div class="ad">
    <aside class="ad-side">
      <div class="ad-brand">
        <span class="ad-mark">KAL</span>
        <div>
          <div class="ad-brand-title">管理后台</div>
          <div class="ad-brand-sub">ADMIN CONSOLE</div>
        </div>
      </div>
      <nav class="ad-menu">
        <RouterLink
          v-for="m in visibleMenus" :key="m.key"
          :to="m.to" class="ad-link" active-class="is-active"
        >
          <Icon :name="m.icon" :size="16" />
          <span>{{ m.label }}</span>
        </RouterLink>
      </nav>
      <div class="ad-side-foot">
        <div class="ad-me">
          <div class="ad-me-name">{{ auth.me?.displayName || auth.me?.name || '管理员' }}</div>
          <div class="ad-me-role">{{ auth.me?.role === 'super_admin' ? '超级管理员' : '管理员' }}</div>
        </div>
        <button class="kal-btn kal-btn-secondary kal-btn-sm ad-logout" @click="logout">
          <Icon name="arrow-right" :size="13" />
          <span>退出</span>
        </button>
      </div>
    </aside>

    <main class="ad-main">
      <header class="ad-top">
        <div class="ad-bread">
          <span>管理后台</span>
          <span class="ad-bread-sep">/</span>
          <strong>{{ breadcrumb }}</strong>
        </div>
        <div class="ad-actions">
          <a href="/" class="ad-back" target="_blank" rel="noopener">
            <Icon name="arrow-right" :size="13" /> 前台首页
          </a>
        </div>
      </header>
      <section class="ad-body">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.ad { display: flex; min-height: 100vh; background: var(--kal-bg); }
.ad-side {
  width: 240px; flex-shrink: 0;
  background: var(--kal-ink); color: #fff;
  display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh;
}
.ad-brand { display: flex; align-items: center; gap: 10px; padding: 28px 24px 22px; border-bottom: 1px solid rgba(255,255,255,0.08); }
.ad-mark {
  width: 36px; height: 36px;
  background: rgba(255,255,255,0.94); color: var(--kal-ink);
  border-radius: var(--kal-radius-sm);
  display: inline-flex; align-items: center; justify-content: center;
  font-family: var(--kal-font-serif); font-weight: 700; font-size: 12px; letter-spacing: 2px;
}
.ad-brand-title { font-family: var(--kal-font-serif); font-weight: 600; font-size: 14px; letter-spacing: 3px; }
.ad-brand-sub { font-size: 9.5px; letter-spacing: 2.5px; opacity: 0.5; margin-top: 3px; }

.ad-menu { padding: 16px 12px; display: flex; flex-direction: column; gap: 2px; flex: 1; }
.ad-link {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; color: rgba(255,255,255,0.7);
  border-radius: var(--kal-radius-sm); font-size: 13.5px; letter-spacing: 0.5px;
  transition: background .15s, color .15s;
}
.ad-link:hover { background: rgba(255,255,255,0.06); color: #fff; }
.ad-link.is-active { background: rgba(255,255,255,0.12); color: #fff; font-weight: 500; }

.ad-side-foot { padding: 16px; border-top: 1px solid rgba(255,255,255,0.08); display: flex; flex-direction: column; gap: 10px; }
.ad-me-name { font-size: 13px; font-weight: 500; }
.ad-me-role { font-size: 11px; letter-spacing: 1px; opacity: 0.5; margin-top: 2px; }
.ad-logout {
  background: rgba(255,255,255,0.08) !important;
  border-color: rgba(255,255,255,0.14) !important;
  color: rgba(255,255,255,0.85) !important;
  justify-content: center;
}
.ad-logout:hover { background: rgba(255,255,255,0.16) !important; color: #fff !important; }

.ad-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.ad-top {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 32px;
  background: var(--kal-surface);
  border-bottom: 1px solid var(--kal-border);
}
.ad-bread { font-size: 13px; color: var(--kal-text-muted); letter-spacing: 0.5px; }
.ad-bread strong { color: var(--kal-text-strong); font-weight: 600; }
.ad-bread-sep { margin: 0 8px; color: var(--kal-text-subtle); }
.ad-actions a { font-size: 12px; color: var(--kal-text-muted); display: inline-flex; align-items: center; gap: 4px; }
.ad-actions a:hover { color: var(--kal-primary-700); }

.ad-body { flex: 1; padding: 28px 32px; overflow: auto; }

@media (max-width: 900px) {
  .ad { flex-direction: column; }
  .ad-side { width: 100%; height: auto; position: static; flex-direction: row; align-items: center; padding: 0 8px; overflow-x: auto; }
  .ad-brand { border: 0; padding: 14px; }
  .ad-menu { flex-direction: row; flex: 1; padding: 8px; }
  .ad-side-foot { border: 0; padding: 8px 14px; }
  .ad-link span { display: none; }
}
</style>
