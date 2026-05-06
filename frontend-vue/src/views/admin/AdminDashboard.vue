<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import Icon from '@/components/Icon.vue'

const stats = ref(null)
const loading = ref(true)
const error = ref('')

async function load () {
  loading.value = true
  try { stats.value = await adminApi.stats() }
  catch (e) { error.value = e.message }
  finally   { loading.value = false }
}
onMounted(load)

const cards = [
  { key: 'totalUsers',         label: '总账号数',     extra: 'activeUsers',          extraLabel: '其中活跃', icon: 'user' },
  { key: 'totalProjects',      label: '项目卡总数',   extra: 'recruitingProjects',   extraLabel: '招募中',   icon: 'document' },
  { key: 'totalForumPosts',    label: '论坛帖子',     extra: 'publishedForumPosts',  extraLabel: '已发布',   icon: 'message' },
  { key: 'totalCompetitions',  label: '比赛信息',     extra: 'activeCompetitions',   extraLabel: '进行中',   icon: 'trophy' },
]
</script>

<template>
  <div class="adb">
    <header class="adb-head">
      <div class="kal-eyebrow">Console / Overview</div>
      <h1 class="adb-title">仪表盘</h1>
      <p class="adb-sub">数据由后端实时统计；H2 内存数据库每次重启会重置为种子数据。</p>
    </header>

    <section v-if="loading" class="adb-loading">数据加载中…</section>
    <section v-else-if="error" class="adb-error">{{ error }}</section>
    <section v-else class="adb-grid">
      <article v-for="c in cards" :key="c.key" class="adb-card">
        <div class="adb-card-icon"><Icon :name="c.icon" :size="18" /></div>
        <div class="adb-card-num">{{ stats?.[c.key] ?? 0 }}</div>
        <div class="adb-card-label">{{ c.label }}</div>
        <div class="adb-card-extra">
          {{ c.extraLabel }} · <strong>{{ stats?.[c.extra] ?? 0 }}</strong>
        </div>
      </article>
    </section>

    <section class="adb-tips">
      <h3>快捷操作</h3>
      <ul>
        <li><RouterLink to="/admin/users">→ 进入账号管理（手动添加 / 封禁 / 重置密码）</RouterLink></li>
        <li><RouterLink to="/admin/projects">→ 进入项目卡治理（隐藏 / 删除 / 恢复）</RouterLink></li>
        <li><RouterLink to="/admin/forum">→ 进入论坛治理（置顶 / 隐藏 / 删除）</RouterLink></li>
        <li><RouterLink to="/admin/competitions">→ 维护比赛信息（新增 / 编辑 / 下架）</RouterLink></li>
        <li><RouterLink to="/admin/audit">→ 查看审计日志</RouterLink></li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.adb-head { margin-bottom: 28px; }
.adb-title { font-family: var(--kal-font-serif); font-size: 32px; letter-spacing: 4px; color: var(--kal-text-strong); margin: 8px 0 6px; font-weight: 600; }
.adb-sub { color: var(--kal-text-muted); font-size: 13px; }

.adb-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }
.adb-card {
  position: relative;
  background: var(--kal-surface); border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md); padding: 22px 22px 18px;
  transition: transform .2s, border-color .2s;
}
.adb-card:hover { border-color: var(--kal-primary-200); transform: translateY(-2px); }
.adb-card-icon {
  position: absolute; top: 18px; right: 18px;
  width: 32px; height: 32px;
  background: var(--kal-primary-50); color: var(--kal-primary-700);
  border-radius: var(--kal-radius-sm);
  display: inline-flex; align-items: center; justify-content: center;
}
.adb-card-num { font-family: var(--kal-font-serif); font-size: 36px; font-weight: 600; color: var(--kal-text-strong); letter-spacing: 1px; }
.adb-card-label { font-size: 12px; color: var(--kal-text-muted); letter-spacing: 1px; margin-top: 6px; }
.adb-card-extra { font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 0.5px; margin-top: 12px; padding-top: 10px; border-top: 1px dashed var(--kal-divider); }
.adb-card-extra strong { color: var(--kal-primary-700); font-weight: 600; }

.adb-tips { background: var(--kal-surface); border: 1px solid var(--kal-border); border-radius: var(--kal-radius-md); padding: 24px; margin-top: 28px; }
.adb-tips h3 { font-family: var(--kal-font-serif); font-size: 16px; letter-spacing: 2px; margin-bottom: 14px; color: var(--kal-text-strong); }
.adb-tips ul { display: flex; flex-direction: column; gap: 8px; padding-left: 0; list-style: none; }
.adb-tips a { font-size: 13.5px; color: var(--kal-text-muted); }
.adb-tips a:hover { color: var(--kal-primary-700); }

.adb-loading, .adb-error { padding: 40px; text-align: center; color: var(--kal-text-muted); }
.adb-error { color: var(--kal-primary-700); }
</style>
