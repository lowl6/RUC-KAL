<script setup>
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  workspaceApi,
  PHASES, PHASE_LABEL,
  STATUSES as WS_STATUSES,
  TICKET_CATEGORIES, TICKET_CATEGORY_LABEL, TICKET_STATUSES,
  MILESTONE_STATUSES,
} from '@/api/workspace'
import { competitionsApi } from '@/api/projects'
import Icon from '@/components/Icon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const workspaceId = computed(() => route.params.id)
const detail = ref(null)
const loading = ref(false)
const error = ref('')
const staffList = ref([])
const competitions = ref([])

const isOwner = computed(() => detail.value?.myRole === 'owner')
const linkedCompetition = computed(() =>
  competitions.value.find(c =>
    c.shortName === detail.value?.competitionShort || c.name === detail.value?.competitionShort
  ) || null
)
const linkedTimeline = computed(() => {
  const c = linkedCompetition.value
  if (!c) return []
  return [
    { label: '报名开始', date: c.registerStart, state: stepState(c.registerStart) },
    { label: '报名截止', date: c.registerEnd, state: stepState(c.registerEnd) },
    { label: '赛事日程', date: c.scheduleNote || '查看通知', state: 'note' },
  ]
})

async function load () {
  loading.value = true; error.value = ''
  try {
    const [d, staff, cs] = await Promise.all([
      workspaceApi.detail(workspaceId.value),
      workspaceApi.staffMembers().catch(() => []),
      competitionsApi.list().catch(() => []),
    ])
    detail.value = d
    staffList.value = staff || []
    competitions.value = cs || []
  } catch (e) {
    error.value = e.message || '加载失败'
    detail.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(workspaceId, load)

/* ============== 编辑：项目元信息（owner 专属） ============== */
const editing = ref(false)
const editForm = ref({})
function openEdit () {
  editForm.value = {
    title: detail.value.title,
    summary: detail.value.summary,
    competitionShort: detail.value.competitionShort,
    competitionTarget: detail.value.competitionTarget,
    assignedStaffId: detail.value.assignedStaffId || '',
    phase: detail.value.phase,
    status: detail.value.status,
  }
  editing.value = true
}
async function saveEdit () {
  try {
    await workspaceApi.update(workspaceId.value, editForm.value)
    editing.value = false
    await load()
  } catch (e) { alert(e.message || '保存失败') }
}

async function assignStaff (staffId) {
  try {
    await workspaceApi.assignStaff(workspaceId.value, staffId || '')
    await load()
  } catch (e) { alert(e.message || '保存工作人员失败') }
}

/* ============== 进度滑块（任意成员都可推动） ============== */
const progressDraft = ref(0)
const phaseDraft = ref('idea')
watch(detail, (v) => {
  if (v) { progressDraft.value = v.progress; phaseDraft.value = v.phase }
})
const progressDirty = computed(() =>
  detail.value && (
    Number(progressDraft.value) !== Number(detail.value.progress) ||
    phaseDraft.value !== detail.value.phase
  )
)
async function pushProgress () {
  try {
    await workspaceApi.patchProgress(workspaceId.value, {
      progress: Number(progressDraft.value),
      phase: phaseDraft.value,
    })
    await load()
  } catch (e) { alert(e.message || '更新失败') }
}

/* ============== 成员管理 ============== */
const memberFormOpen = ref(false)
const newMember = ref({ email: '', roleNote: '' })
async function addMember () {
  if (!newMember.value.email.trim()) return
  try {
    await workspaceApi.addMember(workspaceId.value, {
      email: newMember.value.email.trim(),
      roleNote: newMember.value.roleNote.trim(),
    })
    newMember.value = { email: '', roleNote: '' }
    memberFormOpen.value = false
    await load()
  } catch (e) { alert(e.message || '邀请失败') }
}
async function removeMember (uid, name) {
  if (!confirm(`移除成员「${name}」？`)) return
  try {
    await workspaceApi.removeMember(workspaceId.value, uid)
    await load()
  } catch (e) { alert(e.message || '操作失败') }
}

/* ============== 里程碑 ============== */
const msEditing = ref(null) // 当前编辑的 id；'new' 表示新建
const msDraft = ref({})
function startNewMilestone () {
  msEditing.value = 'new'
  const next = (detail.value?.milestones?.length || 0) + 1
  msDraft.value = { title: '', dueDate: '', status: 'pending', sortOrder: next, note: '' }
}
function startEditMilestone (m) {
  msEditing.value = m.id
  msDraft.value = { ...m, dueDate: m.dueDate ? m.dueDate.slice(0, 10) : '' }
}
async function saveMilestone () {
  if (!msDraft.value.title?.trim()) return
  try {
    if (msEditing.value === 'new') {
      await workspaceApi.createMilestone(workspaceId.value, msDraft.value)
    } else {
      await workspaceApi.updateMilestone(workspaceId.value, msEditing.value, msDraft.value)
    }
    msEditing.value = null
    await load()
  } catch (e) { alert(e.message || '保存失败') }
}
async function removeMilestone (m) {
  if (!confirm(`删除里程碑「${m.title}」？`)) return
  try {
    await workspaceApi.deleteMilestone(workspaceId.value, m.id)
    await load()
  } catch (e) { alert(e.message || '删除失败') }
}
async function toggleMilestone (m) {
  const nextStatus = m.status === 'done' ? 'doing' : 'done'
  try {
    await workspaceApi.updateMilestone(workspaceId.value, m.id, {
      title: m.title, note: m.note, dueDate: m.dueDate, sortOrder: m.sortOrder,
      status: nextStatus,
    })
    await load()
  } catch (e) { alert(e.message || '操作失败') }
}

/* ============== 工单（求助会话） ============== */
const newTicketOpen = ref(false)
const newTicket = ref({ subject: '', category: 'advisor', firstMessage: '' })
async function openTicket () {
  if (!newTicket.value.subject.trim() || !newTicket.value.firstMessage.trim()) return
  try {
    await workspaceApi.openTicket(workspaceId.value, {
      subject: newTicket.value.subject.trim(),
      category: newTicket.value.category,
      firstMessage: newTicket.value.firstMessage.trim(),
    })
    newTicket.value = { subject: '', category: 'advisor', firstMessage: '' }
    newTicketOpen.value = false
    await load()
  } catch (e) { alert(e.message || '提交失败') }
}

/* ----- 工单内联会话窗（点击工单条目展开会话） ----- */
const activeTicketId = ref('')
const activeTicket = ref(null)
const replyDraft = ref('')
const replying = ref(false)
let scrollEl = null
const setScrollRef = (el) => { scrollEl = el || null }
const scrollThreadToBottom = () => {
  if (scrollEl) scrollEl.scrollTo({ top: scrollEl.scrollHeight, behavior: 'smooth' })
}

async function openTicketThread (ticketId) {
  if (activeTicketId.value === ticketId) {
    activeTicketId.value = ''
    activeTicket.value = null
    return
  }
  activeTicketId.value = ticketId
  activeTicket.value = null
  try {
    activeTicket.value = await workspaceApi.ticketDetail(ticketId)
    await nextTick()
    scrollThreadToBottom()
  } catch (e) {
    error.value = e.message || '加载工单失败'
  }
}
async function sendReply () {
  if (!replyDraft.value.trim() || !activeTicketId.value) return
  replying.value = true
  try {
    await workspaceApi.reply(activeTicketId.value, replyDraft.value.trim())
    replyDraft.value = ''
    activeTicket.value = await workspaceApi.ticketDetail(activeTicketId.value)
    await nextTick()
    scrollThreadToBottom()
    await load()
  } catch (e) { alert(e.message || '发送失败') }
  finally { replying.value = false }
}
async function changeTicketStatus (newStatus) {
  if (!confirm(`将本工单标记为「${TICKET_STATUSES[newStatus]?.label || newStatus}」？`)) return
  try {
    await workspaceApi.setTicketStatus(activeTicketId.value, newStatus)
    activeTicket.value = await workspaceApi.ticketDetail(activeTicketId.value)
    await load()
  } catch (e) { alert(e.message || '操作失败') }
}

function fmtTime (s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(5, 16)
}
function fmtDate (s) {
  if (!s) return '—'
  return String(s).slice(0, 10)
}
function daysLeft (dateLike) {
  if (!dateLike) return null
  const d = new Date(dateLike)
  if (Number.isNaN(d.getTime())) return null
  return Math.ceil((d.getTime() - Date.now()) / 86400000)
}
function stepState (dateLike) {
  const left = daysLeft(dateLike)
  if (left == null) return 'muted'
  if (left < 0) return 'done'
  if (left <= 7) return 'urgent'
  return 'open'
}
function compStatusLabel (s) {
  return ({ upcoming: '即将开放', active: '报名中', urgent: '即将截止', ended: '已结束' })[s] || s
}
function authorClass (role) {
  if (role === 'staff' || role === 'admin' || role === 'super_admin') return 'is-staff'
  return 'is-self'
}
function meId () { return auth.me?.userId || auth.me?.user_id }
</script>

<template>
  <div class="kal-container kwd">
    <button class="kwd-back" @click="router.push('/me?tab=workspaces')">
      <Icon name="arrow-right" :size="13" :stroke="1.8" /><span>返回我的项目</span>
    </button>

    <div v-if="loading" class="kal-card kwd-loading">加载中…</div>
    <div v-else-if="error" class="kal-card kwd-error">{{ error }}</div>

    <template v-else-if="detail">
      <!-- Hero -->
      <header class="kwd-hero">
        <div class="kwd-hero-top">
          <div class="kwd-hero-eyebrow">
            <span class="kwd-tag">{{ PHASE_LABEL[detail.phase] }}</span>
            <span class="kwd-sep">/</span>
            <span>{{ detail.competitionShort || '自由项目' }}</span>
            <span v-if="detail.competitionTarget" class="kwd-sep">/</span>
            <span v-if="detail.competitionTarget">目标 · {{ detail.competitionTarget }}</span>
          </div>
          <span class="kmc-status">
            <span class="kmc-status-dot" :class="`kmc-status-dot--${WS_STATUSES[detail.status]?.dot || 'closed'}`"></span>
            <span>{{ WS_STATUSES[detail.status]?.label || detail.status }}</span>
          </span>
        </div>

        <div class="kwd-hero-mid">
          <div>
            <h1 class="kwd-title">{{ detail.title }}</h1>
            <p class="kwd-summary">{{ detail.summary || '暂无简述' }}</p>
          </div>
          <button v-if="isOwner" class="kal-btn kal-btn-sm kal-btn-secondary" @click="openEdit">
            <Icon name="edit" :size="13" /><span>编辑信息</span>
          </button>
        </div>

        <div class="kwd-hero-foot">
          <div class="kwd-hero-stat">
            <dt>项目负责人</dt>
            <dd>{{ detail.ownerName }}</dd>
          </div>
          <div class="kwd-hero-stat">
            <dt>对接工作人员</dt>
            <dd v-if="!isOwner">{{ detail.assignedStaffName || '尚未分配' }}</dd>
            <select v-else class="kwd-staff-select" :value="detail.assignedStaffId || ''" @change="assignStaff($event.target.value)">
              <option value="">暂不指定</option>
              <option v-for="s in staffList" :key="s.userId" :value="s.userId">
                {{ s.displayName }} · {{ s.deptName || '工作人员' }}
              </option>
            </select>
          </div>
          <div class="kwd-hero-stat">
            <dt>成员</dt>
            <dd>{{ detail.members.length }} 人</dd>
          </div>
          <div class="kwd-hero-stat">
            <dt>最近更新</dt>
            <dd>{{ fmtTime(detail.updatedAt) }}</dd>
          </div>
        </div>
      </header>

      <section v-if="linkedCompetition" class="kal-card kwd-comp-card">
        <header class="kwd-comp-head">
          <div>
            <span class="kal-eyebrow">Competition Timeline</span>
            <h2>{{ linkedCompetition.name }}</h2>
            <p>{{ linkedCompetition.description || '暂无赛事说明' }}</p>
          </div>
          <div class="kwd-comp-badge">
            <strong>{{ compStatusLabel(linkedCompetition.status) }}</strong>
            <span v-if="daysLeft(linkedCompetition.registerEnd) !== null">
              {{ daysLeft(linkedCompetition.registerEnd) >= 0 ? `距截止 ${daysLeft(linkedCompetition.registerEnd)} 天` : '报名已结束' }}
            </span>
          </div>
        </header>
        <div class="kwd-comp-timeline">
          <div v-for="s in linkedTimeline" :key="s.label" class="kwd-comp-step" :class="`is-${s.state}`">
            <span class="kwd-comp-dot"></span>
            <div>
              <strong>{{ s.label }}</strong>
              <small>{{ s.date || '待定' }}</small>
            </div>
          </div>
        </div>
        <footer v-if="linkedCompetition.officialLinks?.length" class="kwd-comp-links">
          <a v-for="l in linkedCompetition.officialLinks" :key="l.url || l.label" :href="l.url" target="_blank" rel="noopener">
            {{ l.label || '官方链接' }}
          </a>
        </footer>
      </section>

      <!-- 进度 -->
      <section class="kal-card kwd-card kwd-progress">
        <header class="kwd-card-head">
          <span class="kal-eyebrow">Progress</span>
          <h2>整体进度</h2>
        </header>
        <div class="kwd-progress-row">
          <input type="range" min="0" max="100" step="5" v-model="progressDraft" />
          <span class="kwd-progress-num">{{ progressDraft }}%</span>
        </div>
        <div class="kwd-phase-row">
          <span class="kwd-phase-label">阶段</span>
          <div class="kwd-phase-pills">
            <button
              v-for="p in PHASES"
              :key="p.value"
              type="button"
              class="kwd-phase-pill"
              :class="{ 'is-active': phaseDraft === p.value }"
              @click="phaseDraft = p.value"
            >{{ p.label }}</button>
          </div>
        </div>
        <Transition name="kal-page">
          <div v-if="progressDirty" class="kwd-progress-actions">
            <button class="kal-btn kal-btn-sm" @click="pushProgress">
              <Icon name="check" :size="13" :stroke="2" />
              <span>保存进度</span>
            </button>
            <button class="kal-btn kal-btn-sm kal-btn-ghost"
                    @click="progressDraft = detail.progress; phaseDraft = detail.phase">
              撤销
            </button>
          </div>
        </Transition>
      </section>

      <!-- 里程碑 -->
      <section class="kal-card kwd-card">
        <header class="kwd-card-head">
          <span class="kal-eyebrow">Milestones</span>
          <h2>里程碑</h2>
          <button class="kal-btn kal-btn-sm" @click="startNewMilestone">
            <Icon name="plus" :size="13" :stroke="2" /><span>新增</span>
          </button>
        </header>

        <ul class="kwd-milestones" v-if="detail.milestones.length">
          <li
            v-for="m in detail.milestones"
            :key="m.id"
            class="kwd-milestone"
            :class="`is-${m.status}`"
          >
            <button
              type="button"
              class="kwd-ms-toggle"
              :class="`is-${m.status}`"
              @click="toggleMilestone(m)"
              :title="MILESTONE_STATUSES[m.status]?.label"
            >
              <Icon v-if="m.status === 'done'" name="check" :size="12" :stroke="2.4" />
            </button>
            <div class="kwd-ms-body">
              <div class="kwd-ms-title">{{ m.title }}</div>
              <div class="kwd-ms-meta">
                <span>{{ MILESTONE_STATUSES[m.status]?.label || m.status }}</span>
                <span class="kwd-sep">·</span>
                <span>截止 {{ fmtDate(m.dueDate) }}</span>
                <span v-if="m.note" class="kwd-sep">·</span>
                <span v-if="m.note" class="kwd-ms-note">{{ m.note }}</span>
              </div>
            </div>
            <div class="kwd-ms-ops">
              <button class="kwd-link-btn" @click="startEditMilestone(m)">编辑</button>
              <button class="kwd-link-btn kwd-link-danger" @click="removeMilestone(m)">删除</button>
            </div>
          </li>
        </ul>
        <div v-else class="kwd-empty-row">还没有里程碑，先把"接下来 30 天最重要的 3 件事"写下来吧。</div>

        <!-- 行内编辑 -->
        <Transition name="kal-page">
          <div v-if="msEditing" class="kwd-ms-editor">
            <div class="kwd-ms-editor-grid">
              <div class="kal-field">
                <label class="kal-label kal-label-required">标题</label>
                <input class="kal-input" v-model="msDraft.title" placeholder="如：原型 V1 完成" />
              </div>
              <div class="kal-field">
                <label class="kal-label">截止日期</label>
                <input class="kal-input" type="date" v-model="msDraft.dueDate" />
              </div>
              <div class="kal-field">
                <label class="kal-label">状态</label>
                <select class="kal-input" v-model="msDraft.status">
                  <option value="pending">未开始</option>
                  <option value="doing">进行中</option>
                  <option value="done">已完成</option>
                </select>
              </div>
              <div class="kal-field">
                <label class="kal-label">排序</label>
                <input class="kal-input" type="number" v-model="msDraft.sortOrder" />
              </div>
            </div>
            <div class="kal-field">
              <label class="kal-label">备注（可空）</label>
              <input class="kal-input" v-model="msDraft.note" placeholder='一句话写清楚怎么算 "完成"' maxlength="200" />
            </div>
            <div class="kwd-ms-editor-actions">
              <button class="kal-btn kal-btn-sm kal-btn-secondary" @click="msEditing = null">取消</button>
              <button class="kal-btn kal-btn-sm" @click="saveMilestone">
                <Icon name="check" :size="13" :stroke="2" /><span>保存</span>
              </button>
            </div>
          </div>
        </Transition>
      </section>

      <!-- 成员 -->
      <section class="kal-card kwd-card">
        <header class="kwd-card-head">
          <span class="kal-eyebrow">Members</span>
          <h2>项目成员</h2>
          <button v-if="isOwner" class="kal-btn kal-btn-sm" @click="memberFormOpen = !memberFormOpen">
            <Icon name="plus" :size="13" :stroke="2" /><span>邀请成员</span>
          </button>
        </header>

        <Transition name="kal-page">
          <div v-if="memberFormOpen && isOwner" class="kwd-member-form">
            <div class="kal-grid-2">
              <div class="kal-field">
                <label class="kal-label kal-label-required">校内邮箱</label>
                <input class="kal-input" v-model="newMember.email" placeholder="ta 必须先在平台注册账号" />
              </div>
              <div class="kal-field">
                <label class="kal-label">在项目中的角色</label>
                <input class="kal-input" v-model="newMember.roleNote" placeholder="如：前端 / 商业策划" />
              </div>
            </div>
            <div class="kwd-ms-editor-actions">
              <button class="kal-btn kal-btn-sm kal-btn-secondary" @click="memberFormOpen = false">取消</button>
              <button class="kal-btn kal-btn-sm" @click="addMember">
                <Icon name="plus" :size="13" :stroke="2" /><span>加入项目</span>
              </button>
            </div>
          </div>
        </Transition>

        <ul class="kwd-members">
          <li v-for="m in detail.members" :key="m.userId" class="kwd-member">
            <span class="kal-avatar kal-avatar-sm">{{ (m.displayName || '同')[0] }}</span>
            <div class="kwd-member-body">
              <div class="kwd-member-line">
                <span class="kwd-member-name">{{ m.displayName }}</span>
                <span v-if="m.role === 'owner'" class="kwd-member-tag is-owner">负责人</span>
                <span v-else-if="m.roleNote" class="kwd-member-tag">{{ m.roleNote }}</span>
              </div>
              <div class="kwd-member-sub">
                <span>{{ m.deptName || '—' }}</span>
                <span v-if="m.grade" class="kwd-sep">·</span>
                <span v-if="m.grade">{{ m.grade }}</span>
                <span class="kwd-sep">·</span>
                <span class="kwd-member-email">{{ m.email }}</span>
              </div>
            </div>
            <button v-if="isOwner && m.role !== 'owner'" class="kwd-link-btn kwd-link-danger" @click="removeMember(m.userId, m.displayName)">移除</button>
          </li>
        </ul>
      </section>

      <!-- 工单（求助 / 沟通） -->
      <section class="kal-card kwd-card">
        <header class="kwd-card-head">
          <span class="kal-eyebrow">Tickets</span>
          <h2>工作人员支持</h2>
          <button class="kal-btn kal-btn-sm" @click="newTicketOpen = !newTicketOpen">
            <Icon name="plus" :size="13" :stroke="2" /><span>发起求助</span>
          </button>
        </header>
        <p class="kwd-card-desc">
          需要帮忙对接导师 / 协调经费 / 借用设备场地？开一张工单，工作人员会接手并和你在同一线程下沟通。
        </p>

        <Transition name="kal-page">
          <div v-if="newTicketOpen" class="kwd-member-form">
            <div class="kal-field">
              <label class="kal-label kal-label-required">主题</label>
              <input class="kal-input" v-model="newTicket.subject" placeholder="一句话说清楚你需要什么帮助" maxlength="80" />
            </div>
            <div class="kal-field">
              <label class="kal-label kal-label-required">类别</label>
              <div class="kwd-cat-grid">
                <button
                  v-for="c in TICKET_CATEGORIES"
                  :key="c.value"
                  type="button"
                  class="kwd-cat"
                  :class="{ 'is-active': newTicket.category === c.value }"
                  @click="newTicket.category = c.value"
                >
                  <strong>{{ c.label }}</strong>
                  <small>{{ c.desc }}</small>
                </button>
              </div>
            </div>
            <div class="kal-field">
              <label class="kal-label kal-label-required">说明</label>
              <textarea class="kal-input kwd-textarea" rows="4" v-model="newTicket.firstMessage"
                        placeholder="把背景、当前进度与希望得到的具体支持讲清楚。" maxlength="1000"></textarea>
            </div>
            <div class="kwd-ms-editor-actions">
              <button class="kal-btn kal-btn-sm kal-btn-secondary" @click="newTicketOpen = false">取消</button>
              <button class="kal-btn kal-btn-sm" @click="openTicket">
                <Icon name="check" :size="13" :stroke="2" /><span>提交工单</span>
              </button>
            </div>
          </div>
        </Transition>

        <ul class="kwd-tickets" v-if="detail.tickets.length">
          <li v-for="t in detail.tickets" :key="t.ticketId" class="kwd-ticket-wrap">
            <button class="kwd-ticket" :class="{ 'is-active': activeTicketId === t.ticketId }" @click="openTicketThread(t.ticketId)">
              <div class="kwd-ticket-head">
                <span class="kwd-ticket-cat">{{ TICKET_CATEGORY_LABEL[t.category] || t.category }}</span>
                <span class="kmc-status">
                  <span class="kmc-status-dot" :class="`kmc-status-dot--${TICKET_STATUSES[t.status]?.dot || 'closed'}`"></span>
                  <span>{{ TICKET_STATUSES[t.status]?.label || t.status }}</span>
                </span>
              </div>
              <div class="kwd-ticket-subject">{{ t.subject }}</div>
              <div class="kwd-ticket-meta">
                <span>{{ t.openerName }}</span>
                <span class="kwd-sep">·</span>
                <span v-if="t.assigneeStaffName">由 {{ t.assigneeStaffName }} 处理</span>
                <span v-else class="kwd-ticket-pending">待工作人员认领</span>
                <span class="kwd-sep">·</span>
                <span>{{ t.messageCount }} 条对话</span>
                <span class="kwd-sep">·</span>
                <span>{{ fmtTime(t.lastActivityAt) }}</span>
              </div>
            </button>

            <Transition name="kal-page">
              <div v-if="activeTicketId === t.ticketId" class="kwd-thread">
                <div v-if="!activeTicket" class="kwd-thread-loading">加载中…</div>
                <template v-else>
                  <div class="kwd-thread-msgs" :ref="setScrollRef">
                    <div
                      v-for="msg in activeTicket.messages"
                      :key="msg.id"
                      class="kwd-msg"
                      :class="meId() === msg.authorId ? 'is-self' : authorClass(msg.authorRole)"
                    >
                      <span class="kal-avatar kal-avatar-sm kwd-msg-avatar">{{ (msg.authorName || '·')[0] }}</span>
                      <div class="kwd-msg-body">
                        <div class="kwd-msg-head">
                          <span class="kwd-msg-name">{{ msg.authorName }}</span>
                          <span v-if="msg.authorRole === 'staff' || msg.authorRole === 'admin' || msg.authorRole === 'super_admin'" class="kwd-msg-tag">工作人员</span>
                          <span class="kwd-msg-time">{{ fmtTime(msg.createdAt) }}</span>
                        </div>
                        <div class="kwd-msg-content">{{ msg.content }}</div>
                      </div>
                    </div>
                  </div>

                  <div v-if="activeTicket.ticket.status !== 'closed'" class="kwd-thread-input">
                    <textarea class="kal-input" rows="2" v-model="replyDraft" placeholder="回复…（按 Ctrl+Enter 发送）"
                              @keydown.ctrl.enter.prevent="sendReply"></textarea>
                    <div class="kwd-thread-actions">
                      <div class="kwd-thread-status">
                        <span v-if="activeTicket.ticket.status === 'in_progress'">
                          <button class="kwd-link-btn" @click="changeTicketStatus('resolved')">标记为已解决</button>
                        </span>
                        <span v-if="activeTicket.ticket.openerId === meId() && activeTicket.ticket.status !== 'closed'">
                          <button class="kwd-link-btn" @click="changeTicketStatus('closed')">关闭工单</button>
                        </span>
                      </div>
                      <button class="kal-btn kal-btn-sm" :disabled="replying || !replyDraft.trim()" @click="sendReply">
                        <Icon name="message" :size="13" :stroke="2" />
                        <span>{{ replying ? '发送中…' : '发送' }}</span>
                      </button>
                    </div>
                  </div>
                  <div v-else class="kwd-thread-closed">本工单已关闭，如有新问题请重新发起。</div>
                </template>
              </div>
            </Transition>
          </li>
        </ul>
        <div v-else class="kwd-empty-row">
          暂无工单。如需对接导师 / 经费 / 物资，点击右上角发起求助。
        </div>
      </section>
    </template>

    <!-- 编辑基本信息弹层 -->
    <Transition name="kal-page">
      <div v-if="editing" class="kwd-modal" @click.self="editing = false">
        <div class="kwd-modal-card">
          <header class="kwd-modal-head">
            <h3>编辑项目信息</h3>
            <button class="kwd-icon-btn" @click="editing = false"><Icon name="close" :size="16" /></button>
          </header>
          <div class="kwd-modal-body">
            <div class="kal-field">
              <label class="kal-label kal-label-required">项目标题</label>
              <input class="kal-input" v-model="editForm.title" maxlength="80" />
            </div>
            <div class="kal-field">
              <label class="kal-label">一句话简述</label>
              <input class="kal-input" v-model="editForm.summary" maxlength="200" />
            </div>
            <div class="kal-grid-2">
              <div class="kal-field">
                <label class="kal-label">关联比赛</label>
                <select
                  class="kal-input"
                  :value="editForm.competitionShort || ''"
                  @change="editForm.competitionShort = $event.target.value"
                >
                  <option value="">自由项目 / 不关联比赛</option>
                  <option v-for="c in competitions" :key="c.competitionId" :value="c.shortName || c.name">
                    {{ c.shortName || c.name }} · {{ compStatusLabel(c.status) }}
                  </option>
                </select>
                <input class="kal-input kwd-edit-custom-comp" v-model="editForm.competitionShort" placeholder="也可以手动填写其他比赛名称" />
              </div>
              <div class="kal-field">
                <label class="kal-label">目标</label>
                <input class="kal-input" v-model="editForm.competitionTarget" />
              </div>
            </div>
            <div class="kal-field">
              <label class="kal-label">对接工作人员</label>
              <select class="kal-input" v-model="editForm.assignedStaffId">
                <option value="">暂不指定</option>
                <option v-for="s in staffList" :key="s.userId" :value="s.userId">
                  {{ s.displayName }} · {{ s.deptName || '工作人员' }}
                </option>
              </select>
              <div class="kal-hint">只显示 staff 工作人员账号，不包含管理员/超级管理员。</div>
            </div>
            <div class="kal-grid-2">
              <div class="kal-field">
                <label class="kal-label">阶段</label>
                <select class="kal-input" v-model="editForm.phase">
                  <option v-for="p in PHASES" :key="p.value" :value="p.value">{{ p.label }}</option>
                </select>
              </div>
              <div class="kal-field">
                <label class="kal-label">项目状态</label>
                <select class="kal-input" v-model="editForm.status">
                  <option value="active">进行中</option>
                  <option value="completed">已结题</option>
                  <option value="archived">归档</option>
                </select>
              </div>
            </div>
          </div>
          <footer class="kwd-modal-foot">
            <button class="kal-btn kal-btn-secondary" @click="editing = false">取消</button>
            <button class="kal-btn" @click="saveEdit">保存</button>
          </footer>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.kwd { max-width: 1080px; padding-top: 24px; padding-bottom: 64px; }
.kwd-back {
  display: inline-flex; align-items: center; gap: 6px;
  background: transparent; border: 0; padding: 0; margin-bottom: 16px;
  color: var(--kal-text-muted); font-size: 12px; letter-spacing: 1px; cursor: pointer;
}
.kwd-back :deep(.kal-icon) { transform: rotate(180deg); }
.kwd-back:hover { color: var(--kal-primary-700); }

.kwd-loading, .kwd-error { padding: 28px; text-align: center; color: var(--kal-text-muted); }
.kwd-error { color: var(--kal-primary-700); border-left: 2px solid var(--kal-primary-600); }

/* ---------- Hero ---------- */
.kwd-hero {
  position: relative;
  padding: 36px 40px 28px;
  background: linear-gradient(135deg, var(--kal-paper) 0%, var(--kal-bg) 100%);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  margin-bottom: 22px;
  overflow: hidden;
}
.kwd-hero::before {
  content: '';
  position: absolute;
  top: 0; right: 0;
  width: 200px; height: 200px;
  background: radial-gradient(circle at top right, rgba(134, 26, 18, 0.06), transparent 70%);
  pointer-events: none;
}
.kwd-hero-top {
  display: flex; justify-content: space-between; align-items: center;
  gap: 16px; margin-bottom: 18px; flex-wrap: wrap;
}
.kwd-hero-eyebrow {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 11px; letter-spacing: 1.5px;
  color: var(--kal-text-subtle); text-transform: uppercase;
  flex-wrap: wrap;
}
.kwd-tag {
  padding: 3px 10px;
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: none;
}
.kwd-sep { color: var(--kal-gray-300); }

.kwd-hero-mid {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: 16px; margin-bottom: 22px;
}
.kwd-title {
  font-family: var(--kal-font-serif);
  font-size: 30px; font-weight: 600;
  letter-spacing: 3px;
  color: var(--kal-text-strong);
  margin: 0 0 8px;
  line-height: 1.4;
}
.kwd-summary {
  color: var(--kal-text-muted);
  font-size: 14px; line-height: 1.85;
  margin: 0; max-width: 720px;
}

.kwd-hero-foot {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--kal-divider);
}
.kwd-hero-stat dt {
  font-size: 10.5px; letter-spacing: 1.5px;
  color: var(--kal-text-subtle); text-transform: uppercase;
  margin-bottom: 4px;
}
.kwd-hero-stat dd {
  margin: 0;
  font-family: var(--kal-font-serif);
  font-size: 14px; font-weight: 500;
  color: var(--kal-text-strong);
  letter-spacing: 0.5px;
}
.kwd-staff-select {
  width: 100%;
  max-width: 220px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  background: rgba(255,255,255,0.76);
  color: var(--kal-text-strong);
  font-size: 12px;
}

/* ---------- Competition timeline ---------- */
.kwd-comp-card {
  padding: 24px 28px;
  margin-bottom: 18px;
  background: linear-gradient(135deg, var(--kal-paper) 0%, #fff 100%);
}
.kwd-comp-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}
.kwd-comp-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 6px; }
.kwd-comp-head h2 {
  margin: 0 0 6px;
  font-family: var(--kal-font-serif);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--kal-text-strong);
}
.kwd-comp-head p {
  margin: 0;
  max-width: 680px;
  color: var(--kal-text-muted);
  font-size: 12.5px;
  line-height: 1.75;
}
.kwd-comp-badge {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  padding: 10px 14px;
  background: var(--kal-primary-50);
  border-radius: var(--kal-radius-sm);
  color: var(--kal-primary-700);
}
.kwd-comp-badge strong { font-size: 12px; letter-spacing: 1px; }
.kwd-comp-badge span { font-size: 11px; letter-spacing: 0.5px; }
.kwd-comp-timeline {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.kwd-comp-step {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255,255,255,0.74);
  border: 1px solid var(--kal-divider);
  border-radius: var(--kal-radius-sm);
}
.kwd-comp-dot {
  width: 9px;
  height: 9px;
  margin-top: 5px;
  border-radius: 50%;
  background: var(--kal-text-subtle);
  flex-shrink: 0;
}
.kwd-comp-step.is-open .kwd-comp-dot { background: var(--kal-success); }
.kwd-comp-step.is-urgent .kwd-comp-dot { background: var(--kal-primary-600); }
.kwd-comp-step.is-done .kwd-comp-dot { background: var(--kal-sand); }
.kwd-comp-step strong { display: block; color: var(--kal-text-strong); font-size: 12px; letter-spacing: 0.5px; }
.kwd-comp-step small { display: block; margin-top: 3px; color: var(--kal-text-subtle); font-size: 11px; line-height: 1.4; }
.kwd-comp-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.kwd-comp-links a {
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--kal-border);
  color: var(--kal-primary-700);
  font-size: 11.5px;
}

/* ---------- Card 通用 ---------- */
.kwd-card {
  padding: 26px 32px;
  margin-bottom: 18px;
}
.kwd-card-head {
  display: flex; align-items: center; gap: 14px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--kal-divider);
}
.kwd-card-head .kal-eyebrow { color: var(--kal-text-subtle); margin: 0; }
.kwd-card-head h2 {
  font-family: var(--kal-font-serif);
  font-size: 17px; font-weight: 600;
  letter-spacing: 2px;
  color: var(--kal-text-strong);
  flex: 1;
  margin: 0;
}
.kwd-card-desc { font-size: 12.5px; color: var(--kal-text-subtle); line-height: 1.7; margin: -4px 0 14px; }

/* ---------- Progress ---------- */
.kwd-progress-row {
  display: flex; align-items: center; gap: 16px;
  padding: 8px 0 4px;
}
.kwd-progress-row input[type="range"] { flex: 1; accent-color: var(--kal-primary-600); }
.kwd-progress-num {
  font-family: var(--kal-font-serif);
  font-size: 18px; font-weight: 700;
  color: var(--kal-primary-700);
  min-width: 56px; text-align: right;
  letter-spacing: 0.5px;
}
.kwd-phase-row {
  display: flex; align-items: center; gap: 14px;
  padding-top: 12px; flex-wrap: wrap;
}
.kwd-phase-label {
  font-size: 11px; letter-spacing: 1.5px;
  color: var(--kal-text-subtle); text-transform: uppercase;
}
.kwd-phase-pills { display: flex; gap: 6px; flex-wrap: wrap; }
.kwd-phase-pill {
  padding: 5px 12px;
  border: 1px solid var(--kal-border);
  background: var(--kal-bg-subtle);
  border-radius: 999px;
  font-size: 12px;
  color: var(--kal-text-muted);
  cursor: pointer;
  transition: all .15s;
  letter-spacing: 0.5px;
}
.kwd-phase-pill:hover { border-color: var(--kal-primary-300); color: var(--kal-text-strong); }
.kwd-phase-pill.is-active {
  background: var(--kal-ink); color: #fff;
  border-color: var(--kal-ink);
}
.kwd-progress-actions { margin-top: 14px; display: flex; gap: 8px; }

/* ---------- Milestones ---------- */
.kwd-milestones { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; }
.kwd-milestone {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--kal-divider);
}
.kwd-milestone:last-child { border-bottom: 0; }
.kwd-ms-toggle {
  width: 22px; height: 22px;
  border-radius: 50%;
  border: 1.5px solid var(--kal-gray-300);
  background: transparent;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0; margin-top: 2px;
  cursor: pointer; transition: all .15s;
  color: transparent;
}
.kwd-ms-toggle:hover { border-color: var(--kal-primary-600); }
.kwd-ms-toggle.is-doing { border-color: var(--kal-primary-600); background: var(--kal-primary-50); }
.kwd-ms-toggle.is-done {
  background: var(--kal-success); border-color: var(--kal-success);
  color: #fff;
}
.kwd-ms-body { flex: 1; min-width: 0; }
.kwd-ms-title {
  font-size: 14px; font-weight: 500; letter-spacing: 0.5px;
  color: var(--kal-text-strong); line-height: 1.5;
}
.kwd-milestone.is-done .kwd-ms-title { text-decoration: line-through; color: var(--kal-text-subtle); }
.kwd-ms-meta {
  font-size: 12px; color: var(--kal-text-subtle);
  margin-top: 4px; letter-spacing: 0.3px;
  display: inline-flex; flex-wrap: wrap; gap: 6px;
}
.kwd-ms-note { color: var(--kal-text-muted); }
.kwd-ms-ops { display: flex; gap: 4px; flex-shrink: 0; }
.kwd-link-btn {
  background: transparent; border: 0;
  padding: 2px 8px;
  font-size: 12px; letter-spacing: 0.5px;
  color: var(--kal-primary-700); border-radius: 4px;
  cursor: pointer;
  transition: background .15s;
}
.kwd-link-btn:hover { background: var(--kal-primary-50); }
.kwd-link-danger { color: #b53a2a; }
.kwd-link-danger:hover { background: rgba(181, 58, 42, 0.08); }

.kwd-empty-row {
  padding: 20px 0;
  text-align: center;
  color: var(--kal-text-subtle);
  font-size: 13px; letter-spacing: 0.5px;
}

.kwd-ms-editor, .kwd-member-form {
  margin-top: 16px;
  padding: 16px 18px;
  background: var(--kal-bg-subtle);
  border-radius: var(--kal-radius-sm);
  border-left: 2px solid var(--kal-primary-600);
  display: flex; flex-direction: column; gap: 12px;
}
.kwd-ms-editor-grid { display: grid; grid-template-columns: 2fr 1fr 1fr 0.6fr; gap: 12px; }
.kwd-ms-editor-actions { display: flex; justify-content: flex-end; gap: 8px; }

/* ---------- Members ---------- */
.kwd-members { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; }
.kwd-member {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 0;
  border-bottom: 1px dashed var(--kal-divider);
}
.kwd-member:last-child { border-bottom: 0; }
.kwd-member-body { flex: 1; min-width: 0; }
.kwd-member-line {
  display: inline-flex; align-items: center; gap: 8px;
  flex-wrap: wrap;
}
.kwd-member-name { font-size: 14px; font-weight: 500; color: var(--kal-text-strong); letter-spacing: 0.5px; }
.kwd-member-tag {
  display: inline-block; padding: 1px 8px;
  background: var(--kal-bg-subtle);
  border-radius: 999px;
  font-size: 10.5px;
  color: var(--kal-text-muted);
  letter-spacing: 0.5px;
}
.kwd-member-tag.is-owner {
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  font-weight: 600;
}
.kwd-member-sub {
  font-size: 12px; color: var(--kal-text-subtle);
  margin-top: 4px; letter-spacing: 0.3px;
  display: inline-flex; flex-wrap: wrap; gap: 6px;
}
.kwd-member-email { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }

/* ---------- Tickets ---------- */
.kwd-cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 8px;
}
.kwd-cat {
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  text-align: left;
  cursor: pointer;
  transition: all .15s;
  display: flex; flex-direction: column; gap: 2px;
}
.kwd-cat strong { font-size: 13px; color: var(--kal-text-strong); letter-spacing: 0.5px; font-weight: 600; }
.kwd-cat small { font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 0.3px; line-height: 1.5; }
.kwd-cat:hover { border-color: var(--kal-primary-300); }
.kwd-cat.is-active {
  border-color: var(--kal-primary-600);
  background: var(--kal-primary-50);
}
.kwd-cat.is-active strong { color: var(--kal-primary-700); }

.kwd-textarea { resize: vertical; line-height: 1.7; }

.kwd-tickets { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.kwd-ticket-wrap {}
.kwd-ticket {
  width: 100%;
  display: flex; flex-direction: column; gap: 8px;
  padding: 14px 16px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  text-align: left;
  cursor: pointer;
  transition: all .15s;
}
.kwd-ticket:hover { border-color: var(--kal-primary-300); }
.kwd-ticket.is-active {
  border-color: var(--kal-primary-600);
  background: linear-gradient(180deg, #fff 0%, var(--kal-primary-50) 100%);
}
.kwd-ticket-head {
  display: flex; justify-content: space-between; align-items: center;
}
.kwd-ticket-cat {
  font-size: 11px; letter-spacing: 1px;
  padding: 2px 8px;
  background: var(--kal-bg-subtle);
  border-radius: 999px;
  color: var(--kal-text-muted);
  text-transform: uppercase;
}
.kwd-ticket-subject {
  font-size: 14px; color: var(--kal-text-strong);
  font-weight: 500; letter-spacing: 0.5px;
}
.kwd-ticket-meta {
  display: inline-flex; flex-wrap: wrap; gap: 6px;
  font-size: 11.5px; color: var(--kal-text-subtle);
  letter-spacing: 0.3px;
}
.kwd-ticket-pending { color: var(--kal-primary-700); font-weight: 500; }

.kwd-thread {
  margin-top: 6px;
  padding: 14px 16px 12px;
  background: var(--kal-bg);
  border: 1px solid var(--kal-border);
  border-top: 0;
  border-bottom-left-radius: var(--kal-radius-sm);
  border-bottom-right-radius: var(--kal-radius-sm);
}
.kwd-thread-loading { padding: 18px 0; text-align: center; color: var(--kal-text-subtle); }
.kwd-thread-msgs {
  display: flex; flex-direction: column; gap: 12px;
  max-height: 360px;
  overflow: auto;
  padding: 4px 4px 12px;
}
.kwd-msg { display: flex; gap: 10px; align-items: flex-start; }
.kwd-msg.is-self { flex-direction: row-reverse; }
.kwd-msg.is-self .kwd-msg-body { align-items: flex-end; }
.kwd-msg-avatar { flex-shrink: 0; }
.kwd-msg-body { display: flex; flex-direction: column; gap: 4px; max-width: 80%; }
.kwd-msg-head {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11.5px; color: var(--kal-text-subtle);
}
.kwd-msg-name { font-weight: 500; color: var(--kal-text-strong); }
.kwd-msg-tag {
  display: inline-block; padding: 1px 6px;
  font-size: 10px; letter-spacing: 1px;
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  border-radius: var(--kal-radius-xs);
  font-weight: 600;
}
.kwd-msg-time { color: var(--kal-text-subtle); }
.kwd-msg-content {
  padding: 9px 13px;
  border-radius: var(--kal-radius-sm);
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  font-size: 13.5px; line-height: 1.7;
  color: var(--kal-text); letter-spacing: 0.3px;
  white-space: pre-wrap;
  word-break: break-word;
}
.kwd-msg.is-self .kwd-msg-content {
  background: var(--kal-ink); color: #fff; border-color: var(--kal-ink);
}
.kwd-msg.is-staff .kwd-msg-content {
  background: var(--kal-primary-50);
  border-color: var(--kal-primary-200);
  color: var(--kal-primary-900);
}

.kwd-thread-input {
  margin-top: 8px;
  display: flex; flex-direction: column; gap: 8px;
}
.kwd-thread-input textarea { resize: vertical; line-height: 1.7; }
.kwd-thread-actions {
  display: flex; justify-content: space-between; align-items: center;
  gap: 10px;
}
.kwd-thread-status { display: flex; gap: 8px; font-size: 12px; }
.kwd-thread-closed {
  margin-top: 8px;
  padding: 8px 14px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text-subtle);
  border-radius: var(--kal-radius-sm);
  font-size: 12px;
  letter-spacing: 0.5px;
  text-align: center;
}

/* ---------- Modal ---------- */
.kwd-modal {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(34, 24, 20, 0.45);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
  backdrop-filter: blur(2px);
}
.kwd-modal-card {
  width: 100%; max-width: 520px;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  display: flex; flex-direction: column;
  max-height: 90vh;
}
.kwd-modal-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 24px; border-bottom: 1px solid var(--kal-divider);
}
.kwd-modal-head h3 {
  font-family: var(--kal-font-serif); font-size: 18px;
  letter-spacing: 2px; font-weight: 600; color: var(--kal-text-strong);
}
.kwd-modal-body {
  padding: 20px 24px; overflow: auto;
  display: flex; flex-direction: column; gap: 14px;
}
.kwd-modal-foot {
  padding: 16px 24px; border-top: 1px solid var(--kal-divider);
  display: flex; justify-content: flex-end; gap: 10px;
}
.kwd-icon-btn {
  width: 30px; height: 30px;
  background: transparent; border: 0;
  border-radius: var(--kal-radius-sm);
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--kal-text-muted); cursor: pointer;
  transition: background .15s;
}
.kwd-icon-btn:hover { background: var(--kal-bg); color: var(--kal-text-strong); }
.kwd-edit-custom-comp { margin-top: 8px; }

/* ---------- 复用 MyCenter 的状态点（必须本地写一份避免 scoped 失效） ---------- */
.kmc-status {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11px; letter-spacing: 1.5px;
  text-transform: uppercase; color: var(--kal-text-muted);
}
.kmc-status-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--kal-text-subtle); }
.kmc-status-dot--recruiting { background: var(--kal-success); }
.kmc-status-dot--completed  { background: var(--kal-sand); }
.kmc-status-dot--closed     { background: var(--kal-gray-400); }
.kmc-status-dot--urgent     { background: var(--kal-primary-600); }

@media (max-width: 768px) {
  .kwd { padding-left: 16px; padding-right: 16px; }
  .kwd-hero { padding: 24px 22px; }
  .kwd-card { padding: 22px; }
  .kwd-comp-head { flex-direction: column; }
  .kwd-comp-badge { align-items: flex-start; }
  .kwd-comp-timeline { grid-template-columns: 1fr; }
  .kwd-ms-editor-grid { grid-template-columns: 1fr 1fr; }
}
</style>
