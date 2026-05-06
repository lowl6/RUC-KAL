<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { competitionsApi, newsApi } from '@/api/projects'
import { normalizeCompetition, normalizeNews } from '@/api/normalize'
import Icon from '@/components/Icon.vue'

const router = useRouter()
const filter = ref('all')
const competitions = ref([])
const news = ref([])
const loading = ref(false)
const error = ref('')

const filtered = computed(() => {
  if (filter.value === 'all') return competitions.value
  return competitions.value.filter(c => c.status === filter.value)
})

const statusMap = {
  active:   { label: '报名中' },
  urgent:   { label: '即将截止' },
  upcoming: { label: '即将开启' },
  ended:    { label: '已结束' }
}
const levelMap = {
  national:    'National',
  provincial:  'Beijing',
  school:      'In-School'
}
const levelLabelCn = {
  national:    '国家级',
  provincial:  '北京市级',
  school:      '校级'
}

function openCompetition(c) {
  router.push({ name: 'competition-detail', params: { id: c.competition_id } })
}

function openNews(n) {
  // 来源是外部链接（公众号文章等）→ 直接新标签页跳转原文
  if (n?.link) {
    window.open(n.link, '_blank', 'noopener')
    return
  }
  router.push({ name: 'news-detail', params: { id: n.news_id } })
}

async function loadCompetitions() {
  loading.value = true
  error.value = ''
  try {
    const [list, newsList] = await Promise.all([
      competitionsApi.list(),
      newsApi.list()
    ])
    competitions.value = (list || []).map(normalizeCompetition)
    news.value = (newsList || []).map(normalizeNews)
  } catch (e) {
    error.value = '比赛数据加载失败：' + (e.message || '后端不可用')
    competitions.value = []
    news.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadCompetitions)
</script>

<template>
  <div class="kal-container kc">
    <header class="kc-head">
      <div class="kal-eyebrow">Competition&nbsp;Center</div>
      <h1 class="kc-title">比赛中心</h1>
      <p class="kc-sub">汇集校内外可参与的三创赛事——选定一片舞台，与同行者共赴。</p>
    </header>

    <div class="kc-filter">
      <button
        v-for="f in [
          { value: 'all', label: '全部' },
          { value: 'urgent', label: '即将截止' },
          { value: 'active', label: '报名中' },
          { value: 'upcoming', label: '即将开启' }
        ]"
        :key="f.value"
        class="kc-pill"
        :class="{ 'kc-pill--active': filter === f.value }"
        @click="filter = f.value"
      >{{ f.label }}</button>
    </div>

    <div v-if="error" class="kal-card kc-notice">{{ error }}</div>
    <div v-if="loading" class="kal-card kc-notice">正在同步比赛数据…</div>

    <!-- Competition cards -->
    <div class="kc-grid">
      <article
        v-for="(c, i) in filtered"
        :key="c.competition_id"
        class="kal-card kal-card-hoverable kc-card"
        @click="openCompetition(c)"
      >
        <!-- Poster: 全部使用人大红单色 + 大字 serif initial -->
        <div class="kc-poster">
          <span class="kc-poster-no">No.&nbsp;{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="kc-poster-initial">{{ c.initial }}</span>
          <span class="kc-poster-level">{{ levelMap[c.level] }}</span>
        </div>

        <div class="kc-card-body">
          <div class="kc-card-meta">
            <span class="kc-card-status">
              <span class="kc-status-dot" :class="`kc-status-dot--${c.status}`"></span>
              {{ statusMap[c.status].label }}
            </span>
            <span class="kc-card-divider"></span>
            <span class="kc-card-level">{{ levelLabelCn[c.level] }}</span>
          </div>

          <h3 class="kc-card-title">{{ c.name }}</h3>
          <p class="kc-card-desc">{{ c.description }}</p>

          <div class="kc-card-info">
            <div>
              <span class="kc-info-label">距截止</span>
              <span class="kc-info-value">{{ c.days_left }} 天</span>
            </div>
            <div>
              <span class="kc-info-label">主办</span>
              <span class="kc-info-value kc-info-org">{{ c.organizer }}</span>
            </div>
          </div>

          <div class="kc-card-foot">
            <div class="kc-card-stat">
              <span class="kc-card-stat-num">{{ c.project_count }}</span>
              <span class="kc-card-stat-label">个组队中项目</span>
            </div>
            <button class="kal-btn kal-btn-sm kal-btn-ghost kc-card-btn" @click.stop="openCompetition(c)">
              <span>组队</span>
              <Icon name="arrow-right" :size="13" :stroke="1.8" />
            </button>
          </div>
        </div>
      </article>
    </div>

    <!-- News -->
    <section class="kc-news">
      <header class="kc-news-head">
        <div>
          <div class="kal-eyebrow">Latest&nbsp;Stories</div>
          <h2 class="kc-news-title">相关资讯</h2>
        </div>
        <a class="kc-news-more">
          <span>查看更多</span>
          <Icon name="arrow-right" :size="13" :stroke="1.8" />
        </a>
      </header>
      <div v-if="news.length === 0 && !loading" class="kc-news-empty">暂无资讯，敬请期待。</div>
      <div class="kc-news-list">
        <a v-for="(n, idx) in news" :key="n.news_id" class="kc-news-item"
           :class="{ 'kc-news-item--ext': n.link }"
           @click="openNews(n)">
          <span class="kc-news-num">{{ String(idx + 1).padStart(2, '0') }}</span>
          <div class="kc-news-body">
            <h4>
              <span class="kc-news-text">{{ n.title }}</span>
              <span v-if="n.link" class="kc-news-ext" title="点击跳转公众号原文">
                <Icon name="link" :size="11" />
                <span>原文</span>
              </span>
            </h4>
            <div class="kc-news-meta">
              <span class="kc-news-source">{{ n.source || '知行创坊' }}</span>
              <span class="kc-news-dot"></span>
              <span class="kc-news-time">{{ n.publish_at }}</span>
            </div>
          </div>
        </a>
      </div>
    </section>
  </div>
</template>

<style scoped>
.kc { max-width: 1280px; }
.kc-head { margin-bottom: 32px; }
.kc-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 14px; }
.kc-title {
  font-family: var(--kal-font-serif);
  font-size: 44px;
  font-weight: 600;
  letter-spacing: 6px;
  color: var(--kal-text-strong);
  margin-bottom: 14px;
}
.kc-sub {
  color: var(--kal-text-muted);
  font-size: var(--kal-text-md);
  letter-spacing: 0.5px;
  line-height: 1.85;
  max-width: 540px;
}

.kc-filter { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 28px; }
.kc-notice {
  padding: 12px 16px;
  margin-bottom: 16px;
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
}
.kc-pill {
  padding: 8px 18px;
  border: 1px solid var(--kal-border);
  background: var(--kal-surface);
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-full);
  font-size: var(--kal-text-sm);
  letter-spacing: 1px;
  cursor: pointer;
  transition: all var(--kal-duration-2);
}
.kc-pill:hover { border-color: var(--kal-text); color: var(--kal-text); }
.kc-pill--active {
  background: var(--kal-ink);
  color: #fff;
  border-color: var(--kal-ink);
}

.kc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
  margin-bottom: 64px;
}
.kc-card {
  padding: 0;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ---------- Poster ---------- */
.kc-poster {
  position: relative;
  height: 160px;
  background: var(--ruc-red-dark);
  color: rgba(255, 255, 255, 0.92);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.kc-poster::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(420px 220px at 100% 0%, rgba(161, 84, 72, 0.55), transparent 70%),
    radial-gradient(300px 190px at 0% 100%, rgba(199, 188, 185, 0.24), transparent 70%);
}
.kc-poster::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 4px 4px;
  pointer-events: none;
  opacity: 0.5;
}
.kc-poster-no {
  position: absolute;
  top: 16px;
  left: 18px;
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 11px;
  letter-spacing: 2px;
  opacity: 0.55;
}
.kc-poster-level {
  position: absolute;
  top: 16px;
  right: 18px;
  font-size: 10px;
  letter-spacing: 2.5px;
  text-transform: uppercase;
  opacity: 0.6;
  font-weight: 500;
}
.kc-poster-initial {
  font-family: var(--kal-font-serif);
  font-size: 96px;
  font-weight: 700;
  letter-spacing: -2px;
  z-index: 1;
  line-height: 1;
  color: var(--ruc-red-bg);
}

/* ---------- Body ---------- */
.kc-card-body { padding: 22px 24px 22px; flex: 1; display: flex; flex-direction: column; }
.kc-card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 11px;
  letter-spacing: 1.5px;
  color: var(--kal-text-subtle);
  text-transform: uppercase;
}
.kc-card-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--kal-text);
  font-weight: 500;
}
.kc-status-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--kal-text-subtle); }
.kc-status-dot--active   { background: var(--kal-success); }
.kc-status-dot--urgent   { background: var(--kal-primary-600); }
.kc-status-dot--upcoming { background: var(--kal-sand); }
.kc-card-divider { width: 1px; height: 10px; background: var(--kal-border-strong); }
.kc-card-level { color: var(--kal-text-muted); }

.kc-card-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-lg);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 1.5px;
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 48px;
}
.kc-card-desc {
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  line-height: 1.85;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kc-card-info {
  display: flex;
  gap: 28px;
  padding: 14px 0;
  border-top: 1px dashed var(--kal-divider);
  border-bottom: 1px dashed var(--kal-divider);
  margin-bottom: 14px;
}
.kc-card-info > div { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.kc-info-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: var(--kal-text-subtle); }
.kc-info-value {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-md);
  color: var(--kal-text-strong);
  font-weight: 500;
  letter-spacing: 0.5px;
}
.kc-info-org { font-size: var(--kal-text-sm); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.kc-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}
.kc-card-stat { display: flex; align-items: baseline; gap: 6px; }
.kc-card-stat-num {
  font-family: var(--kal-font-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 0.5px;
}
.kc-card-stat-label { font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 1px; }
.kc-card-btn {
  padding: 4px 10px;
  font-size: var(--kal-text-sm);
  letter-spacing: 1.5px;
  color: var(--kal-text);
}
.kc-card-btn:hover { color: var(--kal-primary-700); background: transparent; }

/* ---------- News ---------- */
.kc-news {
  padding: 32px 36px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
}
.kc-news-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--kal-divider);
  gap: 16px;
}
.kc-news-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 8px; }
.kc-news-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-2xl);
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--kal-text-strong);
}
.kc-news-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--kal-text);
  font-size: var(--kal-text-sm);
  cursor: pointer;
  letter-spacing: 1px;
}
.kc-news-more:hover { color: var(--kal-primary-700); }
.kc-news-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 0 36px; }
.kc-news-empty {
  text-align: center;
  padding: 40px 16px;
  color: var(--kal-text-subtle);
  font-size: var(--kal-text-sm);
  letter-spacing: 1px;
  font-family: var(--kal-font-serif);
  font-style: italic;
}
.kc-news-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 18px 0;
  border-bottom: 1px solid var(--kal-divider);
  cursor: pointer;
  transition: all var(--kal-duration-2);
}
.kc-news-item:hover { padding-left: 6px; }
.kc-news-num {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 13px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
  margin-top: 2px;
  flex-shrink: 0;
}
.kc-news-body { flex: 1; min-width: 0; }
.kc-news-item h4 {
  font-size: var(--kal-text-md);
  font-weight: 500;
  color: var(--kal-text);
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  letter-spacing: 0.5px;
  transition: color var(--kal-duration-2);
}
.kc-news-item:hover h4 { color: var(--kal-primary-700); }
.kc-news-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
}
.kc-news-source { color: var(--kal-text-muted); }
.kc-news-dot { width: 3px; height: 3px; border-radius: 50%; background: currentColor; }

.kc-news-item h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  -webkit-line-clamp: unset;
  overflow: visible;
}
.kc-news-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kc-news-ext {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 7px;
  font-size: 10.5px;
  letter-spacing: 1px;
  color: var(--ruc-red);
  background: var(--kal-primary-50);
  border: 1px solid var(--kal-primary-200);
  border-radius: var(--kal-radius-full);
  flex-shrink: 0;
  font-weight: 400;
}
.kc-news-item--ext:hover .kc-news-ext {
  background: var(--kal-primary-100);
}

@media (max-width: 768px) {
  .kc-title { font-size: 30px; letter-spacing: 3px; }
  .kc-news { padding: 24px; }
}
</style>
