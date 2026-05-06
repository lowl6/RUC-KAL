<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api/admin'
import Icon from '@/components/Icon.vue'

const list = ref([])
const competitions = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const search = ref('')

const showEdit = ref(false)
const showImport = ref(false)
const importing = ref(false)
const importText = ref('')
const importCompetitionId = ref('')
const importResult = ref(null)

const blank = () => ({
  newsId: null,
  competitionId: '',
  title: '',
  source: '',
  summary: '',
  content: '',
  link: '',
  coverUrl: '',
  status: 'published',
  sortOrder: 0,
  publishAt: new Date().toISOString().slice(0, 16)
})
const editing = reactive(blank())

async function load () {
  loading.value = true; error.value = ''
  try {
    const r = await adminApi.listNews({ keyword: search.value || undefined, page: 1, size: 100 })
    list.value = r?.items || []
    if (!competitions.value.length) {
      try { competitions.value = await adminApi.listCompetitions() } catch {}
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)

function open (n) {
  Object.assign(editing, blank())
  if (!n) return
  Object.assign(editing, {
    newsId: n.newsId,
    competitionId: n.competitionId || '',
    title: n.title || '',
    source: n.source || '',
    summary: n.summary || '',
    content: n.content || '',
    link: n.link || '',
    coverUrl: n.coverUrl || '',
    status: n.status || 'published',
    sortOrder: n.sortOrder ?? 0,
    publishAt: (n.publishAt || new Date().toISOString()).slice(0, 16)
  })
  showEdit.value = true
}
function close () { showEdit.value = false }

async function save () {
  if (!editing.title.trim()) { alert('请填写标题'); return }
  saving.value = true
  try {
    const payload = { ...editing }
    if (!payload.competitionId) payload.competitionId = null
    if (payload.publishAt && payload.publishAt.length === 16) payload.publishAt = payload.publishAt + ':00'
    await adminApi.upsertNews(payload)
    showEdit.value = false
    await load()
  } catch (e) {
    alert(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove (n) {
  if (!confirm(`确定删除「${n.title}」？`)) return
  await adminApi.deleteNews(n.newsId)
  await load()
}

function openImport () {
  importText.value = ''
  importCompetitionId.value = ''
  importResult.value = null
  showImport.value = true
}
function closeImport () {
  if (importing.value) return
  showImport.value = false
}

const importUrls = computed(() =>
  (importText.value || '')
    .split(/[\r\n]+/)
    .map(s => s.trim())
    .filter(s => s)
)

async function doImport () {
  const urls = importUrls.value
  if (!urls.length) { alert('请粘贴至少一条链接'); return }
  importing.value = true
  importResult.value = null
  try {
    const r = await adminApi.importNewsLinks({
      urls,
      competitionId: importCompetitionId.value || null
    })
    importResult.value = r
    if ((r?.created + r?.updated) > 0) await load()
  } catch (e) {
    alert(e.message || '同步失败')
  } finally {
    importing.value = false
  }
}

const compName = (id) => competitions.value.find(c => c.competitionId === id)?.name || '综合'
const statusLabel = (s) => s === 'hidden' ? '已下架' : '已发布'
</script>

<template>
  <div class="ad-page">
    <header class="ad-page-head">
      <div>
        <div class="kal-eyebrow">Console / News</div>
        <h1 class="ad-page-title">资讯发布</h1>
        <p class="ad-page-sub">为「比赛中心 · 相关资讯」与各赛事详情页提供专题动态。</p>
      </div>
      <div class="ad-page-actions">
        <button class="kal-btn kal-btn-secondary" @click="openImport" title="粘贴公众号链接，自动同步标题 / 来源 / 封面 / 发布时间">
          <Icon name="link" :size="14" /> 公众号链接同步
        </button>
        <button class="kal-btn kal-btn-primary" @click="open(null)">
          <Icon name="plus" :size="14" /> 新增资讯
        </button>
      </div>
    </header>

    <div v-if="error" class="ad-alert">{{ error }}</div>

    <div class="ad-filter">
      <input class="kal-input" v-model="search" placeholder="搜索：标题 / 摘要" @keyup.enter="load" />
      <button class="kal-btn kal-btn-secondary" @click="load">
        <Icon name="search" :size="13" /><span>检索</span>
      </button>
    </div>

    <div class="ad-table-wrap">
      <table class="ad-table">
        <thead>
          <tr>
            <th>标题</th><th>关联赛事</th><th>来源</th><th>发布时间</th><th>状态</th>
            <th style="width: 200px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="ad-empty">加载中…</td></tr>
          <tr v-else-if="!list.length"><td colspan="6" class="ad-empty">暂无资讯</td></tr>
          <tr v-for="n in list" :key="n.newsId">
            <td>
              <div class="ad-name">{{ n.title }}</div>
              <div class="ad-sub">{{ n.summary }}</div>
            </td>
            <td><span class="ad-tag">{{ compName(n.competitionId) }}</span></td>
            <td>{{ n.source || '—' }}</td>
            <td class="ad-mono">{{ (n.publishAt || '').slice(0, 16).replace('T', ' ') }}</td>
            <td><span class="ad-dot" :class="n.status === 'hidden' ? 'is-hidden' : 'is-published'"></span>{{ statusLabel(n.status) }}</td>
            <td class="ad-ops">
              <button class="ad-link-btn" @click="open(n)">编辑</button>
              <button class="ad-link-btn ad-link-danger" @click="remove(n)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ============ 公众号链接同步 ============ -->
    <Transition name="kal-page">
      <div v-if="showImport" class="ad-modal" @click.self="closeImport">
        <div class="ad-modal-card ad-modal-card--lg">
          <header class="ad-modal-head">
            <h3>公众号链接同步</h3>
            <button class="ad-icon-btn" @click="closeImport"><Icon name="close" :size="16" /></button>
          </header>
          <div class="ad-modal-body">
            <div class="imp-intro">
              <Icon name="info" :size="14" />
              <div>
                <div class="imp-intro-title">把公众号文章地址（<code>https://mp.weixin.qq.com/s/...</code>）每行一个粘贴进来</div>
                <div class="imp-intro-sub">系统会自动抓取标题、来源公众号、封面与发布时间；点击后跳转原文，正文不会被复制到平台。重复链接会更新而不会创建副本。</div>
              </div>
            </div>

            <div class="kal-field">
              <label class="kal-label kal-label-required">链接（每行一条）</label>
              <textarea class="kal-input ad-news-content" rows="6"
                v-model="importText"
                placeholder="https://mp.weixin.qq.com/s/xxxxxxxxxxxxx
https://mp.weixin.qq.com/s/yyyyyyyyyyyyy"></textarea>
              <div class="kal-hint">已识别 {{ importUrls.length }} 条</div>
            </div>

            <div class="kal-field">
              <label class="kal-label">默认关联赛事（可空，留空入「综合资讯」）</label>
              <select class="kal-input" v-model="importCompetitionId">
                <option value="">— 不关联，作为综合资讯 —</option>
                <option v-for="c in competitions" :key="c.competitionId" :value="c.competitionId">
                  {{ c.name }}
                </option>
              </select>
            </div>

            <div v-if="importResult" class="imp-result">
              <div class="imp-result-stat">
                <div><span class="imp-num imp-num-ok">{{ importResult.created }}</span><span>新增</span></div>
                <div><span class="imp-num">{{ importResult.updated }}</span><span>更新</span></div>
                <div><span class="imp-num imp-num-bad">{{ (importResult.failed || []).length }}</span><span>失败</span></div>
              </div>

              <div v-if="(importResult.items || []).length" class="imp-list">
                <div class="imp-list-title">已同步</div>
                <div v-for="it in importResult.items" :key="it.newsId" class="imp-item">
                  <div class="imp-item-head">
                    <span class="imp-item-title">{{ it.title }}</span>
                    <span class="ad-tag">{{ it.source || '微信公众号' }}</span>
                  </div>
                  <a class="imp-item-link" :href="it.link" target="_blank" rel="noopener">{{ it.link }}</a>
                </div>
              </div>

              <div v-if="(importResult.failed || []).length" class="imp-list imp-list--fail">
                <div class="imp-list-title">失败</div>
                <div v-for="(f, i) in importResult.failed" :key="i" class="imp-item">
                  <div class="imp-item-head"><span class="imp-item-title">{{ f.url }}</span></div>
                  <div class="imp-item-reason">{{ f.reason }}</div>
                </div>
              </div>
            </div>
          </div>
          <footer class="ad-modal-foot">
            <button class="kal-btn kal-btn-secondary" :disabled="importing" @click="closeImport">关闭</button>
            <button class="kal-btn kal-btn-primary" :disabled="importing || !importUrls.length" @click="doImport">
              <Icon v-if="!importing" name="refresh" :size="14" />
              <span>{{ importing ? '同步中…' : `同步 ${importUrls.length || ''} 条` }}</span>
            </button>
          </footer>
        </div>
      </div>
    </Transition>

    <Transition name="kal-page">
      <div v-if="showEdit" class="ad-modal" @click.self="close">
        <div class="ad-modal-card ad-modal-card--lg">
          <header class="ad-modal-head">
            <h3>{{ editing.newsId ? '编辑资讯' : '发布新资讯' }}</h3>
            <button class="ad-icon-btn" @click="close"><Icon name="close" :size="16" /></button>
          </header>
          <div class="ad-modal-body">
            <div class="kal-field">
              <label class="kal-label kal-label-required">标题</label>
              <input class="kal-input" v-model="editing.title" placeholder="一句话讲清楚资讯主题" />
            </div>
            <div class="kal-grid-2">
              <div class="kal-field">
                <label class="kal-label">关联赛事（可空）</label>
                <select class="kal-input" v-model="editing.competitionId">
                  <option value="">综合资讯</option>
                  <option v-for="c in competitions" :key="c.competitionId" :value="c.competitionId">
                    {{ c.name }}
                  </option>
                </select>
              </div>
              <div class="kal-field">
                <label class="kal-label">来源</label>
                <input class="kal-input" v-model="editing.source" placeholder="如：教务处 / 校团委" />
              </div>
            </div>
            <div class="kal-field">
              <label class="kal-label">摘要</label>
              <textarea class="kal-input" rows="2" v-model="editing.summary"
                placeholder="一段话简要概述资讯内容（用于列表预览）"></textarea>
            </div>
            <div class="kal-field">
              <label class="kal-label kal-label-required">正文</label>
              <textarea class="kal-input ad-news-content" rows="10" v-model="editing.content"
                placeholder="支持多行文本。每段一行，自动分段。"></textarea>
              <div class="kal-hint">{{ (editing.content || '').length }} / 4000</div>
            </div>
            <div class="kal-grid-2">
              <div class="kal-field">
                <label class="kal-label">外部原文链接</label>
                <input class="kal-input" v-model="editing.link" placeholder="https://..." />
              </div>
              <div class="kal-field">
                <label class="kal-label">封面图片 URL（可选）</label>
                <input class="kal-input" v-model="editing.coverUrl" placeholder="https://..." />
              </div>
            </div>
            <div class="kal-grid-2">
              <div class="kal-field">
                <label class="kal-label">发布时间</label>
                <input class="kal-input" type="datetime-local" v-model="editing.publishAt" />
              </div>
              <div class="kal-field">
                <label class="kal-label">状态</label>
                <select class="kal-input" v-model="editing.status">
                  <option value="published">已发布</option>
                  <option value="hidden">下架（不可见）</option>
                </select>
              </div>
            </div>
          </div>
          <footer class="ad-modal-foot">
            <button class="kal-btn kal-btn-secondary" @click="close">取消</button>
            <button class="kal-btn kal-btn-primary" :disabled="saving" @click="save">
              {{ saving ? '保存中…' : '保存' }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped src="./admin-shared.css"></style>
<style scoped>
.ad-modal-card--lg { max-width: 760px; }
.ad-news-content { font-family: ui-monospace, "SF Mono", Consolas, monospace; font-size: 13px; line-height: 1.7; }

.ad-page-actions { display: flex; gap: 10px; align-items: center; }

.imp-intro {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 14px;
  background: var(--kal-primary-50);
  border: 1px solid var(--kal-primary-200);
  border-radius: var(--kal-radius-sm);
  color: var(--ruc-red-dark);
  margin-bottom: 6px;
}
.imp-intro-title { font-weight: 600; font-size: 13.5px; letter-spacing: 0.3px; }
.imp-intro-title code {
  padding: 0 5px; font-size: 12px;
  background: rgba(255,255,255,.6);
  border-radius: 3px;
  font-family: var(--kal-font-mono);
}
.imp-intro-sub {
  margin-top: 4px;
  font-size: 12.5px; line-height: 1.7;
  color: var(--kal-text-muted); letter-spacing: 0.3px;
}

.imp-result {
  margin-top: 10px;
  padding: 16px 18px;
  background: var(--kal-bg-subtle);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
}
.imp-result-stat {
  display: flex; gap: 28px; flex-wrap: wrap;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--kal-border);
}
.imp-result-stat > div {
  display: flex; flex-direction: column; gap: 2px;
  font-size: 11.5px; letter-spacing: 1.5px; color: var(--kal-text-subtle);
}
.imp-num {
  font-family: var(--kal-font-serif);
  font-weight: 600; font-size: 22px;
  letter-spacing: 0; color: var(--kal-text-strong);
}
.imp-num-ok  { color: var(--ruc-red); }
.imp-num-bad { color: var(--kal-danger); }

.imp-list { margin-top: 14px; display: flex; flex-direction: column; gap: 8px; }
.imp-list-title {
  font-family: var(--kal-font-serif);
  font-size: 12px; letter-spacing: 2px;
  color: var(--kal-text-subtle); text-transform: uppercase;
}
.imp-item {
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--kal-border);
  border-left: 2px solid var(--kal-primary-600);
  border-radius: var(--kal-radius-xs);
}
.imp-list--fail .imp-item { border-left-color: var(--kal-danger); }
.imp-item-head { display: flex; align-items: center; gap: 8px; }
.imp-item-title {
  font-size: 13.5px; color: var(--kal-text-strong); letter-spacing: 0.3px;
  flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.imp-item-link {
  display: block; margin-top: 4px;
  font-family: var(--kal-font-mono); font-size: 11.5px;
  color: var(--kal-text-muted);
  word-break: break-all;
}
.imp-item-link:hover { color: var(--ruc-red); }
.imp-item-reason {
  margin-top: 2px;
  font-size: 12px; color: var(--kal-danger); letter-spacing: 0.3px;
}
</style>
