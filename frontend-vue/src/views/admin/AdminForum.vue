<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api/admin'
import Icon from '@/components/Icon.vue'

const filter = reactive({ keyword: '', topic: '', status: '' })
const page = ref(1)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

async function load () {
  loading.value = true; error.value = ''
  try {
    const r = await adminApi.listPosts({
      keyword: filter.keyword || undefined,
      topic: filter.topic || undefined,
      status: filter.status || undefined,
      page: page.value, size: 20
    })
    list.value = r.items; total.value = r.total
  } catch (e) { error.value = e.message }
  finally   { loading.value = false }
}
onMounted(load)
async function search () { page.value = 1; await load() }

async function setStatus (p, status) {
  const reason = status === 'hidden' || status === 'deleted'
    ? prompt(`将「${p.title}」状态置为 ${status}？请填写原因：`) : null
  if ((status === 'hidden' || status === 'deleted') && reason === null) return
  await adminApi.updatePostStatus(p.postId, status, reason)
  await load()
}
async function pin (p, pinned) {
  await adminApi.pinPost(p.postId, pinned)
  await load()
}

const showComments = ref(false)
const activePost = ref(null)
const comments = ref([])
async function viewComments (p) {
  activePost.value = p
  showComments.value = true
  comments.value = []
  try { comments.value = await adminApi.listComments(p.postId) }
  catch (e) { alert(e.message || '评论加载失败') }
}
async function removeComment (c) {
  if (!confirm('确定删除这条评论？')) return
  await adminApi.removeComment(c.id)
  comments.value = comments.value.filter(x => x.id !== c.id)
}

function statusLabel (s) { return ({ published: '已发布', hidden: '已隐藏', deleted: '已删除' })[s] || s }
</script>

<template>
  <div class="ad-page">
    <header class="ad-page-head">
      <div>
        <div class="kal-eyebrow">Console / Forum</div>
        <h1 class="ad-page-title">论坛治理</h1>
      </div>
    </header>

    <div class="ad-filter">
      <input class="kal-input" v-model="filter.keyword" placeholder="按标题/正文搜索" @keyup.enter="search" />
      <input class="kal-input" v-model="filter.topic" placeholder="话题（如：找队友）" @keyup.enter="search" />
      <select class="kal-input" v-model="filter.status" @change="search">
        <option value="">全部状态</option>
        <option value="published">已发布</option>
        <option value="hidden">已隐藏</option>
        <option value="deleted">已删除</option>
      </select>
      <button class="kal-btn kal-btn-secondary" @click="search"><Icon name="search" :size="14" /> 搜索</button>
    </div>

    <div v-if="error" class="ad-alert">{{ error }}</div>

    <div class="ad-table-wrap">
      <table class="ad-table">
        <thead>
          <tr>
            <th>帖子</th><th>话题</th><th>作者</th><th>互动</th><th>状态</th>
            <th style="width: 220px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="ad-empty">加载中…</td></tr>
          <tr v-else-if="!list.length"><td colspan="6" class="ad-empty">暂无帖子</td></tr>
          <tr v-for="p in list" :key="p.postId">
            <td>
              <div class="ad-name">
                <Icon v-if="p.pinned" name="pin" :size="13" />
                {{ p.title }}
              </div>
              <div class="ad-sub">{{ p.excerpt }}</div>
            </td>
            <td><span class="ad-tag">{{ p.topic || '—' }}</span></td>
            <td>{{ p.authorId }}</td>
            <td>{{ p.viewCount }} 看 · {{ p.replyCount }} 回 · {{ p.likeCount }} 赞</td>
            <td><span class="ad-dot" :class="`is-${p.status}`"></span>{{ statusLabel(p.status) }}</td>
            <td class="ad-ops">
              <button class="ad-link-btn" @click="viewComments(p)">评论</button>
              <button class="ad-link-btn" @click="pin(p, !p.pinned)">{{ p.pinned ? '取消置顶' : '置顶' }}</button>
              <button v-if="p.status !== 'hidden'" class="ad-link-btn" @click="setStatus(p, 'hidden')">隐藏</button>
              <button v-else class="ad-link-btn" @click="setStatus(p, 'published')">恢复</button>
              <button v-if="p.status !== 'deleted'" class="ad-link-btn ad-link-danger" @click="setStatus(p, 'deleted')">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="ad-pager">
      <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="ad-pager-info">第 {{ page }} 页 · 共 {{ total }} 条</span>
      <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page * 20 >= total" @click="page++; load()">下一页</button>
    </div>

    <Transition name="kal-page">
      <div v-if="showComments" class="ad-modal" @click.self="showComments = false">
        <div class="ad-modal-card ad-modal-card--lg">
          <header class="ad-modal-head">
            <h3>评论管理 · {{ activePost?.title }}</h3>
            <button class="ad-icon-btn" @click="showComments = false"><Icon name="close" :size="16" /></button>
          </header>
          <div class="ad-modal-body">
            <div v-if="!comments.length" class="ad-empty">暂无评论</div>
            <div v-for="c in comments" :key="c.id" class="ad-cm">
              <div class="ad-cm-head">
                <strong>{{ c.authorName || c.authorId }}</strong>
                <span class="ad-cm-time">{{ (c.createdAt || '').slice(0, 16).replace('T', ' ') }}</span>
                <button class="ad-link-btn ad-link-danger" @click="removeComment(c)">删除</button>
              </div>
              <p>{{ c.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped src="./admin-shared.css"></style>
<style scoped>
.ad-modal-card--lg { max-width: 720px; }
.ad-cm { padding: 14px 0; border-bottom: 1px dashed var(--kal-divider); }
.ad-cm:last-child { border-bottom: 0; }
.ad-cm-head { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.ad-cm-head strong { font-size: 13.5px; color: var(--kal-text-strong); }
.ad-cm-time { font-size: 11.5px; color: var(--kal-text-subtle); flex: 1; }
.ad-cm p { font-size: 13.5px; color: var(--kal-text); line-height: 1.7; white-space: pre-wrap; }
</style>
