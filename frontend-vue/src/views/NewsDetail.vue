<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { newsApi, competitionsApi } from '@/api/projects'
import { normalizeNews, normalizeCompetition } from '@/api/normalize'
import Icon from '@/components/Icon.vue'

const route = useRoute()
const router = useRouter()

const news = ref(null)
const competition = ref(null)
const loading = ref(false)
const error = ref('')

async function load () {
  loading.value = true; error.value = ''
  try {
    const n = await newsApi.detail(route.params.id)
    news.value = normalizeNews(n)
    if (news.value.competition_id) {
      try {
        const c = await competitionsApi.detail(news.value.competition_id)
        competition.value = normalizeCompetition(c)
      } catch { competition.value = null }
    } else {
      competition.value = null
    }
  } catch (e) {
    error.value = '资讯加载失败：' + (e.message || '请稍后重试')
    news.value = null
  } finally {
    loading.value = false
  }
}

function goCompetition () {
  if (competition.value) router.push({ name: 'competition-detail', params: { id: competition.value.competition_id } })
}
function back () { router.back() }

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="kal-container knd">
    <button class="knd-back" @click="back">
      <Icon name="arrow-left" :size="14" :stroke="1.8" />
      <span>返回</span>
    </button>

    <div v-if="error" class="kal-card knd-notice">{{ error }}</div>
    <div v-if="loading" class="kal-card knd-notice">正在读取资讯…</div>

    <article v-if="news" class="knd-article">
      <div class="kal-eyebrow knd-kicker">News&nbsp;/&nbsp;{{ news.source || '知行创坊' }}</div>

      <h1 class="knd-title">{{ news.title }}</h1>

      <div class="knd-meta">
        <span>{{ news.publish_at || '—' }}</span>
        <span class="knd-meta-sep"></span>
        <span>{{ news.source || '知行创坊' }}</span>
        <template v-if="competition">
          <span class="knd-meta-sep"></span>
          <button class="knd-link-btn" @click="goCompetition">
            <span>关联：{{ competition.short_name }}</span>
            <Icon name="arrow-right" :size="12" :stroke="1.8" />
          </button>
        </template>
      </div>

      <p v-if="news.summary" class="knd-lede">{{ news.summary }}</p>

      <div class="knd-rule"></div>

      <div class="knd-body">
        <p v-for="(para, idx) in news.content.split('\n')" :key="idx">{{ para }}</p>
      </div>

      <footer v-if="news.link" class="knd-foot">
        <a class="kal-btn" :href="news.link" target="_blank" rel="noopener">
          <Icon name="external-link" :size="13" />
          <span>阅读官方原文</span>
        </a>
      </footer>
    </article>
  </div>
</template>

<style scoped>
.knd { max-width: 760px; }
.knd-back {
  display: inline-flex; align-items: center; gap: 8px;
  background: transparent; border: 0; padding: 0;
  color: var(--kal-text-muted); cursor: pointer;
  margin-bottom: 24px; font-size: var(--kal-text-sm); letter-spacing: 1px;
}
.knd-back:hover { color: var(--kal-primary-700); }
.knd-notice { padding: 12px 16px; margin-bottom: 16px; color: var(--kal-text-muted); font-size: var(--kal-text-sm); }

.knd-article {
  background: var(--kal-paper);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
  padding: 56px 64px 64px;
}
.knd-kicker { color: var(--kal-primary-700); margin-bottom: 18px; }
.knd-title {
  font-family: var(--kal-font-serif);
  font-size: 36px; font-weight: 600;
  letter-spacing: 3px; line-height: 1.4;
  color: var(--kal-text-strong);
  margin-bottom: 18px;
}
.knd-meta {
  display: flex; align-items: center; flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
  margin-bottom: 24px;
}
.knd-meta-sep { width: 1px; height: 11px; background: var(--kal-border-strong); }
.knd-link-btn {
  display: inline-flex; align-items: center; gap: 4px;
  background: transparent; border: 0; padding: 0;
  color: var(--kal-text); font-size: 12px; cursor: pointer; letter-spacing: 1px;
}
.knd-link-btn:hover { color: var(--kal-primary-700); }

.knd-lede {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-lg);
  line-height: 1.85; letter-spacing: 1px;
  color: var(--kal-text-muted);
  font-style: italic;
  padding-left: 16px;
  border-left: 2px solid var(--kal-primary-600);
  margin-bottom: 24px;
}

.knd-rule {
  width: 80px; height: 1px;
  background: var(--kal-text-strong);
  margin-bottom: 28px;
}

.knd-body p {
  font-size: var(--kal-text-md);
  line-height: 2;
  color: var(--kal-text);
  letter-spacing: 0.5px;
  margin-bottom: 18px;
  text-align: justify;
}

.knd-foot {
  margin-top: 36px;
  padding-top: 28px;
  border-top: 1px dashed var(--kal-divider);
}

@media (max-width: 768px) {
  .knd-article { padding: 36px 24px 40px; }
  .knd-title { font-size: 26px; letter-spacing: 2px; }
}
</style>
