<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { competitionsApi } from '@/api/projects'
import { normalizeCompetition, normalizeNews } from '@/api/normalize'
import Icon from '@/components/Icon.vue'

const route = useRoute()
const router = useRouter()

const competition = ref(null)
const news = ref([])
const loading = ref(false)
const error = ref('')

const statusMap = {
  active:   '报名中',
  urgent:   '即将截止',
  upcoming: '即将开启',
  ended:    '已结束'
}
const levelLabelCn = {
  national:    '国家级',
  provincial:  '北京市级',
  school:      '校级'
}

const dateLabel = computed(() => {
  if (!competition.value) return ''
  const a = competition.value.register_start || ''
  const b = competition.value.register_end || ''
  return `${a}  →  ${b}`
})

async function load () {
  loading.value = true
  error.value = ''
  try {
    const c = await competitionsApi.detail(route.params.id)
    competition.value = normalizeCompetition(c)
    const ns = await competitionsApi.news(route.params.id)
    news.value = (ns || []).map(normalizeNews)
  } catch (e) {
    error.value = '比赛详情加载失败：' + (e.message || '请稍后重试')
    competition.value = null
    news.value = []
  } finally {
    loading.value = false
  }
}

function goJoin () {
  router.push({ path: '/projects/new', query: { prefill_competition: competition.value?.short_name } })
}
function back () { router.back() }
function openNews (n) { router.push({ name: 'news-detail', params: { id: n.news_id } }) }

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="kal-container kcd">
    <button class="kcd-back" @click="back">
      <Icon name="arrow-left" :size="14" :stroke="1.8" />
      <span>返回比赛中心</span>
    </button>

    <div v-if="error" class="kal-card kcd-notice">{{ error }}</div>
    <div v-if="loading" class="kal-card kcd-notice">正在读取赛事档案…</div>

    <template v-if="competition">
      <!-- ============ 顶部封面 ============ -->
      <header class="kcd-hero">
        <div class="kcd-poster">
          <span class="kcd-poster-no">No.&nbsp;{{ competition.competition_id?.slice(-2)?.toUpperCase() || '01' }}</span>
          <span class="kcd-poster-initial">{{ competition.initial }}</span>
          <span class="kcd-poster-level">{{ levelLabelCn[competition.level] || '—' }}</span>
        </div>
        <div class="kcd-head">
          <div class="kcd-meta">
            <span class="kcd-status">
              <span class="kcd-dot" :class="`kcd-dot--${competition.status}`"></span>
              <span>{{ statusMap[competition.status] || '—' }}</span>
            </span>
            <span class="kcd-meta-sep"></span>
            <span>{{ levelLabelCn[competition.level] || '—' }}</span>
            <span class="kcd-meta-sep"></span>
            <span>主办：{{ competition.organizer || '—' }}</span>
          </div>
          <h1 class="kcd-title">{{ competition.name }}</h1>
          <p class="kcd-desc">{{ competition.description || '尚未填写赛事介绍。' }}</p>

          <dl class="kcd-stats">
            <div>
              <dt>{{ competition.days_left }}</dt>
              <dd>距报名截止（天）</dd>
            </div>
            <div>
              <dt>{{ competition.project_count || 0 }}</dt>
              <dd>组队中项目</dd>
            </div>
            <div>
              <dt>{{ news.length || 0 }}</dt>
              <dd>关联资讯</dd>
            </div>
          </dl>

          <div class="kcd-actions">
            <button class="kal-btn kal-btn-lg" @click="goJoin">
              <Icon name="plus" :size="14" :stroke="2" />
              <span>发起组队报名</span>
            </button>
            <a v-if="competition.official_links?.[0]?.url" class="kal-btn kal-btn-lg kal-btn-secondary"
               :href="competition.official_links[0].url" target="_blank" rel="noopener">
              <Icon name="external-link" :size="14" />
              <span>{{ competition.official_links[0].label || '官方主页' }}</span>
            </a>
          </div>
        </div>
      </header>

      <!-- ============ 主体两栏 ============ -->
      <div class="kcd-layout">
        <main class="kcd-main">
          <!-- 报名时间 -->
          <section class="kal-card kcd-section">
            <header class="kcd-section-head">
              <span class="kal-eyebrow">Schedule</span>
              <h2><span class="kal-serial">i.</span><span>报名与赛程</span></h2>
            </header>
            <div class="kcd-schedule">
              <div class="kcd-sched-row">
                <span class="kcd-sched-key">报名窗口</span>
                <span class="kcd-sched-val">{{ dateLabel }}</span>
              </div>
              <div v-if="competition.schedule_note" class="kcd-sched-note">{{ competition.schedule_note }}</div>
            </div>
          </section>

          <!-- 奖励 -->
          <section v-if="competition.prize" class="kal-card kcd-section">
            <header class="kcd-section-head">
              <span class="kal-eyebrow">Awards</span>
              <h2><span class="kal-serial">ii.</span><span>奖项设置</span></h2>
            </header>
            <p class="kcd-text">{{ competition.prize }}</p>
          </section>

          <!-- 资讯 -->
          <section class="kal-card kcd-section">
            <header class="kcd-section-head">
              <span class="kal-eyebrow">Bulletin</span>
              <h2><span class="kal-serial">iii.</span><span>赛事资讯</span></h2>
            </header>
            <div v-if="!news.length" class="kcd-empty">暂无关联资讯。</div>
            <div v-else class="kcd-news-list">
              <article v-for="(n, i) in news" :key="n.news_id" class="kcd-news" @click="openNews(n)">
                <span class="kcd-news-num">{{ String(i + 1).padStart(2, '0') }}</span>
                <div class="kcd-news-body">
                  <h3>{{ n.title }}</h3>
                  <div class="kcd-news-meta">
                    <span>{{ n.source || '知行创坊' }}</span>
                    <span class="kcd-news-dot"></span>
                    <span>{{ n.publish_at }}</span>
                  </div>
                  <p>{{ n.summary }}</p>
                </div>
                <Icon name="arrow-right" :size="14" :stroke="1.8" />
              </article>
            </div>
          </section>
        </main>

        <!-- 右栏：链接 / 二维码 / 联系 -->
        <aside class="kcd-side">
          <section v-if="competition.official_links?.length" class="kal-card kcd-side-card">
            <h3 class="kcd-side-title">
              <Icon name="external-link" :size="14" />
              <span>官方链接</span>
            </h3>
            <a v-for="(l, i) in competition.official_links" :key="i" class="kcd-link"
               :href="l.url" target="_blank" rel="noopener">
              <span class="kcd-link-num">{{ String(i + 1).padStart(2, '0') }}</span>
              <span class="kcd-link-body">
                <span class="kcd-link-label">{{ l.label || '官方资源' }}</span>
                <span class="kcd-link-url">{{ l.url }}</span>
              </span>
              <Icon name="arrow-right" :size="13" :stroke="1.8" />
            </a>
          </section>

          <section v-if="competition.qr_codes?.length" class="kal-card kcd-side-card">
            <h3 class="kcd-side-title">
              <Icon name="message" :size="14" />
              <span>扫码加入 / 关注</span>
            </h3>
            <div class="kcd-qr-list">
              <div v-for="(q, i) in competition.qr_codes" :key="i" class="kcd-qr">
                <div class="kcd-qr-img">
                  <img v-if="q.imageUrl" :src="q.imageUrl" :alt="q.label" />
                  <span v-else>QR</span>
                </div>
                <span class="kcd-qr-label">{{ q.label || '加入交流' }}</span>
              </div>
            </div>
          </section>

          <section v-if="competition.contact_email || competition.contact_phone" class="kal-card kcd-side-card">
            <h3 class="kcd-side-title">
              <Icon name="info" :size="14" />
              <span>联系</span>
            </h3>
            <div v-if="competition.contact_email" class="kcd-contact">
              <span class="kcd-contact-key">邮箱</span>
              <a class="kcd-contact-val" :href="`mailto:${competition.contact_email}`">{{ competition.contact_email }}</a>
            </div>
            <div v-if="competition.contact_phone" class="kcd-contact">
              <span class="kcd-contact-key">电话</span>
              <span class="kcd-contact-val">{{ competition.contact_phone }}</span>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.kcd { max-width: 1200px; }
.kcd-back {
  display: inline-flex; align-items: center; gap: 8px;
  background: transparent; border: 0; padding: 0;
  color: var(--kal-text-muted); cursor: pointer;
  margin-bottom: 24px; font-size: var(--kal-text-sm); letter-spacing: 1px;
}
.kcd-back:hover { color: var(--kal-primary-700); }
.kcd-notice { padding: 12px 16px; margin-bottom: 16px; color: var(--kal-text-muted); font-size: var(--kal-text-sm); }

/* hero */
.kcd-hero {
  display: grid;
  grid-template-columns: 1fr;
  gap: 28px;
  background: var(--kal-paper);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
  padding: 32px;
  margin-bottom: 32px;
  overflow: hidden;
}
.kcd-poster {
  position: relative;
  height: 240px;
  background: var(--ruc-red-dark);
  color: rgba(255,255,255,0.95);
  border-radius: var(--kal-radius-md);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.kcd-poster::before {
  content: ''; position: absolute; inset: 0;
  background:
    radial-gradient(420px 220px at 100% 0%, rgba(161, 84, 72, 0.55), transparent 70%),
    radial-gradient(300px 190px at 0% 100%, rgba(199, 188, 185, 0.24), transparent 70%);
}
.kcd-poster::after {
  content: ''; position: absolute; inset: 0;
  background-image: radial-gradient(rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 4px 4px;
}
.kcd-poster-no {
  position: absolute; top: 16px; left: 18px;
  font-family: var(--kal-font-serif); font-style: italic;
  font-size: 11px; letter-spacing: 2px; opacity: 0.55;
}
.kcd-poster-level {
  position: absolute; top: 16px; right: 18px;
  font-size: 10px; letter-spacing: 2.5px; text-transform: uppercase;
  opacity: 0.6; font-weight: 500;
}
.kcd-poster-initial {
  font-family: var(--kal-font-serif); font-size: 160px;
  font-weight: 700; letter-spacing: -2px; z-index: 1; line-height: 1;
  color: var(--ruc-red-bg);
}

.kcd-head .kcd-meta {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  font-size: 11px; color: var(--kal-text-subtle);
  letter-spacing: 1.5px; text-transform: uppercase; margin-bottom: 14px;
}
.kcd-status {
  display: inline-flex; align-items: center; gap: 6px;
  color: var(--kal-text); font-weight: 500;
}
.kcd-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--kal-text-subtle); }
.kcd-dot--active { background: var(--kal-success); }
.kcd-dot--urgent { background: var(--kal-primary-600); }
.kcd-dot--upcoming { background: var(--kal-sand); }
.kcd-dot--ended { background: var(--kal-border-strong); }
.kcd-meta-sep { width: 1px; height: 11px; background: var(--kal-border-strong); }

.kcd-title {
  font-family: var(--kal-font-serif);
  font-size: 38px; font-weight: 600;
  letter-spacing: 4px; line-height: 1.35;
  color: var(--kal-text-strong); margin-bottom: 14px;
}
.kcd-desc {
  font-size: var(--kal-text-md);
  color: var(--kal-text-muted);
  line-height: 1.85; letter-spacing: 0.5px;
  margin-bottom: 28px; max-width: 720px;
}

.kcd-stats {
  display: flex; gap: 56px;
  padding: 22px 0;
  border-top: 1px solid var(--kal-border);
  border-bottom: 1px solid var(--kal-border);
  margin-bottom: 22px;
}
.kcd-stats > div { display: flex; flex-direction: column; }
.kcd-stats dt {
  font-family: var(--kal-font-serif);
  font-size: 32px; font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 1px; line-height: 1;
  margin-bottom: 6px;
}
.kcd-stats dd {
  font-size: 11px; color: var(--kal-text-subtle);
  letter-spacing: 1.5px; margin: 0;
}

.kcd-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.kcd-actions .kal-btn { letter-spacing: 1.5px; }

/* layout */
.kcd-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 1024px) {
  .kcd-layout { grid-template-columns: 1fr 360px; }
  .kcd-hero { grid-template-columns: 360px 1fr; align-items: stretch; }
  .kcd-poster { height: auto; min-height: 320px; }
}

.kcd-section { padding: 28px 32px; margin-bottom: 20px; }
.kcd-section-head { margin-bottom: 18px; }
.kcd-section-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 8px; }
.kcd-section-head h2 {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-2xl);
  font-weight: 600; letter-spacing: 3px;
  color: var(--kal-text-strong);
  display: inline-flex; align-items: baseline; gap: 12px;
}
.kcd-section-head h2 .kal-serial {
  font-size: 14px; color: var(--kal-text-subtle);
}

.kcd-text {
  color: var(--kal-text);
  font-size: var(--kal-text-md);
  line-height: 1.95; letter-spacing: 0.5px;
  white-space: pre-line;
}
.kcd-empty {
  text-align: center; padding: 32px 12px;
  color: var(--kal-text-subtle); font-size: var(--kal-text-sm);
  font-family: var(--kal-font-serif); font-style: italic; letter-spacing: 1px;
}

/* schedule */
.kcd-schedule { display: flex; flex-direction: column; gap: 14px; }
.kcd-sched-row {
  display: grid; grid-template-columns: 80px 1fr; gap: 18px;
  padding: 14px 0; border-bottom: 1px dashed var(--kal-divider);
}
.kcd-sched-key {
  font-size: 11px; letter-spacing: 1.5px; color: var(--kal-text-subtle);
  text-transform: uppercase; padding-top: 4px;
}
.kcd-sched-val {
  font-family: var(--kal-font-serif); font-weight: 500;
  font-size: var(--kal-text-md); color: var(--kal-text-strong); letter-spacing: 1px;
}
.kcd-sched-note {
  background: var(--kal-bg-subtle); padding: 14px 18px;
  border-radius: var(--kal-radius-sm);
  font-size: var(--kal-text-sm); color: var(--kal-text-muted);
  line-height: 1.85;
}

/* news list */
.kcd-news-list { display: flex; flex-direction: column; }
.kcd-news {
  display: flex; gap: 16px; align-items: flex-start;
  padding: 18px 0;
  border-bottom: 1px solid var(--kal-divider);
  cursor: pointer;
  transition: padding-left var(--kal-duration-2);
}
.kcd-news:last-child { border-bottom: 0; }
.kcd-news:hover { padding-left: 8px; }
.kcd-news:hover h3 { color: var(--kal-primary-700); }
.kcd-news-num {
  font-family: var(--kal-font-serif); font-style: italic;
  font-size: 13px; color: var(--kal-text-subtle); letter-spacing: 1px;
  margin-top: 4px; flex-shrink: 0;
}
.kcd-news-body { flex: 1; min-width: 0; }
.kcd-news-body h3 {
  font-family: var(--kal-font-serif); font-weight: 600;
  font-size: var(--kal-text-md); color: var(--kal-text-strong);
  letter-spacing: 1px; line-height: 1.5; margin-bottom: 6px;
  transition: color var(--kal-duration-2);
}
.kcd-news-meta {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 1px;
  margin-bottom: 8px;
}
.kcd-news-dot { width: 3px; height: 3px; border-radius: 50%; background: currentColor; }
.kcd-news-body p {
  font-size: var(--kal-text-sm); color: var(--kal-text-muted);
  line-height: 1.75;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}

/* side */
.kcd-side { display: flex; flex-direction: column; gap: 20px; }
.kcd-side-card { padding: 22px 24px; }
.kcd-side-title {
  display: inline-flex; align-items: center; gap: 8px;
  font-family: var(--kal-font-serif); font-weight: 600;
  font-size: var(--kal-text-md); letter-spacing: 2px;
  color: var(--kal-text-strong); margin-bottom: 18px;
}
.kcd-link {
  display: flex; gap: 12px; align-items: center;
  padding: 12px 0; border-bottom: 1px dashed var(--kal-divider);
  cursor: pointer; transition: color var(--kal-duration-2);
}
.kcd-link:last-child { border-bottom: 0; }
.kcd-link:hover { color: var(--kal-primary-700); }
.kcd-link-num {
  font-family: var(--kal-font-serif); font-style: italic;
  font-size: 12px; color: var(--kal-text-subtle); letter-spacing: 1px;
}
.kcd-link-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.kcd-link-label {
  font-size: var(--kal-text-sm); font-weight: 500;
  color: var(--kal-text-strong); letter-spacing: 0.5px;
}
.kcd-link-url {
  font-size: 11px; color: var(--kal-text-subtle);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.kcd-qr-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.kcd-qr {
  display: flex; flex-direction: column; align-items: center;
  gap: 8px;
}
.kcd-qr-img {
  width: 100%; aspect-ratio: 1 / 1;
  background: var(--kal-bg-subtle);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  color: var(--kal-text-subtle);
  font-family: var(--kal-font-serif); letter-spacing: 2px;
}
.kcd-qr-img img { width: 100%; height: 100%; object-fit: cover; }
.kcd-qr-label {
  font-size: 11px; color: var(--kal-text-muted);
  letter-spacing: 1px; text-align: center;
}

.kcd-contact {
  display: flex; gap: 16px; padding: 10px 0;
  border-bottom: 1px dashed var(--kal-divider);
}
.kcd-contact:last-child { border-bottom: 0; }
.kcd-contact-key {
  width: 48px; flex-shrink: 0;
  font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 1.5px;
  text-transform: uppercase; padding-top: 2px;
}
.kcd-contact-val {
  flex: 1; font-size: var(--kal-text-sm);
  color: var(--kal-text-strong); letter-spacing: 0.5px;
  word-break: break-all;
}

@media (max-width: 768px) {
  .kcd-title { font-size: 26px; letter-spacing: 2px; }
  .kcd-stats { gap: 24px; flex-wrap: wrap; }
  .kcd-stats dt { font-size: 24px; }
  .kcd-section { padding: 22px 20px; }
}
</style>
