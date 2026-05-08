<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const menus = [
  { key: 'tickets', label: '工单工作台', icon: 'message',  to: '/staff/tickets' },
]

const breadcrumb = computed(() => menus.find(m => route.path.startsWith(m.to))?.label || '')

function logout () {
  auth.logout()
  router.replace('/admin/login')
}
</script>

<template>
  <div class="kst">
    <aside class="kst-side">
      <div class="kst-brand">
        <span class="kst-mark">KAL</span>
        <div>
          <div class="kst-brand-title">工作台</div>
          <div class="kst-brand-sub">STAFF&nbsp;CONSOLE</div>
        </div>
      </div>
      <nav class="kst-menu">
        <RouterLink
          v-for="m in menus" :key="m.key"
          :to="m.to" class="kst-link" active-class="is-active"
        >
          <Icon :name="m.icon" :size="16" />
          <span>{{ m.label }}</span>
        </RouterLink>
        <RouterLink
          v-if="auth.isAdmin"
          to="/admin"
          class="kst-link kst-link-admin"
        >
          <Icon name="shield" :size="15" />
          <span>切换至管理后台</span>
        </RouterLink>
      </nav>
      <div class="kst-side-foot">
        <div class="kst-me">
          <div class="kst-me-name">{{ auth.me?.displayName || auth.me?.name || '工作人员' }}</div>
          <div class="kst-me-role">
            {{ auth.me?.role === 'super_admin' ? '超级管理员' :
               auth.me?.role === 'admin'       ? '管理员'      :
               auth.me?.role === 'staff'       ? '工作人员'    : '—' }}
          </div>
        </div>
        <button class="kal-btn kal-btn-secondary kal-btn-sm kst-logout" @click="logout">
          <Icon name="arrow-right" :size="13" />
          <span>退出</span>
        </button>
      </div>
    </aside>

    <main class="kst-main">
      <header class="kst-top">
        <div class="kst-bread">
          <span>知行创坊 · 工作人员</span>
          <span class="kst-bread-sep">/</span>
          <strong>{{ breadcrumb }}</strong>
        </div>
        <a href="/" class="kst-back" target="_blank" rel="noopener">
          <Icon name="arrow-right" :size="13" />
          <span>前台首页</span>
        </a>
      </header>
      <section class="kst-body">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.kst { display: flex; min-height: 100vh; background: var(--kal-bg); }
.kst-side {
  width: 240px; flex-shrink: 0;
  background: var(--kal-ink); color: #fff;
  display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh;
}
.kst-brand { display: flex; align-items: center; gap: 10px; padding: 28px 24px 22px; border-bottom: 1px solid rgba(255,255,255,0.08); }
.kst-mark {
  width: 36px; height: 36px;
  background: rgba(255,255,255,0.94); color: var(--kal-ink);
  border-radius: var(--kal-radius-sm);
  display: inline-flex; align-items: center; justify-content: center;
  font-family: var(--kal-font-serif); font-weight: 700; font-size: 12px; letter-spacing: 2px;
}
.kst-brand-title { font-family: var(--kal-font-serif); font-weight: 600; font-size: 14px; letter-spacing: 3px; }
.kst-brand-sub { font-size: 9.5px; letter-spacing: 2.5px; opacity: 0.5; margin-top: 3px; }

.kst-menu { padding: 16px 12px; display: flex; flex-direction: column; gap: 2px; flex: 1; }
.kst-link {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; color: rgba(255,255,255,0.7);
  border-radius: var(--kal-radius-sm); font-size: 13.5px; letter-spacing: 0.5px;
  transition: background .15s, color .15s;
}
.kst-link:hover { background: rgba(255,255,255,0.06); color: #fff; }
.kst-link.is-active { background: rgba(255,255,255,0.12); color: #fff; font-weight: 500; }
.kst-link-admin {
  margin-top: auto;
  background: rgba(255,255,255,0.04);
  border: 1px dashed rgba(255,255,255,0.2);
  font-size: 12.5px;
}

.kst-side-foot { padding: 16px; border-top: 1px solid rgba(255,255,255,0.08); display: flex; flex-direction: column; gap: 10px; }
.kst-me-name { font-size: 13px; font-weight: 500; }
.kst-me-role { font-size: 11px; letter-spacing: 1px; opacity: 0.5; margin-top: 2px; }
.kst-logout {
  background: rgba(255,255,255,0.08) !important;
  border-color: rgba(255,255,255,0.14) !important;
  color: rgba(255,255,255,0.85) !important;
  justify-content: center;
}
.kst-logout:hover { background: rgba(255,255,255,0.16) !important; color: #fff !important; }

.kst-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.kst-top {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 32px;
  background: var(--kal-surface);
  border-bottom: 1px solid var(--kal-border);
}
.kst-bread { font-size: 13px; color: var(--kal-text-muted); letter-spacing: 0.5px; }
.kst-bread strong { color: var(--kal-text-strong); font-weight: 600; }
.kst-bread-sep { margin: 0 8px; color: var(--kal-text-subtle); }
.kst-back { font-size: 12px; color: var(--kal-text-muted); display: inline-flex; align-items: center; gap: 4px; }
.kst-back:hover { color: var(--kal-primary-700); }

.kst-body { flex: 1; padding: 28px 32px; overflow: auto; }

@media (max-width: 900px) {
  .kst { flex-direction: column; }
  .kst-side { width: 100%; height: auto; position: static; flex-direction: row; align-items: center; padding: 0 8px; overflow-x: auto; }
  .kst-brand { border: 0; padding: 14px; }
  .kst-menu { flex-direction: row; flex: 1; padding: 8px; }
  .kst-side-foot { border: 0; padding: 8px 14px; }
  .kst-link span { display: none; }
}
</style>
