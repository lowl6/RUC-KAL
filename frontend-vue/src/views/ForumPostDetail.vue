<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { forumApi, reportsApi } from '@/api/projects'
import { normalizeForumPost, normalizeComment } from '@/api/normalize'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const post = ref(null)
const comments = ref([])
const liked = ref(false)
const loading = ref(false)
const error = ref('')
const draft = ref('')
const submitting = ref(false)
const liking = ref(false)

async function load () {
  loading.value = true; error.value = ''
  try {
    const r = await forumApi.detail(route.params.id)
    post.value = normalizeForumPost(r.post)
    comments.value = (r.comments || []).map(normalizeComment)
    liked.value = !!r.liked
  } catch (e) {
    error.value = '帖子加载失败：' + (e.message || '请稍后重试')
    post.value = null
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function toggleLike () {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!post.value) return
  liking.value = true
  try {
    const r = await forumApi.like(post.value.post_id)
    liked.value = !!r.liked
    post.value.like_count = r.likeCount
  } catch (e) {
    alert(e.message || '操作失败')
  } finally {
    liking.value = false
  }
}

async function submitComment () {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  const text = draft.value.trim()
  if (!text) return
  submitting.value = true
  try {
    const c = await forumApi.comment(post.value.post_id, text)
    comments.value.push(normalizeComment(c))
    if (post.value) post.value.reply_count = (post.value.reply_count || 0) + 1
    draft.value = ''
  } catch (e) {
    alert(e.message || '评论发布失败')
  } finally {
    submitting.value = false
  }
}

async function reportPost () {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!post.value?.post_id) return
  const reason = prompt('请填写举报原因（例如：人身攻击、广告灌水、违规内容）')
  if (reason == null) return
  if (!reason.trim()) {
    alert('请填写举报原因')
    return
  }
  try {
    await reportsApi.create({ targetType: 'forum_post', targetId: post.value.post_id, reason: reason.trim() })
    alert('已提交举报，管理员会尽快处理。')
  } catch (e) {
    alert(e.message || '举报失败，请稍后重试。')
  }
}

function back () { router.back() }

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="kal-container kfd">
    <button class="kfd-back" @click="back">
      <Icon name="arrow-left" :size="14" :stroke="1.8" />
      <span>返回论坛</span>
    </button>

    <div v-if="error" class="kal-card kfd-notice">{{ error }}</div>
    <div v-if="loading" class="kal-card kfd-notice">正在读取帖子…</div>

    <article v-if="post" class="kfd-article">
      <div class="kfd-meta">
        <span class="kfd-topic">{{ post.topic || '综合' }}</span>
        <span v-if="post.pinned" class="kfd-badge">置顶</span>
        <span v-if="post.essence" class="kfd-badge kfd-badge--essence">精华</span>
        <span class="kfd-meta-sep"></span>
        <span>{{ post.last_reply_at }}</span>
        <span class="kfd-meta-sep"></span>
        <span>浏览 {{ post.view_count || 0 }}</span>
      </div>

      <h1 class="kfd-title">{{ post.title }}</h1>

      <div class="kfd-author">
        <span class="kal-avatar">{{ post.author.initial }}</span>
        <div>
          <div class="kfd-author-name">{{ post.author.name }}</div>
          <div class="kfd-author-role">楼主 · {{ post.author.dept || '人大' }}</div>
        </div>
      </div>

      <div class="kfd-rule"></div>

      <div class="kfd-body">
        <p v-for="(para, idx) in post.content.split('\n')" :key="idx">{{ para }}</p>
      </div>

      <div class="kfd-stats">
        <button class="kfd-action" :class="{ 'kfd-action--liked': liked }" :disabled="liking" @click="toggleLike">
          <Icon name="heart" :size="14" />
          <span>{{ liked ? '已点赞' : '点赞' }}</span>
          <strong>{{ post.like_count || 0 }}</strong>
        </button>
        <button class="kfd-action" @click="reportPost">
          <Icon name="alert" :size="14" />
          <span>举报</span>
        </button>
        <div class="kfd-action kfd-action--info">
          <Icon name="message" :size="14" />
          <span>回复</span>
          <strong>{{ post.reply_count || comments.length || 0 }}</strong>
        </div>
      </div>
    </article>

    <section v-if="post" class="kal-card kfd-comments">
      <header class="kfd-comments-head">
        <div>
          <span class="kal-eyebrow">Comments</span>
          <h2><span class="kal-serial">i.</span><span>讨论 / {{ comments.length }}</span></h2>
        </div>
      </header>

      <div v-if="!comments.length" class="kfd-empty">暂无评论，等你来开个头。</div>

      <ul class="kfd-comment-list">
        <li v-for="(c, idx) in comments" :key="c.id" class="kfd-comment">
          <span class="kfd-comment-num">{{ String(idx + 1).padStart(2, '0') }}</span>
          <span class="kal-avatar kal-avatar-sm">{{ c.author_initial }}</span>
          <div class="kfd-comment-body">
            <div class="kfd-comment-row1">
              <span class="kfd-comment-name">{{ c.author_name }}</span>
              <span class="kfd-comment-time">{{ c.created_at }}</span>
            </div>
            <p>{{ c.content }}</p>
          </div>
        </li>
      </ul>

      <footer class="kfd-input">
        <textarea
          class="kal-textarea kfd-textarea"
          v-model="draft"
          rows="3"
          maxlength="800"
          placeholder="写下你的看法，对楼主或上方评论的回应也欢迎"
        ></textarea>
        <div class="kfd-input-foot">
          <span class="kal-hint">{{ draft.length }}/800 · 友善表达，禁止泄露他人隐私</span>
          <button class="kal-btn" :disabled="!draft.trim() || submitting" @click="submitComment">
            {{ submitting ? '发布中…' : '发表评论' }}
          </button>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.kfd { max-width: 860px; }
.kfd-back {
  display: inline-flex; align-items: center; gap: 8px;
  background: transparent; border: 0; padding: 0;
  color: var(--kal-text-muted); cursor: pointer;
  margin-bottom: 24px; font-size: var(--kal-text-sm); letter-spacing: 1px;
}
.kfd-back:hover { color: var(--kal-primary-700); }
.kfd-notice { padding: 12px 16px; margin-bottom: 16px; color: var(--kal-text-muted); font-size: var(--kal-text-sm); }

.kfd-article {
  background: var(--kal-paper);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
  padding: 48px 52px;
  margin-bottom: 24px;
}

.kfd-meta {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  font-size: 11px; color: var(--kal-text-subtle);
  letter-spacing: 1.5px; text-transform: uppercase;
  margin-bottom: 18px;
}
.kfd-topic { color: var(--kal-text); font-weight: 500; padding: 3px 10px; background: var(--kal-bg-subtle); border-radius: var(--kal-radius-xs); letter-spacing: 1px; }
.kfd-badge {
  background: var(--kal-bg-subtle); color: var(--kal-text);
  padding: 3px 8px; font-size: 10px; letter-spacing: 1px;
  border-radius: var(--kal-radius-xs); font-weight: 500;
}
.kfd-badge--essence { background: var(--kal-ink); color: #fff; }
.kfd-meta-sep { width: 1px; height: 11px; background: var(--kal-border-strong); }

.kfd-title {
  font-family: var(--kal-font-serif);
  font-size: 32px; font-weight: 600;
  letter-spacing: 3px; line-height: 1.45;
  color: var(--kal-text-strong);
  margin-bottom: 22px;
}

.kfd-author {
  display: flex; align-items: center; gap: 12px;
  padding-bottom: 22px;
}
.kfd-author-name {
  font-weight: 600; font-size: var(--kal-text-md);
  color: var(--kal-text-strong); letter-spacing: 0.5px;
}
.kfd-author-role { font-size: 11px; color: var(--kal-text-subtle); margin-top: 3px; letter-spacing: 1px; }

.kfd-rule { width: 60px; height: 1px; background: var(--kal-text-strong); margin-bottom: 28px; }

.kfd-body p {
  font-size: var(--kal-text-md);
  line-height: 2; letter-spacing: 0.5px;
  color: var(--kal-text); margin-bottom: 16px;
  text-align: justify;
}

.kfd-stats {
  display: flex; gap: 16px; align-items: center;
  margin-top: 28px; padding-top: 20px;
  border-top: 1px dashed var(--kal-divider);
}
.kfd-action {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-full);
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
  letter-spacing: 1px;
  cursor: pointer;
  transition: all var(--kal-duration-2);
}
.kfd-action:hover { border-color: var(--kal-primary-600); color: var(--kal-primary-700); }
.kfd-action--liked { background: var(--kal-primary-50); border-color: var(--kal-primary-600); color: var(--kal-primary-700); }
.kfd-action--info { cursor: default; }
.kfd-action--info:hover { border-color: var(--kal-border); color: var(--kal-text-muted); }
.kfd-action strong {
  font-family: var(--kal-font-serif); font-weight: 600;
  color: var(--kal-text-strong); margin-left: 2px;
}
.kfd-action--liked strong { color: var(--kal-primary-700); }

/* comments */
.kfd-comments { padding: 32px 36px; }
.kfd-comments-head { margin-bottom: 22px; }
.kfd-comments-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 8px; }
.kfd-comments-head h2 {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-2xl);
  font-weight: 600; letter-spacing: 3px;
  color: var(--kal-text-strong);
  display: inline-flex; align-items: baseline; gap: 12px;
}
.kfd-comments-head h2 .kal-serial { font-size: 14px; color: var(--kal-text-subtle); }

.kfd-empty {
  text-align: center; padding: 28px 12px;
  color: var(--kal-text-subtle); font-size: var(--kal-text-sm);
  font-family: var(--kal-font-serif); font-style: italic; letter-spacing: 1px;
}

.kfd-comment-list { display: flex; flex-direction: column; }
.kfd-comment {
  display: flex; gap: 14px; align-items: flex-start;
  padding: 18px 0; border-bottom: 1px solid var(--kal-divider);
}
.kfd-comment:last-child { border-bottom: 0; }
.kfd-comment-num {
  font-family: var(--kal-font-serif); font-style: italic;
  font-size: 12px; color: var(--kal-text-subtle); letter-spacing: 1px;
  margin-top: 4px; width: 22px; flex-shrink: 0;
}
.kfd-comment-body { flex: 1; min-width: 0; }
.kfd-comment-row1 {
  display: flex; align-items: baseline; gap: 12px;
  margin-bottom: 6px;
}
.kfd-comment-name {
  font-weight: 600; font-size: var(--kal-text-sm);
  color: var(--kal-text-strong); letter-spacing: 0.5px;
}
.kfd-comment-time { font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 1px; }
.kfd-comment-body p {
  font-size: var(--kal-text-sm); color: var(--kal-text);
  line-height: 1.85; letter-spacing: 0.3px;
  white-space: pre-wrap;
}

/* input */
.kfd-input {
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px dashed var(--kal-divider);
}
.kfd-textarea { width: 100%; resize: vertical; }
.kfd-input-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 12px; gap: 12px;
}
.kfd-input-foot .kal-hint { font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 0.5px; }

@media (max-width: 768px) {
  .kfd-article { padding: 32px 22px; }
  .kfd-title { font-size: 24px; letter-spacing: 2px; }
  .kfd-comments { padding: 22px 20px; }
}
</style>
