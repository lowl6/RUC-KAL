<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  workspaceApi,
  TICKET_CATEGORY_LABEL, TICKET_STATUSES,
} from '@/api/workspace'
import Icon from '@/components/Icon.vue'

const auth = useAuthStore()

const filters = ref({ scope: 'all', status: '' })
const list = ref([])
const total = ref(0)
const page = ref(1)
const stats = ref({ open: 0, in_progress: 0, resolved: 0, closed: 0 })
const loading = ref(false)
const error = ref('')

async function load () {
  loading.value = true; error.value = ''
  try {
    const r = await workspaceApi.staffTickets({
      scope: filters.value.scope || 'all',
      status: filters.value.status || undefined,
      page: page.value, size: 20
    })
    list.value = r.items
    total.value = r.total
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
async function loadStats () {
  try { stats.value = await workspaceApi.staffStats() }
  catch (e) {}
}
onMounted(async () => { await Promise.all([load(), loadStats()]) })
watch(filters, () => { page.value = 1; load() }, { deep: true })

/* 工单详情面板 */
const activeId = ref('')
const activeDetail = ref(null)
const replyDraft = ref('')
const sending = ref(false)
let scrollEl = null
const setScrollRef = (el) => { scrollEl = el || null }

async function open (t) {
  activeId.value = t.ticketId
  activeDetail.value = null
  try {
    activeDetail.value = await workspaceApi.ticketDetail(t.ticketId)
    await nextTick()
    if (scrollEl) scrollEl.scrollTo({ top: scrollEl.scrollHeight, behavior: 'smooth' })
  } catch (e) { error.value = e.message }
}
async function reply () {
  if (!replyDraft.value.trim()) return
  sending.value = true
  try {
    await workspaceApi.reply(activeId.value, replyDraft.value.trim())
    replyDraft.value = ''
    activeDetail.value = await workspaceApi.ticketDetail(activeId.value)
    await nextTick()
    if (scrollEl) scrollEl.scrollTo({ top: scrollEl.scrollHeight, behavior: 'smooth' })
    await Promise.all([load(), loadStats()])
  } catch (e) { alert(e.message || '发送失败') }
  finally { sending.value = false }
}
async function setStatus (status) {
  if (!confirm(`将工单标记为「${TICKET_STATUSES[status]?.label}」？`)) return
  try {
    await workspaceApi.setTicketStatus(activeId.value, status)
    activeDetail.value = await workspaceApi.ticketDetail(activeId.value)
    await Promise.all([load(), loadStats()])
  } catch (e) { alert(e.message || '操作失败') }
}

function fmtTime (s) { return s ? String(s).replace('T', ' ').slice(5, 16) : '' }
function meId () { return auth.me?.userId || auth.me?.user_id }
const isMine = computed(() => activeDetail.value?.ticket?.assigneeStaffId === meId())
</script>

<template>
  <div class="kts">
    <header class="kts-head">
      <div>
        <div class="kal-eyebrow">Console / Tickets</div>
        <h1 class="kts-title">工单工作台</h1>
        <p class="kts-sub">承接学生项目的求助：导师推荐、经费支持、物资场地协调。第一个查看工单的工作人员将自动认领。</p>
      </div>
      <div class="kts-stats">
        <div class="kts-stat is-urgent">
          <dt>待响应</dt>
          <dd>{{ stats.open }}</dd>
        </div>
        <div class="kts-stat is-doing">
          <dt>处理中</dt>
          <dd>{{ stats.in_progress }}</dd>
        </div>
        <div class="kts-stat">
          <dt>已解决</dt>
          <dd>{{ stats.resolved }}</dd>
        </div>
        <div class="kts-stat is-mute">
          <dt>已关闭</dt>
          <dd>{{ stats.closed }}</dd>
        </div>
      </div>
    </header>

    <div class="kts-body">
      <!-- 左侧列表 -->
      <aside class="kts-list">
        <div class="kts-filter">
          <div class="kts-tabs">
            <button class="kts-tab" :class="{ 'is-active': filters.scope === 'all' }"  @click="filters.scope = 'all'">全部</button>
            <button class="kts-tab" :class="{ 'is-active': filters.scope === 'mine' }" @click="filters.scope = 'mine'">我处理的</button>
          </div>
          <select class="kal-input kts-status" v-model="filters.status">
            <option value="">所有状态</option>
            <option value="open">待响应</option>
            <option value="in_progress">处理中</option>
            <option value="resolved">已解决</option>
            <option value="closed">已关闭</option>
          </select>
        </div>

        <div v-if="error" class="kts-alert">{{ error }}</div>

        <ul v-if="list.length" class="kts-items">
          <li
            v-for="t in list"
            :key="t.ticketId"
            class="kts-item"
            :class="{ 'is-active': activeId === t.ticketId, 'is-pending': t.status === 'open' }"
            @click="open(t)"
          >
            <div class="kts-item-top">
              <span class="kts-item-cat">{{ TICKET_CATEGORY_LABEL[t.category] || t.category }}</span>
              <span class="kts-item-status">
                <span class="kts-dot" :class="`kts-dot--${TICKET_STATUSES[t.status]?.dot || 'closed'}`"></span>
                {{ TICKET_STATUSES[t.status]?.label || t.status }}
              </span>
            </div>
            <div class="kts-item-subject">{{ t.subject }}</div>
            <div class="kts-item-ws">
              <Icon name="briefcase" :size="11" />
              <span>{{ t.workspaceTitle }}</span>
            </div>
            <div class="kts-item-meta">
              <span>{{ t.openerName }}</span>
              <span class="kts-sep">·</span>
              <span v-if="t.assigneeStaffName">由 {{ t.assigneeStaffName }} 处理</span>
              <span v-else class="kts-pending">待认领</span>
              <span class="kts-sep">·</span>
              <span>{{ fmtTime(t.lastActivityAt) }}</span>
            </div>
          </li>
        </ul>
        <div v-else-if="!loading" class="kts-empty">当前筛选下暂无工单</div>
        <div v-if="loading" class="kts-empty">加载中…</div>

        <div class="kts-pager">
          <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page <= 1" @click="page--; load()">上一页</button>
          <span>第 {{ page }} 页 · {{ total }}</span>
          <button class="kal-btn kal-btn-sm kal-btn-secondary" :disabled="page * 20 >= total" @click="page++; load()">下一页</button>
        </div>
      </aside>

      <!-- 右侧详情 -->
      <section class="kts-detail">
        <template v-if="!activeDetail">
          <div class="kts-detail-empty">
            <Icon name="message" :size="36" />
            <h3>选择左侧工单查看会话</h3>
            <p>第一个查看 / 回复未分配工单的工作人员将自动成为该工单的处理人。</p>
          </div>
        </template>
        <template v-else>
          <header class="kts-detail-head">
            <div>
              <div class="kts-detail-eyebrow">
                <span class="kts-cat-pill">{{ TICKET_CATEGORY_LABEL[activeDetail.ticket.category] }}</span>
                <span class="kts-sep">·</span>
                <span>{{ activeDetail.ticket.workspaceTitle || activeDetail.workspaceTitle }}</span>
              </div>
              <h2 class="kts-detail-subject">{{ activeDetail.ticket.subject }}</h2>
              <div class="kts-detail-meta">
                <span>发起 · {{ activeDetail.ticket.openerName }}</span>
                <span class="kts-sep">·</span>
                <span>处理 · {{ activeDetail.ticket.assigneeStaffName || '尚未认领' }}</span>
                <span class="kts-sep">·</span>
                <span>创建于 {{ fmtTime(activeDetail.ticket.createdAt) }}</span>
              </div>
            </div>
            <div class="kts-detail-status">
              <span class="kts-dot" :class="`kts-dot--${TICKET_STATUSES[activeDetail.ticket.status]?.dot}`"></span>
              <span>{{ TICKET_STATUSES[activeDetail.ticket.status]?.label }}</span>
            </div>
          </header>

          <div class="kts-thread" :ref="setScrollRef">
            <div
              v-for="m in activeDetail.messages"
              :key="m.id"
              class="kts-msg"
              :class="m.authorId === meId() ? 'is-self' : (m.authorRole === 'staff' || m.authorRole === 'admin' || m.authorRole === 'super_admin' ? 'is-staff' : 'is-them')"
            >
              <span class="kal-avatar kal-avatar-sm">{{ (m.authorName || '·')[0] }}</span>
              <div class="kts-msg-body">
                <div class="kts-msg-head">
                  <span>{{ m.authorName }}</span>
                  <span v-if="m.authorRole === 'staff' || m.authorRole === 'admin' || m.authorRole === 'super_admin'" class="kts-msg-tag">工作人员</span>
                  <span class="kts-msg-time">{{ fmtTime(m.createdAt) }}</span>
                </div>
                <div class="kts-msg-content">{{ m.content }}</div>
              </div>
            </div>
          </div>

          <footer v-if="activeDetail.ticket.status !== 'closed'" class="kts-input">
            <textarea class="kal-input" rows="3" v-model="replyDraft"
                      placeholder="回复学生（按 Ctrl+Enter 发送）"
                      @keydown.ctrl.enter.prevent="reply"></textarea>
            <div class="kts-input-actions">
              <div class="kts-input-status">
                <button v-if="activeDetail.ticket.status === 'in_progress'" class="kts-link-btn" @click="setStatus('resolved')">标记为已解决</button>
                <button v-if="activeDetail.ticket.status !== 'closed'" class="kts-link-btn kts-link-danger" @click="setStatus('closed')">关闭工单</button>
              </div>
              <button class="kal-btn kal-btn-sm" :disabled="sending || !replyDraft.trim()" @click="reply">
                <Icon name="message" :size="13" :stroke="2" />
                <span>{{ sending ? '发送中…' : (isMine ? '发送' : '认领并回复') }}</span>
              </button>
            </div>
          </footer>
          <div v-else class="kts-input-closed">本工单已关闭。</div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.kts { display: flex; flex-direction: column; gap: 22px; height: calc(100vh - 116px); }

.kts-head {
  display: flex; justify-content: space-between; align-items: flex-end;
  gap: 24px; flex-wrap: wrap;
}
.kts-title {
  font-family: var(--kal-font-serif);
  font-size: 28px; font-weight: 600;
  letter-spacing: 4px; color: var(--kal-text-strong);
  margin: 6px 0 4px;
}
.kts-sub { color: var(--kal-text-muted); font-size: 13px; max-width: 600px; line-height: 1.7; }
.kts-stats { display: flex; gap: 18px; }
.kts-stat {
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  padding: 12px 18px;
  min-width: 96px;
}
.kts-stat dt { font-size: 10.5px; letter-spacing: 1.5px; color: var(--kal-text-subtle); text-transform: uppercase; }
.kts-stat dd {
  font-family: var(--kal-font-serif); font-size: 22px; font-weight: 600;
  color: var(--kal-text-strong); margin: 4px 0 0; letter-spacing: 0.5px;
}
.kts-stat.is-urgent { border-left: 2px solid var(--kal-primary-600); }
.kts-stat.is-urgent dd { color: var(--kal-primary-700); }
.kts-stat.is-doing { border-left: 2px solid #4a8c5b; }
.kts-stat.is-mute  { background: var(--kal-bg-subtle); }

.kts-body {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 18px;
  flex: 1; min-height: 0;
}

/* ---------- 列表 ---------- */
.kts-list {
  display: flex; flex-direction: column;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  overflow: hidden;
}
.kts-filter {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--kal-border);
}
.kts-tabs {
  display: inline-flex;
  background: var(--kal-bg-subtle);
  border-radius: var(--kal-radius-sm);
  padding: 2px;
  flex: 1;
}
.kts-tab {
  flex: 1;
  padding: 6px 10px;
  background: transparent; border: 0;
  border-radius: var(--kal-radius-xs);
  font-size: 12px; letter-spacing: 0.5px;
  color: var(--kal-text-muted);
  cursor: pointer;
  transition: all .15s;
}
.kts-tab.is-active { background: var(--kal-ink); color: #fff; }
.kts-status { width: 120px; flex-shrink: 0; padding: 6px 10px; font-size: 12px; }

.kts-alert {
  margin: 8px 14px;
  padding: 8px 12px;
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  border-left: 2px solid var(--kal-primary-600);
  font-size: 12px;
  border-radius: var(--kal-radius-sm);
}

.kts-items { flex: 1; list-style: none; padding: 0; margin: 0; overflow: auto; }
.kts-item {
  padding: 12px 14px;
  border-bottom: 1px solid var(--kal-divider);
  cursor: pointer;
  transition: background .15s;
  position: relative;
}
.kts-item:hover { background: var(--kal-bg-subtle); }
.kts-item.is-active {
  background: var(--kal-primary-50);
  border-left: 2px solid var(--kal-primary-600);
  padding-left: 12px;
}
.kts-item.is-pending::after {
  content: '';
  position: absolute;
  top: 14px; right: 12px;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--kal-primary-600);
}
.kts-item-top {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 6px;
}
.kts-item-cat {
  font-size: 10.5px; letter-spacing: 1px;
  padding: 1px 8px;
  background: var(--kal-bg-subtle);
  border-radius: 999px;
  color: var(--kal-text-muted);
  text-transform: uppercase;
}
.kts-item-status {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 10.5px; color: var(--kal-text-muted);
  letter-spacing: 0.5px; text-transform: uppercase;
}
.kts-item-subject {
  font-size: 13.5px; color: var(--kal-text-strong); font-weight: 500;
  margin-bottom: 4px; letter-spacing: 0.3px; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.kts-item-ws {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11.5px; color: var(--kal-text-muted);
  letter-spacing: 0.3px; margin-bottom: 4px;
}
.kts-item-meta {
  display: inline-flex; flex-wrap: wrap; gap: 4px;
  font-size: 11px; color: var(--kal-text-subtle);
  letter-spacing: 0.3px;
}
.kts-pending { color: var(--kal-primary-700); font-weight: 500; }
.kts-sep { color: var(--kal-gray-300); }

.kts-empty { padding: 32px 20px; text-align: center; color: var(--kal-text-subtle); font-size: 12px; }
.kts-pager {
  padding: 10px 14px;
  display: flex; align-items: center; justify-content: space-between;
  font-size: 11.5px; color: var(--kal-text-subtle);
  border-top: 1px solid var(--kal-divider);
}
.kts-pager > span { letter-spacing: 0.3px; }

/* ---------- 详情 ---------- */
.kts-detail {
  display: flex; flex-direction: column;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  overflow: hidden;
}
.kts-detail-empty {
  flex: 1;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 64px 32px;
  color: var(--kal-text-subtle);
  text-align: center;
  gap: 14px;
}
.kts-detail-empty :deep(.kal-icon) { color: var(--kal-gray-300); }
.kts-detail-empty h3 {
  font-family: var(--kal-font-serif);
  font-size: 18px; font-weight: 600;
  letter-spacing: 2px; color: var(--kal-text-strong);
  margin: 0;
}
.kts-detail-empty p { font-size: 13px; line-height: 1.7; max-width: 360px; margin: 0; }

.kts-detail-head {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: 14px; padding: 22px 28px 16px;
  border-bottom: 1px solid var(--kal-divider);
}
.kts-detail-eyebrow {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11px; color: var(--kal-text-subtle);
  letter-spacing: 1px; text-transform: uppercase;
  margin-bottom: 8px; flex-wrap: wrap;
}
.kts-cat-pill {
  padding: 2px 8px;
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: none;
}
.kts-detail-subject {
  font-family: var(--kal-font-serif);
  font-size: 19px; font-weight: 600;
  letter-spacing: 1.5px; color: var(--kal-text-strong);
  margin: 0 0 8px;
}
.kts-detail-meta {
  display: inline-flex; flex-wrap: wrap; gap: 6px;
  font-size: 12px; color: var(--kal-text-subtle);
  letter-spacing: 0.3px;
}
.kts-detail-status {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11px; letter-spacing: 1px;
  color: var(--kal-text-muted); text-transform: uppercase;
  flex-shrink: 0;
}

.kts-thread {
  flex: 1;
  display: flex; flex-direction: column; gap: 14px;
  padding: 22px 28px;
  overflow: auto;
  background: var(--kal-bg);
}
.kts-msg { display: flex; gap: 10px; align-items: flex-start; }
.kts-msg.is-self { flex-direction: row-reverse; }
.kts-msg.is-self .kts-msg-body { align-items: flex-end; }
.kts-msg-body { display: flex; flex-direction: column; gap: 4px; max-width: 75%; }
.kts-msg-head { display: inline-flex; align-items: center; gap: 6px; font-size: 11.5px; color: var(--kal-text-subtle); }
.kts-msg-head > span:first-child { font-weight: 500; color: var(--kal-text-strong); }
.kts-msg-tag {
  display: inline-block; padding: 1px 6px;
  font-size: 10px; letter-spacing: 1px;
  background: var(--kal-primary-50); color: var(--kal-primary-700);
  border-radius: var(--kal-radius-xs); font-weight: 600;
}
.kts-msg-time { color: var(--kal-text-subtle); }
.kts-msg-content {
  padding: 10px 14px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  font-size: 13.5px; line-height: 1.7;
  color: var(--kal-text); letter-spacing: 0.3px;
  white-space: pre-wrap; word-break: break-word;
}
.kts-msg.is-self .kts-msg-content {
  background: var(--kal-ink); color: #fff; border-color: var(--kal-ink);
}
.kts-msg.is-staff .kts-msg-content {
  background: var(--kal-primary-50); border-color: var(--kal-primary-200);
  color: var(--kal-primary-900);
}

.kts-input {
  padding: 14px 28px 18px;
  border-top: 1px solid var(--kal-divider);
  display: flex; flex-direction: column; gap: 10px;
}
.kts-input textarea { resize: vertical; line-height: 1.7; }
.kts-input-actions {
  display: flex; justify-content: space-between; align-items: center; gap: 10px;
}
.kts-input-status { display: flex; gap: 8px; }
.kts-link-btn {
  background: transparent; border: 0;
  padding: 4px 10px;
  font-size: 12px; letter-spacing: 0.5px;
  color: var(--kal-primary-700); border-radius: 4px;
  cursor: pointer; transition: background .15s;
}
.kts-link-btn:hover { background: var(--kal-primary-50); }
.kts-link-danger { color: #b53a2a; }
.kts-link-danger:hover { background: rgba(181, 58, 42, 0.08); }
.kts-input-closed {
  margin: 14px 28px 18px;
  padding: 10px 14px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text-subtle);
  border-radius: var(--kal-radius-sm);
  font-size: 12px; letter-spacing: 0.5px; text-align: center;
}

.kts-dot {
  display: inline-block; width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--kal-text-subtle);
}
.kts-dot--recruiting { background: #4a8c5b; }
.kts-dot--completed  { background: var(--kal-sand); }
.kts-dot--closed     { background: var(--kal-gray-400); }
.kts-dot--urgent     { background: var(--kal-primary-600); }

@media (max-width: 1024px) {
  .kts-body { grid-template-columns: 1fr; }
  .kts { height: auto; }
  .kts-list, .kts-detail { min-height: 400px; }
}
</style>
