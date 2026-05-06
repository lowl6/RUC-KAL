<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { projectsApi, messagesApi } from '@/api/projects'
import { normalizeProject } from '@/api/normalize'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const project = ref(normalizeProject({}))
const related = ref([])
const loading = ref(false)
const error = ref('')

const projectTypeLabel = computed(() => {
  const map = { innovation: '创新', creation: '创造', entrepreneurship: '创业' }
  return map[project.value.project_type] || ''
})

async function loadProject() {
  loading.value = true
  error.value = ''
  try {
    project.value = normalizeProject(await projectsApi.detail(route.params.id))
    const list = await projectsApi.list({ page: 1, size: 6 })
    related.value = (list?.items || [])
      .map(normalizeProject)
      .filter(p => p.project_id !== project.value.project_id)
      .slice(0, 3)
  } catch (e) {
    error.value = '项目加载失败：' + (e.message || '请稍后重试')
    project.value = normalizeProject({})
    related.value = []
  } finally {
    loading.value = false
  }
}

async function apply() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!project.value.creator_id) {
    router.push('/messages')
    return
  }
  try {
    const conv = await messagesApi.open(project.value.creator_id)
    await messagesApi.send(conv.conversationId, `你好，我想了解「${project.value.project_name}」的组队信息。`)
    router.push({ path: '/messages', query: { conversation: conv.conversationId } })
  } catch (e) {
    alert(e.message || '无法打开私信，请确认后端已启动。')
  }
}
function back() { router.back() }

watch(() => route.params.id, loadProject)
onMounted(loadProject)
</script>

<template>
  <div class="kal-container kpd">
    <button class="kpd-back" @click="back">
      <Icon name="arrow-left" :size="14" :stroke="1.8" />
      <span>返回</span>
    </button>

    <div v-if="error" class="kal-card kpd-notice">{{ error }}</div>
    <div v-if="loading" class="kal-card kpd-notice">正在读取项目详情…</div>

    <div class="kpd-layout">
      <!-- Main -->
      <main class="kpd-main">
        <!-- Header Card -->
        <section class="kal-card kpd-hero">
          <div class="kpd-hero-meta">
            <span class="kpd-hero-comp">{{ project.competition_short }}</span>
            <span class="kpd-hero-divider"></span>
            <span class="kpd-hero-type">{{ projectTypeLabel }}</span>
            <span class="kpd-hero-divider"></span>
            <span class="kpd-hero-status" :class="{ 'kpd-hero-status--urgent': project.badge === 'urgent' }">
              <span class="kpd-hero-status-dot"></span>
              <span>{{ project.days_left }} 天后截止</span>
            </span>
          </div>
          <h1 class="kpd-title">{{ project.project_name }}</h1>
          <p class="kpd-subtitle">{{ project.one_liner }}</p>
          <div class="kpd-author">
            <span class="kal-avatar">{{ project.creator.initial }}</span>
            <div>
              <div class="kpd-author-name">
                <span>{{ project.creator.name }}</span>
                <span class="kpd-author-role">项目负责人</span>
              </div>
              <div class="kpd-author-dept">{{ project.creator.dept }} · {{ project.creator.grade }}</div>
            </div>
          </div>
        </section>

        <!-- Detail -->
        <section class="kal-card kpd-section">
          <header class="kpd-section-head">
            <span class="kal-eyebrow">Description</span>
            <h2 class="kpd-section-title">
              <span class="kal-serial">i.</span>
              <span>项目详情</span>
            </h2>
          </header>
          <p class="kpd-detail">{{ project.detail }}</p>
        </section>

        <!-- Roles -->
        <section class="kal-card kpd-section">
          <header class="kpd-section-head">
            <span class="kal-eyebrow">Wanted&nbsp;Roles</span>
            <h2 class="kpd-section-title">
              <span class="kal-serial">ii.</span>
              <span>所需人员</span>
            </h2>
          </header>
          <div class="kpd-roles">
            <div v-for="(r, i) in project.roles" :key="i" class="kpd-role">
              <span class="kpd-role-num">{{ String(i + 1).padStart(2, '0') }}</span>
              <div class="kpd-role-body">
                <div class="kpd-role-head">
                  <span class="kpd-role-name">{{ r.role_name }}</span>
                  <span class="kpd-role-count">招募 {{ r.count }} 人</span>
                </div>
                <div v-if="r.skills" class="kpd-role-skills">
                  <span class="kpd-role-label">技能要求</span>
                  <span>{{ r.skills }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Tags -->
        <section class="kal-card kpd-section">
          <header class="kpd-section-head">
            <span class="kal-eyebrow">Keywords</span>
            <h2 class="kpd-section-title">
              <span class="kal-serial">iii.</span>
              <span>技术标签</span>
            </h2>
          </header>
          <div class="kpd-tags">
            <span v-for="t in project.tags" :key="t" class="kpd-tag">{{ t }}</span>
          </div>
        </section>

        <!-- Related -->
        <section v-if="related.length" class="kal-card kpd-section">
          <header class="kpd-section-head">
            <span class="kal-eyebrow">You&nbsp;May&nbsp;Also&nbsp;Like</span>
            <h2 class="kpd-section-title">
              <span class="kal-serial">iv.</span>
              <span>相关项目</span>
            </h2>
          </header>
          <div class="kpd-related">
            <RouterLink
              v-for="r in related"
              :key="r.project_id"
              :to="`/projects/${r.project_id}`"
              class="kpd-related-item"
            >
              <div class="kpd-related-meta">{{ r.competition_short }} · 招募 {{ r.needed_count }} 人</div>
              <div class="kpd-related-name">{{ r.project_name }}</div>
              <Icon class="kpd-related-arrow" name="arrow-right" :size="14" :stroke="1.6" />
            </RouterLink>
          </div>
        </section>
      </main>

      <!-- Aside -->
      <aside class="kpd-aside">
        <div class="kal-card kpd-stats">
          <div class="kal-eyebrow kpd-stats-eyebrow">Project&nbsp;Stats</div>
          <div class="kpd-stats-grid">
            <div>
              <dt>{{ project.current_members }}</dt>
              <dd>已有成员</dd>
            </div>
            <div>
              <dt>{{ project.needed_count }}</dt>
              <dd>招募人数</dd>
            </div>
            <div>
              <dt>{{ project.weekly_hours }}</dt>
              <dd>周投入(h)</dd>
            </div>
            <div>
              <dt>{{ project.view_count }}</dt>
              <dd>浏览次数</dd>
            </div>
          </div>
        </div>

        <div class="kpd-deadline">
          <div class="kpd-deadline-row">
            <Icon name="clock" :size="13" />
            <span class="kpd-deadline-label">距截止</span>
          </div>
          <div class="kpd-deadline-num">
            <strong>{{ project.days_left }}</strong>
            <span>days</span>
          </div>
          <div class="kpd-deadline-tip">建议尽快递交申请并沟通</div>
        </div>

        <button class="kal-btn kal-btn-lg kal-btn-block kpd-apply" @click="apply">
          <Icon name="message" :size="14" />
          <span>申请加入 / 私信负责人</span>
        </button>
        <p class="kpd-tip">沟通后再交换微信，平台守护双方边界。</p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.kpd { max-width: 1200px; }
.kpd-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: none;
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
  letter-spacing: 1px;
  padding: 6px 0;
  margin-bottom: 16px;
  cursor: pointer;
  transition: color 180ms;
}
.kpd-back:hover { color: var(--kal-text); }
.kpd-notice {
  padding: 12px 16px;
  margin-bottom: 16px;
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
}

.kpd-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
.kpd-main { display: flex; flex-direction: column; gap: 20px; }

.kpd-hero { padding: 36px 40px; }
.kpd-hero-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-bottom: 18px;
  color: var(--kal-text-muted);
}
.kpd-hero-comp { color: var(--kal-text-strong); font-weight: 600; }
.kpd-hero-type { color: var(--kal-text); }
.kpd-hero-divider { width: 1px; height: 11px; background: var(--kal-border-strong); }
.kpd-hero-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--kal-text-muted);
}
.kpd-hero-status-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--kal-text-subtle); }
.kpd-hero-status--urgent { color: var(--kal-primary-700); }
.kpd-hero-status--urgent .kpd-hero-status-dot { background: var(--kal-primary-600); animation: kpd-pulse 1.6s ease-in-out infinite; }
@keyframes kpd-pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.3; }
}

.kpd-title {
  font-family: var(--kal-font-serif);
  font-size: 42px;
  font-weight: 600;
  letter-spacing: 3.5px;
  line-height: 1.35;
  color: var(--kal-text-strong);
  margin-bottom: 14px;
}
.kpd-subtitle {
  font-size: var(--kal-text-lg);
  color: var(--kal-text-muted);
  line-height: 1.85;
  margin-bottom: 28px;
  letter-spacing: 0.5px;
}
.kpd-author {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 24px;
  border-top: 1px solid var(--kal-divider);
}
.kpd-author-name {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  font-size: var(--kal-text-md);
  color: var(--kal-text-strong);
  letter-spacing: 0.5px;
}
.kpd-author-role {
  font-size: 10px;
  font-weight: 500;
  color: var(--kal-text-subtle);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  padding-left: 12px;
  border-left: 1px solid var(--kal-border);
}
.kpd-author-dept { font-size: 11px; color: var(--kal-text-subtle); margin-top: 4px; letter-spacing: 0.5px; }

/* Section */
.kpd-section { padding: 32px 36px; }
.kpd-section-head { margin-bottom: 22px; padding-bottom: 14px; border-bottom: 1px solid var(--kal-divider); }
.kpd-section-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 8px; }
.kpd-section-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xl);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 2px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.kpd-section-title .kal-serial { font-size: 0.7em; }

.kpd-detail {
  font-size: var(--kal-text-md);
  color: var(--kal-text);
  line-height: 1.95;
  letter-spacing: 0.3px;
}

/* Roles */
.kpd-roles { display: flex; flex-direction: column; gap: 4px; }
.kpd-role {
  display: flex;
  gap: 18px;
  padding: 18px 0;
  border-bottom: 1px dashed var(--kal-divider);
}
.kpd-role:last-child { border-bottom: none; }
.kpd-role-num {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 14px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
  margin-top: 2px;
  width: 24px;
  flex-shrink: 0;
}
.kpd-role-body { flex: 1; }
.kpd-role-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; gap: 10px; }
.kpd-role-name {
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: var(--kal-text-lg);
  color: var(--kal-text-strong);
  letter-spacing: 1.5px;
}
.kpd-role-count {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  color: var(--kal-primary-700);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-weight: 500;
  padding: 2px 10px;
  background: var(--kal-primary-50);
  border-radius: var(--kal-radius-xs);
}
.kpd-role-skills { font-size: var(--kal-text-sm); color: var(--kal-text-muted); display: flex; gap: 12px; align-items: baseline; }
.kpd-role-label {
  font-size: 10px;
  color: var(--kal-text-subtle);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-weight: 500;
  flex-shrink: 0;
}

/* Tags */
.kpd-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.kpd-tag {
  display: inline-flex;
  padding: 4px 12px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text);
  font-size: var(--kal-text-xs);
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}

/* Related */
.kpd-related { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 0; }
.kpd-related-item {
  position: relative;
  padding: 18px 24px 18px 4px;
  border-bottom: 1px solid var(--kal-divider);
  border-right: 1px solid var(--kal-divider);
  transition: all var(--kal-duration-2);
}
.kpd-related-item:hover { padding-left: 12px; background: var(--kal-bg-subtle); }
.kpd-related-meta {
  font-size: 10px;
  color: var(--kal-text-subtle);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-bottom: 8px;
}
.kpd-related-name {
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: var(--kal-text-md);
  color: var(--kal-text-strong);
  letter-spacing: 1px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.kpd-related-arrow {
  position: absolute;
  right: 16px;
  bottom: 18px;
  color: var(--kal-text-subtle);
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--kal-duration-2);
}
.kpd-related-item:hover .kpd-related-arrow { opacity: 1; transform: translateX(0); color: var(--kal-primary-700); }

/* ---------- Aside ---------- */
.kpd-aside { display: flex; flex-direction: column; gap: 16px; }
.kpd-stats { padding: 24px 28px; }
.kpd-stats-eyebrow { color: var(--kal-text-subtle); margin-bottom: 18px; }
.kpd-stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.kpd-stats-grid > div { display: flex; flex-direction: column; }
.kpd-stats-grid dt {
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: 28px;
  color: var(--kal-text-strong);
  letter-spacing: 0.5px;
  line-height: 1;
  margin-bottom: 5px;
}
.kpd-stats-grid dd { font-size: 10px; color: var(--kal-text-subtle); margin: 0; letter-spacing: 1.5px; text-transform: uppercase; }

.kpd-deadline {
  position: relative;
  padding: 22px 26px;
  background: var(--kal-ink);
  color: rgba(255, 255, 255, 0.92);
  border-radius: var(--kal-radius-md);
  overflow: hidden;
}
.kpd-deadline::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(300px 200px at 100% 100%, rgba(134, 26, 18, 0.45), transparent 60%);
}
.kpd-deadline-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  opacity: 0.7;
}
.kpd-deadline-label { font-size: 10px; letter-spacing: 2px; text-transform: uppercase; }
.kpd-deadline-num {
  position: relative;
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
}
.kpd-deadline-num strong {
  font-family: var(--kal-font-serif);
  font-size: 36px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
  line-height: 1;
}
.kpd-deadline-num span { font-size: 11px; opacity: 0.6; letter-spacing: 1.5px; }
.kpd-deadline-tip { position: relative; font-size: 11px; opacity: 0.55; letter-spacing: 0.5px; }

.kpd-apply {
  background: var(--kal-primary-600);
  border-color: var(--kal-primary-600);
  letter-spacing: 1px;
}
.kpd-apply:hover { background: var(--kal-primary-700); border-color: var(--kal-primary-700); }

.kpd-tip {
  text-align: center;
  font-size: 11px;
  color: var(--kal-text-subtle);
  margin-top: 4px;
  letter-spacing: 0.5px;
}

@media (min-width: 1024px) {
  .kpd-layout { grid-template-columns: 1fr 320px; }
  .kpd-aside { position: sticky; top: calc(var(--kal-header-height) + 16px); align-self: start; }
}
@media (max-width: 768px) {
  .kpd-hero { padding: 28px 24px; }
  .kpd-section { padding: 24px 24px; }
  .kpd-title { font-size: 28px; letter-spacing: 2px; }
}
</style>
