<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ProjectCard from '@/components/ProjectCard.vue'
import PersonalCard from '@/components/PersonalCard.vue'
import Icon from '@/components/Icon.vue'
import { projectsApi, personalCardsApi, competitionsApi, messagesApi } from '@/api/projects'
import { normalizeProject, normalizePersonalCard } from '@/api/normalize'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const tab = ref('projects')
const projects = ref([])
const personals = ref([])
const competitionCount = ref(0)
const loading = ref(false)
const loadError = ref('')
const filter = ref({
  competition: '',
  role: '',
  sort: 'urgent',
  keyword: ''
})

const competitionOptions = [
  { value: '',          label: '全部比赛' },
  { value: '互联网+',   label: '互联网+' },
  { value: '挑战杯',    label: '挑战杯' },
  { value: '大创',      label: '大创' },
  { value: '京彩大创',  label: '京彩大创' }
]
const roleOptions = [
  { value: '',         label: '全部角色' },
  { value: '技术开发', label: '技术开发' },
  { value: '产品设计', label: '产品设计' },
  { value: '视觉设计', label: '视觉设计' },
  { value: '商业运营', label: '商业运营' }
]
const sortOptions = [
  { value: 'urgent', label: '紧急度' },
  { value: 'latest', label: '热门度' }
]

const filteredProjects = computed(() => {
  let list = projects.value
  if (filter.value.competition) {
    list = list.filter(p => p.competition_short === filter.value.competition)
  }
  if (filter.value.keyword) {
    const k = filter.value.keyword.toLowerCase()
    list = list.filter(p =>
      p.project_name.toLowerCase().includes(k) ||
      p.one_liner.toLowerCase().includes(k) ||
      p.tags.some(t => t.toLowerCase().includes(k))
    )
  }
  if (filter.value.sort === 'urgent') {
    list = [...list].sort((a, b) => a.days_left - b.days_left)
  } else {
    list = [...list].sort((a, b) => b.view_count - a.view_count)
  }
  return list
})

const filteredPersonals = computed(() => {
  let list = personals.value
  if (filter.value.role) {
    list = list.filter(p => p.target_role === filter.value.role)
  }
  if (filter.value.keyword) {
    const k = filter.value.keyword.toLowerCase()
    list = list.filter(p =>
      p.skills.some(s => s.toLowerCase().includes(k)) ||
      p.self_intro.toLowerCase().includes(k)
    )
  }
  return list
})

function openProject(id) { router.push(`/projects/${id}`) }
async function openMessage(project) {
  if (!auth.isLoggedIn) return router.push({ path: '/login', query: { redirect: '/' } })
  const targetId = project?.creator_id
  if (!targetId) return router.push('/messages')
  try {
    const conv = await messagesApi.open(targetId)
    router.push({ path: '/messages', query: { conversation: conv.conversationId } })
  } catch (e) {
    router.push({ path: '/messages', query: { userId: targetId } })
  }
}
async function inviteCard(card) {
  if (!auth.isLoggedIn) return router.push({ path: '/login', query: { redirect: '/' } })
  if (!card?.user_id) return router.push('/messages')
  try {
    const conv = await messagesApi.open(card.user_id)
    router.push({ path: '/messages', query: { conversation: conv.conversationId } })
  } catch (e) {
    router.push({ path: '/messages', query: { userId: card.user_id } })
  }
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [p, cards, comps] = await Promise.all([
      projectsApi.list({ page: 1, size: 50 }),
      personalCardsApi.list({ page: 1, size: 50 }),
      competitionsApi.list()
    ])
    projects.value = (p?.items || []).map(normalizeProject)
    personals.value = (cards?.items || []).map(normalizePersonalCard)
    competitionCount.value = comps?.length || 0
  } catch (e) {
    loadError.value = '数据加载失败：' + (e.message || '请稍后重试')
    projects.value = []
    personals.value = []
    competitionCount.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="kal-container kh-home">
    <!-- ===================== HERO ===================== -->
    <section class="kh-hero">
      <div class="kh-hero-text">
        <div class="kal-eyebrow kh-hero-eyebrow">A&nbsp;Workshop&nbsp;of&nbsp;Knowing&nbsp;&amp;&nbsp;Doing</div>
        <h1 class="kh-hero-title">
          以<span class="kh-hero-emph">所知</span>启行，<br/>
          以<span class="kh-hero-emph">所行</span>成创。
        </h1>
        <p class="kh-hero-desc">
          人大学子的三创合作之所——构想在此凝结，<br class="kh-hero-br"/>
          同行者在此相遇，作品在此生长。
        </p>
        <div class="kh-hero-actions">
          <button class="kal-btn kal-btn-lg" @click="router.push('/projects/new')">
            <Icon name="plus" :size="14" :stroke="2" />
            <span>发布项目卡</span>
          </button>
          <button class="kal-btn kal-btn-lg kal-btn-secondary" @click="router.push('/personal-cards/edit')">
            <Icon name="target" :size="14" />
            <span>发布个人卡</span>
          </button>
        </div>
        <dl class="kh-stats">
          <div>
            <dt>{{ projects.length }}</dt>
            <dd><span class="kal-serial">/ </span>组队中项目</dd>
          </div>
          <div>
            <dt>{{ personals.length }}</dt>
            <dd><span class="kal-serial">/ </span>等待加入的同学</dd>
          </div>
          <div>
            <dt>{{ String(competitionCount || 0).padStart(2, '0') }}</dt>
            <dd><span class="kal-serial">/ </span>正在进行的赛事</dd>
          </div>
        </dl>
      </div>

      <!-- 编辑式装饰：以"卡片缩略图"为主体的小栏 -->
      <aside class="kh-hero-art" aria-hidden="true">
        <article class="kh-art-card">
          <div class="kh-art-meta">
            <span class="kal-serial">No. 01 / 项目卡</span>
            <span class="kh-art-tag">互联网+</span>
          </div>
          <h3 class="kh-art-title">智慧校园 AI 学习助手</h3>
          <p class="kh-art-desc">为人大同学打造的伴学系统：知识问答、学习规划与资源整合。</p>
          <div class="kh-art-foot">
            <span>缺&nbsp;前端 / 后端</span>
            <span class="kh-art-dot"></span>
            <span>3天后截止</span>
          </div>
        </article>

        <article class="kh-art-card kh-art-card--profile">
          <div class="kh-art-meta">
            <span class="kal-serial">No. 02 / 个人卡</span>
          </div>
          <div class="kh-art-profile">
            <span class="kal-avatar">刘</span>
            <div>
              <div class="kh-art-name">刘 同学</div>
              <div class="kh-art-tagline">信息学院 · 2023 级</div>
            </div>
          </div>
          <div class="kh-art-skills">
            <span>Python</span>
            <span>机器学习</span>
            <span>数据分析</span>
          </div>
        </article>

        <article class="kh-art-quote">
          <Icon name="message" :size="14" />
          <span>「彼此契合，交换微信。」</span>
        </article>
      </aside>
    </section>

    <!-- ===================== TAB & SEARCH ===================== -->
    <section class="kh-tabs-wrap">
      <div class="kh-tabs">
        <button
          class="kh-tab"
          :class="{ 'kh-tab--active': tab === 'projects' }"
          @click="tab = 'projects'"
        >
          <span>找项目</span>
          <span class="kh-tab-count">{{ projects.length }}</span>
        </button>
        <button
          class="kh-tab"
          :class="{ 'kh-tab--active': tab === 'teammates' }"
          @click="tab = 'teammates'"
        >
          <span>找队友</span>
          <span class="kh-tab-count">{{ personals.length }}</span>
        </button>
      </div>

      <div class="kh-search">
        <Icon name="search" :size="14" />
        <input
          class="kh-search-input"
          v-model="filter.keyword"
          placeholder="搜索项目 / 技能 / 关键字"
        />
      </div>
    </section>

    <!-- ===================== FILTERS ===================== -->
    <section class="kh-filters">
      <div class="kh-filter">
        <label>{{ tab === 'projects' ? '比赛' : '角色' }}</label>
        <select
          v-if="tab === 'projects'"
          class="kal-select kh-filter-select"
          v-model="filter.competition"
        >
          <option v-for="o in competitionOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <select v-else class="kal-select kh-filter-select" v-model="filter.role">
          <option v-for="o in roleOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </div>
      <div v-if="tab === 'projects'" class="kh-filter">
        <label>排序</label>
        <select class="kal-select kh-filter-select" v-model="filter.sort">
          <option v-for="o in sortOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </div>
      <div class="kh-filter-spacer"></div>
      <span class="kh-filter-count">
        <span class="kal-serial">共</span>
        <strong>{{ tab === 'projects' ? filteredProjects.length : filteredPersonals.length }}</strong>
        <span class="kal-serial">条</span>
      </span>
    </section>

    <div v-if="loadError" class="kal-card kh-notice">{{ loadError }}</div>
    <div v-if="loading" class="kal-card kh-notice">正在同步真实数据…</div>

    <!-- ===================== CARDS ===================== -->
    <section v-if="tab === 'projects'" class="kh-grid">
      <ProjectCard
        v-for="p in filteredProjects"
        :key="p.project_id"
        :project="p"
        @click="openProject(p.project_id)"
        @contact="openMessage"
      />
      <div v-if="filteredProjects.length === 0" class="kal-card kal-empty kh-grid-empty">
        <div class="kal-empty-title">暂无符合条件的项目</div>
        <div class="kal-empty-desc">尝试调整筛选项或清空关键字</div>
      </div>
    </section>

    <section v-else class="kh-grid">
      <PersonalCard
        v-for="card in filteredPersonals"
        :key="card.card_id"
        :card="card"
        @invite="inviteCard"
      />
      <div v-if="filteredPersonals.length === 0" class="kal-card kal-empty kh-grid-empty">
        <div class="kal-empty-title">暂无匹配同学</div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ---------- HERO ---------- */
.kh-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1fr;
  gap: 48px;
  margin-bottom: 56px;
  padding: 56px 48px 48px;
  border-radius: var(--kal-radius-lg);
  background: var(--kal-paper);
  border: 1px solid var(--kal-border);
  overflow: hidden;
}
.kh-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(28, 25, 23, 0.04) 1px, transparent 1px);
  background-size: 4px 4px;
  pointer-events: none;
}
.kh-hero-text { position: relative; z-index: 1; }
.kh-hero-eyebrow { color: var(--kal-text-subtle); margin-bottom: 24px; }
.kh-hero-title {
  font-family: var(--kal-font-serif);
  font-size: 56px;
  line-height: 1.2;
  font-weight: 500;
  letter-spacing: 6px;
  color: var(--kal-text-strong);
  margin-bottom: 24px;
}
.kh-hero-emph {
  font-weight: 700;
  color: var(--kal-primary-700);
  position: relative;
  padding: 0 4px;
}
.kh-hero-emph::after {
  content: '';
  position: absolute;
  inset: auto 4px -2px 4px;
  height: 6px;
  background: var(--kal-primary-100);
  z-index: -1;
}
.kh-hero-desc {
  font-size: var(--kal-text-lg);
  color: var(--kal-text-muted);
  line-height: 1.85;
  margin-bottom: 32px;
  max-width: 520px;
  letter-spacing: 1px;
}
.kh-hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.kh-hero-actions .kal-btn { letter-spacing: 1.5px; }

.kh-stats {
  display: flex;
  gap: 56px;
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid var(--kal-border);
  margin-block-end: 0;
}
.kh-stats > div { display: flex; flex-direction: column; }
.kh-stats dt {
  font-family: var(--kal-font-serif);
  font-size: 36px;
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 1px;
  line-height: 1;
  margin-bottom: 6px;
}
.kh-stats dd {
  font-size: var(--kal-text-xs);
  color: var(--kal-text-subtle);
  letter-spacing: 1.5px;
  margin: 0;
}

/* ---------- HERO ART ---------- */
.kh-hero-art {
  display: none;
  position: relative;
  height: 100%;
  min-height: 360px;
}
.kh-art-card {
  position: absolute;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  padding: 18px 20px;
  width: 256px;
  box-shadow: var(--kal-shadow-md);
  transition: transform var(--kal-duration-4) var(--kal-ease-out);
}
.kh-art-card:nth-of-type(1) {
  top: 12px;
  right: 32px;
  transform: rotate(2deg);
  animation: kh-float-a 7s ease-in-out infinite;
}
.kh-art-card:nth-of-type(2) {
  top: 168px;
  left: 0;
  transform: rotate(-2deg);
  animation: kh-float-b 8s ease-in-out infinite;
}
.kh-art-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--kal-text-subtle);
}
.kh-art-tag {
  background: var(--kal-bg-subtle);
  color: var(--kal-text);
  padding: 2px 8px;
  border-radius: var(--kal-radius-xs);
  font-weight: 500;
  letter-spacing: 0.5px;
}
.kh-art-title {
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: 15px;
  color: var(--kal-text-strong);
  letter-spacing: 1.5px;
  margin-bottom: 8px;
  line-height: 1.4;
}
.kh-art-desc {
  font-size: 12px;
  color: var(--kal-text-muted);
  line-height: 1.7;
  margin-bottom: 12px;
}
.kh-art-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--kal-text-subtle);
  padding-top: 12px;
  border-top: 1px dashed var(--kal-divider);
}
.kh-art-dot { width: 3px; height: 3px; border-radius: 50%; background: currentColor; }

.kh-art-card--profile { padding-bottom: 16px; }
.kh-art-profile {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.kh-art-name {
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: 14px;
  color: var(--kal-text-strong);
  letter-spacing: 1px;
}
.kh-art-tagline { font-size: 11px; color: var(--kal-text-subtle); margin-top: 2px; letter-spacing: 0.5px; }
.kh-art-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.kh-art-skills span {
  font-size: 10px;
  padding: 2px 8px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}

.kh-art-quote {
  position: absolute;
  bottom: 8px;
  right: 80px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--kal-ink);
  color: rgba(255, 255, 255, 0.92);
  font-family: var(--kal-font-serif);
  font-size: 12px;
  letter-spacing: 1.5px;
  border-radius: var(--kal-radius-full);
  box-shadow: var(--kal-shadow-md);
  animation: kh-float-c 6s ease-in-out infinite;
}

@keyframes kh-float-a {
  0%, 100% { transform: translateY(0) rotate(2deg); }
  50%      { transform: translateY(-10px) rotate(2.5deg); }
}
@keyframes kh-float-b {
  0%, 100% { transform: translateY(0) rotate(-2deg); }
  50%      { transform: translateY(-8px) rotate(-1.5deg); }
}
@keyframes kh-float-c {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-6px); }
}

/* ---------- TABS + SEARCH ---------- */
.kh-tabs-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.kh-tabs {
  display: inline-flex;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  padding: 3px;
  box-shadow: var(--kal-shadow-xs);
}
.kh-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 22px;
  background: transparent;
  border: none;
  font-size: var(--kal-text-md);
  font-weight: 500;
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-xs);
  transition: all var(--kal-duration-2) var(--kal-ease-out);
  letter-spacing: 1px;
}
.kh-tab--active {
  background: var(--kal-ink);
  color: #fff;
}
.kh-tab-count {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 11px;
  opacity: 0.6;
}
.kh-tab--active .kh-tab-count { opacity: 0.7; }

.kh-search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 42px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  color: var(--kal-text-subtle);
  transition: all var(--kal-duration-2);
  min-width: 240px;
  max-width: 380px;
}
.kh-search:focus-within {
  border-color: var(--kal-ink);
  color: var(--kal-text);
}
.kh-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: var(--kal-text-md);
  color: var(--kal-text);
}

/* ---------- FILTERS ---------- */
.kh-filters {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 24px;
  padding: 14px 20px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
}
.kh-notice {
  padding: 12px 16px;
  margin-bottom: 16px;
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
}
.kh-filter { display: flex; align-items: center; gap: 10px; }
.kh-filter label {
  font-size: 11px;
  color: var(--kal-text-subtle);
  font-weight: 500;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}
.kh-filter-select {
  width: auto;
  min-width: 130px;
  padding: 6px 30px 6px 10px;
  font-size: var(--kal-text-sm);
  background-color: transparent;
  border-color: transparent;
  border-bottom: 1px solid var(--kal-border-strong);
  border-radius: 0;
}
.kh-filter-select:focus {
  border-bottom-color: var(--kal-ink);
  box-shadow: none;
}
.kh-filter-spacer { flex: 1; }
.kh-filter-count {
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
}
.kh-filter-count strong { color: var(--kal-text-strong); font-family: var(--kal-font-serif); font-size: var(--kal-text-lg); }

/* ---------- GRID ---------- */
.kh-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}
.kh-grid-empty { grid-column: 1 / -1; padding: 80px 20px; text-align: center; }

@media (min-width: 1024px) {
  .kh-hero { grid-template-columns: 1.35fr 1fr; }
  .kh-hero-art { display: block; }
}

@media (max-width: 768px) {
  .kh-hero { padding: 36px 24px 32px; gap: 24px; }
  .kh-hero-title { font-size: 36px; letter-spacing: 3px; }
  .kh-hero-desc { font-size: var(--kal-text-md); }
  .kh-hero-br { display: none; }
  .kh-stats { gap: 24px; flex-wrap: wrap; }
  .kh-stats dt { font-size: 26px; }
  .kh-search { min-width: 100%; max-width: 100%; }
  .kh-filters { padding: 12px 14px; gap: 14px; }
}
</style>
