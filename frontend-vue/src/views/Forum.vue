<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { forumApi } from '@/api/projects'
import { normalizeForumPost } from '@/api/normalize'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'

const TOPIC_TAGS = ['找队友', '赛事问答', '经验分享', '资源互助']

const router = useRouter()
const auth = useAuthStore()
const tab = ref('latest')
const activeTopic = ref('')
const posts = ref([])
const loading = ref(false)
const error = ref('')
const showPostModal = ref(false)
const submitting = ref(false)
const postForm = ref({ title: '', topic: '找队友', content: '' })

const tabs = [
  { value: 'latest', label: '最新' },
  { value: 'hot', label: '热门' },
  { value: 'essence', label: '精华' }
]

const forumTopics = computed(() => TOPIC_TAGS.map(tag => ({
  tag,
  count: posts.value.filter(p => p.topic === tag).length
})))

const filtered = computed(() => {
  let list = [...posts.value]
  if (tab.value === 'hot') {
    list.sort((a, b) => (b.like_count + b.reply_count * 2) - (a.like_count + a.reply_count * 2))
  } else if (tab.value === 'essence') {
    list = list.filter(p => p.badges.includes('essence'))
  } else {
    list.sort((a, b) => {
      const ap = a.badges.includes('pinned') ? 1 : 0
      const bp = b.badges.includes('pinned') ? 1 : 0
      return bp - ap
    })
  }
  if (activeTopic.value) {
    list = list.filter(p => p.topic && activeTopic.value.includes(p.topic))
  }
  return list
})

function formatNum(n) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n
}
function pickTopic(t) { activeTopic.value = activeTopic.value === t.tag ? '' : t.tag }
function viewPost(p) {
  router.push({ name: 'forum-post-detail', params: { id: p.post_id } })
}
function newPost() {
  if (!auth.isLoggedIn) return router.push({ path: '/login', query: { redirect: '/forum' } })
  showPostModal.value = true
}

async function loadPosts() {
  loading.value = true
  error.value = ''
  try {
    const r = await forumApi.list({
      topic: activeTopic.value || undefined,
      page: 1,
      size: 50
    })
    posts.value = (r?.items || []).map(normalizeForumPost)
  } catch (e) {
    error.value = '帖子加载失败：' + (e.message || '后端不可用')
    posts.value = []
  } finally {
    loading.value = false
  }
}

async function submitPost() {
  if (!postForm.value.title.trim() || !postForm.value.content.trim()) {
    alert('请填写标题和正文')
    return
  }
  submitting.value = true
  try {
    await forumApi.create({
      title: postForm.value.title.trim(),
      topic: postForm.value.topic,
      content: postForm.value.content.trim(),
    })
    showPostModal.value = false
    postForm.value = { title: '', topic: '找队友', content: '' }
    await loadPosts()
  } catch (e) {
    alert(e.message || '发布失败，请确认已登录且后端已启动。')
  } finally {
    submitting.value = false
  }
}

onMounted(loadPosts)
</script>

<template>
  <div class="kal-container kfm">
    <header class="kfm-head">
      <div>
        <div class="kal-eyebrow">Salon&nbsp;&amp;&nbsp;Discourse</div>
        <h1 class="kfm-title">创坊论坛</h1>
        <p class="kfm-sub">同行者的公共留白——攻略、问答、复盘，皆在此处汇聚。</p>
      </div>
      <button class="kal-btn kfm-new" @click="newPost">
        <Icon name="edit" :size="14" />
        <span>发布新帖</span>
      </button>
    </header>

    <!-- Hot topics -->
    <section class="kfm-topics">
      <div class="kfm-topics-head">
        <span class="kal-eyebrow">Topics</span>
      </div>
      <div class="kfm-topics-list">
        <button
          v-for="t in forumTopics"
          :key="t.tag"
          class="kfm-topic"
          :class="{ 'kfm-topic--active': activeTopic === t.tag }"
          @click="pickTopic(t)"
        >
          <span>{{ t.tag }}</span>
          <span class="kfm-topic-count">{{ t.count }}</span>
        </button>
      </div>
    </section>

    <!-- Posts container -->
    <section class="kfm-posts">
      <div class="kfm-posts-head">
        <div class="kfm-tabs">
          <button
            v-for="t in tabs"
            :key="t.value"
            class="kfm-tab"
            :class="{ 'kfm-tab--active': tab === t.value }"
            @click="tab = t.value"
          >{{ t.label }}</button>
        </div>
        <div class="kfm-posts-info">
          <span class="kal-serial">共</span>
          <strong>{{ filtered.length }}</strong>
          <span class="kal-serial">篇</span>
        </div>
      </div>

      <div class="kfm-list">
        <div v-if="error" class="kfm-notice">{{ error }}</div>
        <div v-if="loading" class="kfm-notice">正在同步论坛帖子…</div>
        <article
          v-for="(p, idx) in filtered"
          :key="p.post_id"
          class="kfm-post"
          @click="viewPost(p)"
        >
          <span class="kfm-post-num">{{ String(idx + 1).padStart(2, '0') }}</span>
          <span class="kal-avatar">{{ p.author.initial }}</span>
          <div class="kfm-post-body">
            <div class="kfm-post-head">
              <h3 class="kfm-post-title">{{ p.title }}</h3>
              <div class="kfm-post-badges">
                <span v-if="p.badges.includes('pinned')" class="kfm-badge kfm-badge--pin">
                  <Icon name="pin" :size="10" />
                  <span>置顶</span>
                </span>
                <span v-if="p.badges.includes('essence')" class="kfm-badge kfm-badge--essence">精华</span>
                <span v-if="p.badges.includes('hot')" class="kfm-badge kfm-badge--hot">
                  <Icon name="flame" :size="10" />
                  <span>热门</span>
                </span>
                <span v-if="p.badges.includes('new')" class="kfm-badge kfm-badge--new">NEW</span>
              </div>
            </div>
            <div class="kfm-post-meta">
              <span class="kfm-post-author">{{ p.author.name }}</span>
              <span class="kfm-post-dot"></span>
              <span>{{ p.author.dept }}</span>
              <span class="kfm-post-dot"></span>
              <span>{{ p.last_reply_at }}</span>
            </div>
            <p class="kfm-post-excerpt">{{ p.excerpt }}</p>
            <div class="kfm-post-stats">
              <span class="kfm-post-stat">
                <Icon name="eye" :size="13" />
                <span>{{ formatNum(p.view_count) }}</span>
              </span>
              <span class="kfm-post-stat">
                <Icon name="message" :size="13" />
                <span>{{ formatNum(p.reply_count) }}</span>
              </span>
              <span class="kfm-post-stat">
                <Icon name="heart" :size="13" />
                <span>{{ formatNum(p.like_count) }}</span>
              </span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <Transition name="kal-page">
      <div v-if="showPostModal" class="kfm-modal" @click.self="showPostModal = false">
        <div class="kfm-modal-card">
          <header class="kfm-modal-head">
            <div>
              <div class="kal-eyebrow">New&nbsp;Post</div>
              <h3>发布新帖</h3>
            </div>
            <button class="kfm-icon-btn" @click="showPostModal = false">
              <Icon name="close" :size="16" />
            </button>
          </header>
          <div class="kfm-modal-body">
            <label class="kal-label kal-label-required">标题</label>
            <input class="kal-input" v-model="postForm.title" maxlength="80" placeholder="清晰说明你的问题或招募意图" />
            <label class="kal-label kal-label-required">话题</label>
            <select class="kal-select" v-model="postForm.topic">
              <option value="找队友">找队友</option>
              <option value="赛事问答">赛事问答</option>
              <option value="经验分享">经验分享</option>
              <option value="资源互助">资源互助</option>
            </select>
            <label class="kal-label kal-label-required">正文</label>
            <textarea class="kal-textarea" rows="7" v-model="postForm.content" maxlength="1200" placeholder="写下项目背景、需求、你希望得到的回应等"></textarea>
            <div class="kal-hint">{{ postForm.content.length }}/1200</div>
          </div>
          <footer class="kfm-modal-foot">
            <button class="kal-btn kal-btn-secondary" @click="showPostModal = false">取消</button>
            <button class="kal-btn" :disabled="submitting" @click="submitPost">{{ submitting ? '发布中…' : '发布' }}</button>
          </footer>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.kfm { max-width: 1100px; }
.kfm-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 28px; }
.kfm-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 14px; }
.kfm-title {
  font-family: var(--kal-font-serif);
  font-size: 44px;
  font-weight: 600;
  letter-spacing: 6px;
  color: var(--kal-text-strong);
  margin-bottom: 14px;
}
.kfm-sub {
  color: var(--kal-text-muted);
  letter-spacing: 0.5px;
  line-height: 1.85;
  max-width: 520px;
}
.kfm-new { letter-spacing: 1.5px; }

/* ---------- Topics ---------- */
.kfm-topics {
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  padding: 22px 28px;
  margin-bottom: 20px;
}
.kfm-topics-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 14px; }
.kfm-topics-list { display: flex; flex-wrap: wrap; gap: 8px; }
.kfm-topic {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px 6px 14px;
  background: transparent;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-full);
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  cursor: pointer;
  transition: all var(--kal-duration-2);
  letter-spacing: 0.5px;
}
.kfm-topic:hover { border-color: var(--kal-text); color: var(--kal-text); }
.kfm-topic--active {
  background: var(--kal-ink);
  color: #fff;
  border-color: var(--kal-ink);
}
.kfm-topic-count {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 11px;
  opacity: 0.6;
}
.kfm-topic--active .kfm-topic-count { opacity: 0.7; }

/* ---------- Posts ---------- */
.kfm-posts {
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  overflow: hidden;
}
.kfm-posts-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 28px;
  border-bottom: 1px solid var(--kal-divider);
}
.kfm-tabs { display: flex; gap: 28px; }
.kfm-tab {
  background: transparent;
  border: none;
  padding: 4px 0;
  font-size: var(--kal-text-md);
  color: var(--kal-text-muted);
  cursor: pointer;
  position: relative;
  transition: color var(--kal-duration-2);
  letter-spacing: 1px;
}
.kfm-tab:hover { color: var(--kal-text); }
.kfm-tab--active { color: var(--kal-text-strong); font-weight: 600; }
.kfm-tab--active::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: -19px;
  height: 1.5px;
  background: var(--kal-primary-600);
}
.kfm-posts-info {
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
}
.kfm-posts-info strong {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-lg);
  color: var(--kal-text-strong);
  font-weight: 600;
}

.kfm-list { display: flex; flex-direction: column; }
.kfm-notice {
  padding: 14px 28px;
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
  border-bottom: 1px solid var(--kal-divider);
}
.kfm-post {
  display: flex;
  gap: 16px;
  padding: 22px 28px;
  border-bottom: 1px solid var(--kal-divider);
  cursor: pointer;
  transition: background var(--kal-duration-2);
  align-items: flex-start;
}
.kfm-post:hover { background: var(--kal-bg-subtle); }
.kfm-post:last-child { border-bottom: none; }
.kfm-post-num {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 13px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
  margin-top: 8px;
  width: 24px;
  flex-shrink: 0;
}
.kfm-post-body { flex: 1; min-width: 0; }
.kfm-post-head { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.kfm-post-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-lg);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 1px;
  line-height: 1.5;
  flex: 1;
  min-width: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--kal-duration-2);
}
.kfm-post:hover .kfm-post-title { color: var(--kal-primary-700); }
.kfm-post-badges { display: flex; gap: 5px; }
.kfm-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  letter-spacing: 1px;
  padding: 2px 7px;
  border-radius: var(--kal-radius-xs);
  font-weight: 500;
}
.kfm-badge--pin     { background: var(--kal-bg-subtle); color: var(--kal-text); }
.kfm-badge--essence { background: var(--kal-ink); color: #fff; }
.kfm-badge--hot     { background: var(--kal-primary-50); color: var(--kal-primary-700); }
.kfm-badge--new     { background: var(--kal-text); color: #fff; }
.kfm-post-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: var(--kal-text-subtle);
  margin-bottom: 10px;
  letter-spacing: 0.5px;
  align-items: center;
}
.kfm-post-author { font-weight: 500; color: var(--kal-text-muted); }
.kfm-post-dot { width: 3px; height: 3px; border-radius: 50%; background: currentColor; }
.kfm-post-excerpt {
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  line-height: 1.75;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.kfm-post-stats { display: flex; gap: 22px; font-size: var(--kal-text-xs); color: var(--kal-text-subtle); }
.kfm-post-stat { display: inline-flex; align-items: center; gap: 5px; }

.kfm-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(34, 24, 20, 0.45);
  backdrop-filter: blur(2px);
}
.kfm-modal-card {
  width: 100%;
  max-width: 620px;
  max-height: 90vh;
  overflow: hidden;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  display: flex;
  flex-direction: column;
}
.kfm-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--kal-divider);
}
.kfm-modal-head h3 {
  font-family: var(--kal-font-serif);
  font-size: 20px;
  letter-spacing: 3px;
  color: var(--kal-text-strong);
}
.kfm-modal-body {
  padding: 20px 24px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kfm-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--kal-divider);
}
.kfm-icon-btn {
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  border-radius: var(--kal-radius-sm);
  color: var(--kal-text-muted);
  cursor: pointer;
}
.kfm-icon-btn:hover { background: var(--kal-bg); color: var(--kal-text-strong); }

@media (max-width: 768px) {
  .kfm-head { flex-direction: column; align-items: flex-start; }
  .kfm-title { font-size: 30px; letter-spacing: 3px; }
  .kfm-post { padding: 18px 20px; gap: 12px; }
  .kfm-post-num { display: none; }
  .kfm-posts-head { padding: 16px 20px; }
}
</style>
