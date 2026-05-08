<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { workspaceApi, PHASES } from '@/api/workspace'
import { competitionsApi } from '@/api/projects'
import Icon from '@/components/Icon.vue'

const router = useRouter()
const form = ref({
  title: '',
  summary: '',
  competitionShort: '',
  competitionTarget: '',
  assignedStaffId: '',
  phase: 'idea',
  progress: 0,
})
const submitting = ref(false)
const error = ref('')
const staffList = ref([])
const competitions = ref([])
const competitionMode = ref('select') // select | custom
const selectedCompetitionId = ref('')

const selectedCompetition = computed(() =>
  competitions.value.find(c => c.competitionId === selectedCompetitionId.value) || null
)

const competitionOptions = computed(() =>
  competitions.value
    .filter(c => c.status !== 'ended')
    .sort((a, b) => String(a.registerEnd || '').localeCompare(String(b.registerEnd || '')))
)

const timelineSteps = computed(() => {
  const c = selectedCompetition.value
  if (!c) return []
  return [
    { label: '报名开始', date: c.registerStart, state: stepState(c.registerStart) },
    { label: '报名截止', date: c.registerEnd, state: stepState(c.registerEnd) },
    { label: '赛事日程', date: c.scheduleNote || '查看通知', state: 'note' },
  ]
})

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

function statusLabel (s) {
  return ({ upcoming: '即将开放', active: '报名中', urgent: '即将截止', ended: '已结束' })[s] || s
}

function applyCompetition (c) {
  selectedCompetitionId.value = c.competitionId
  form.value.competitionShort = c.shortName || c.name
  form.value.competitionTarget = form.value.competitionTarget || (c.level === 'national' ? '国赛入围 / 校赛推荐' : '完成校赛提交')
}

async function loadOptions () {
  try {
    const [cs, staff] = await Promise.all([
      competitionsApi.list().catch(() => []),
      workspaceApi.staffMembers().catch(() => []),
    ])
    competitions.value = cs || []
    staffList.value = staff || []
  } catch (e) {
    // 选项加载失败不阻断项目创建，仍可手动填写
  }
}

onMounted(loadOptions)

async function submit () {
  error.value = ''
  if (!form.value.title.trim()) { error.value = '请填写项目标题'; return }
  submitting.value = true
  try {
    const w = await workspaceApi.create({
      title: form.value.title.trim(),
      summary: form.value.summary.trim(),
      competitionShort: form.value.competitionShort.trim(),
      competitionTarget: form.value.competitionTarget.trim(),
      assignedStaffId: form.value.assignedStaffId || '',
      phase: form.value.phase,
      progress: Number(form.value.progress) || 0,
    })
    router.replace(`/workspaces/${w.workspaceId}`)
  } catch (e) {
    error.value = e.message || '创建失败'
  } finally {
    submitting.value = false
  }
}

function cancel () { router.back() }
</script>

<template>
  <div class="kal-container kwc">
    <header class="kwc-head">
      <div class="kal-eyebrow">My Center / 我的项目</div>
      <h1 class="kwc-title">新建项目</h1>
      <p class="kwc-desc">
        登记一个真正在做的项目；创建后你即为「项目负责人」，可以邀请队友、维护里程碑，并向工作人员求助。
      </p>
    </header>

    <form class="kal-card kwc-card" @submit.prevent="submit">
      <div class="kal-field">
        <label class="kal-label kal-label-required" for="kwc-title">项目标题</label>
        <input id="kwc-title" class="kal-input" v-model="form.title" placeholder="如：校园闲置物品流转小程序" maxlength="80" />
      </div>

      <div class="kal-field">
        <label class="kal-label" for="kwc-summary">一句话简述</label>
        <input id="kwc-summary" class="kal-input" v-model="form.summary" placeholder="让队友与工作人员一眼看懂你们在做什么" maxlength="200" />
      </div>

      <div class="kal-field">
        <label class="kal-label">关联比赛</label>
        <div class="kwc-mode">
          <button type="button" class="kwc-mode-btn" :class="{ 'is-active': competitionMode === 'select' }" @click="competitionMode = 'select'">
            从比赛中心选择
          </button>
          <button type="button" class="kwc-mode-btn" :class="{ 'is-active': competitionMode === 'custom' }" @click="competitionMode = 'custom'">
            手动填写
          </button>
        </div>

        <div v-if="competitionMode === 'select'" class="kwc-comp-grid">
          <button
            v-for="c in competitionOptions"
            :key="c.competitionId"
            type="button"
            class="kwc-comp-card"
            :class="{ 'is-active': selectedCompetitionId === c.competitionId }"
            @click="applyCompetition(c)"
          >
            <span class="kwc-comp-initial">{{ c.initial || (c.shortName || c.name)?.[0] || '赛' }}</span>
            <span class="kwc-comp-body">
              <strong>{{ c.shortName || c.name }}</strong>
              <small>{{ statusLabel(c.status) }} · 截止 {{ c.registerEnd || '待定' }}</small>
            </span>
            <span v-if="daysLeft(c.registerEnd) !== null" class="kwc-comp-days" :class="{ 'is-urgent': daysLeft(c.registerEnd) <= 7 }">
              {{ daysLeft(c.registerEnd) >= 0 ? `${daysLeft(c.registerEnd)}天` : '已过期' }}
            </span>
          </button>
        </div>

        <div v-if="competitionMode === 'custom' || !competitionOptions.length" class="kal-grid-2 kwc-custom-comp">
          <div class="kal-field">
            <label class="kal-label" for="kwc-comp">比赛名称</label>
            <input id="kwc-comp" class="kal-input" v-model="form.competitionShort" placeholder="如：中国国际大学生创新大赛" />
          </div>
          <div class="kal-field">
            <label class="kal-label" for="kwc-target">目标</label>
            <input id="kwc-target" class="kal-input" v-model="form.competitionTarget" placeholder="如：校赛一等奖 / 推荐至省赛" />
          </div>
        </div>

        <div v-if="selectedCompetition && competitionMode === 'select'" class="kwc-timeline">
          <header class="kwc-timeline-head">
            <div>
              <span class="kwc-timeline-kicker">Competition Timeline</span>
              <h3>{{ selectedCompetition.name }}</h3>
            </div>
            <span class="kwc-timeline-status">{{ statusLabel(selectedCompetition.status) }}</span>
          </header>
          <div class="kwc-timeline-steps">
            <div v-for="s in timelineSteps" :key="s.label" class="kwc-timeline-step" :class="`is-${s.state}`">
              <span class="kwc-timeline-dot"></span>
              <div>
                <strong>{{ s.label }}</strong>
                <small>{{ s.date || '待定' }}</small>
              </div>
            </div>
          </div>
          <p v-if="selectedCompetition.description" class="kwc-timeline-desc">{{ selectedCompetition.description }}</p>
        </div>
      </div>

      <div class="kal-grid-2">
        <div class="kal-field">
          <label class="kal-label">项目目标</label>
          <input class="kal-input" v-model="form.competitionTarget" placeholder="如：校赛一等奖 / 推荐至省赛" />
        </div>
        <div class="kal-field">
          <label class="kal-label">对接工作人员</label>
          <select class="kal-input" v-model="form.assignedStaffId">
            <option value="">暂不指定，提交工单时由工作人员认领</option>
            <option v-for="s in staffList" :key="s.userId" :value="s.userId">
              {{ s.displayName }} · {{ s.deptName || '工作人员' }}
            </option>
          </select>
          <div class="kal-hint">只展示管理员手动创建的工作人员账号，不包含管理员账号。</div>
        </div>
      </div>

      <div class="kal-field">
        <label class="kal-label">当前阶段</label>
        <div class="kwc-phases">
          <button
            v-for="p in PHASES"
            :key="p.value"
            type="button"
            class="kwc-phase"
            :class="{ 'is-active': form.phase === p.value }"
            @click="form.phase = p.value"
          >
            <span class="kwc-phase-label">{{ p.label }}</span>
            <span class="kwc-phase-desc">{{ p.desc }}</span>
          </button>
        </div>
      </div>

      <div class="kal-field">
        <label class="kal-label" for="kwc-progress">起始进度（可日后随时调整）</label>
        <div class="kwc-progress">
          <input id="kwc-progress" type="range" min="0" max="100" step="5" v-model="form.progress" />
          <span class="kwc-progress-num">{{ form.progress }}%</span>
        </div>
      </div>

      <Transition name="kal-page">
        <div v-if="error" class="kwc-error">
          <Icon name="alert" :size="13" />
          <span>{{ error }}</span>
        </div>
      </Transition>

      <footer class="kwc-actions">
        <button type="button" class="kal-btn kal-btn-secondary" @click="cancel">取消</button>
        <button type="submit" class="kal-btn" :disabled="submitting">
          <Icon name="check" :size="13" :stroke="2" />
          <span>{{ submitting ? '创建中…' : '创建项目' }}</span>
        </button>
      </footer>
    </form>
  </div>
</template>

<style scoped>
.kwc { max-width: 760px; padding-top: 32px; padding-bottom: 64px; }
.kwc-head { margin-bottom: 24px; }
.kwc-title {
  font-family: var(--kal-font-serif);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 4px;
  color: var(--kal-text-strong);
  margin: 8px 0 8px;
}
.kwc-desc { color: var(--kal-text-muted); font-size: 13px; line-height: 1.8; max-width: 600px; }
.kwc-card {
  padding: 32px 36px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.kwc-mode {
  display: inline-flex;
  align-self: flex-start;
  padding: 3px;
  margin-bottom: 12px;
  background: var(--kal-bg-subtle);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
}
.kwc-mode-btn {
  border: 0;
  background: transparent;
  padding: 7px 14px;
  border-radius: var(--kal-radius-xs);
  color: var(--kal-text-muted);
  font-size: 12px;
  letter-spacing: 0.5px;
  cursor: pointer;
}
.kwc-mode-btn.is-active { background: var(--kal-ink); color: #fff; }

.kwc-comp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}
.kwc-comp-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  cursor: pointer;
  text-align: left;
  transition: all var(--kal-duration-2);
}
.kwc-comp-card:hover { border-color: var(--kal-primary-300); transform: translateY(-1px); }
.kwc-comp-card.is-active {
  border-color: var(--kal-primary-600);
  background: linear-gradient(180deg, #fff 0%, var(--kal-primary-50) 100%);
  box-shadow: 0 10px 24px rgba(134, 26, 18, 0.08);
}
.kwc-comp-initial {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: var(--kal-radius-sm);
  background: var(--kal-ink);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--kal-font-serif);
  font-size: 15px;
  font-weight: 600;
}
.kwc-comp-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.kwc-comp-body strong {
  color: var(--kal-text-strong);
  font-size: 13.5px;
  letter-spacing: 0.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kwc-comp-body small { color: var(--kal-text-subtle); font-size: 11.5px; letter-spacing: 0.3px; }
.kwc-comp-days {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text-muted);
  font-size: 11px;
  letter-spacing: 0.5px;
}
.kwc-comp-days.is-urgent { background: var(--kal-primary-50); color: var(--kal-primary-700); font-weight: 600; }
.kwc-custom-comp { margin-top: 4px; }

.kwc-timeline {
  margin-top: 14px;
  padding: 18px 20px;
  background: linear-gradient(135deg, var(--kal-paper) 0%, #fff 100%);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
}
.kwc-timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.kwc-timeline-kicker {
  display: block;
  margin-bottom: 4px;
  color: var(--kal-text-subtle);
  font-size: 10px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
}
.kwc-timeline-head h3 {
  margin: 0;
  font-family: var(--kal-font-serif);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--kal-text-strong);
}
.kwc-timeline-status {
  align-self: flex-start;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  font-size: 11px;
  letter-spacing: 1px;
  font-weight: 600;
}
.kwc-timeline-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.kwc-timeline-step {
  position: relative;
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255,255,255,0.72);
  border: 1px solid var(--kal-divider);
  border-radius: var(--kal-radius-sm);
}
.kwc-timeline-dot {
  width: 9px;
  height: 9px;
  margin-top: 5px;
  border-radius: 50%;
  background: var(--kal-text-subtle);
  flex-shrink: 0;
}
.kwc-timeline-step.is-open .kwc-timeline-dot { background: var(--kal-success); }
.kwc-timeline-step.is-urgent .kwc-timeline-dot { background: var(--kal-primary-600); }
.kwc-timeline-step.is-done .kwc-timeline-dot { background: var(--kal-sand); }
.kwc-timeline-step strong { display: block; font-size: 12px; color: var(--kal-text-strong); letter-spacing: 0.5px; }
.kwc-timeline-step small { display: block; margin-top: 3px; font-size: 11px; color: var(--kal-text-subtle); line-height: 1.4; }
.kwc-timeline-desc { margin: 12px 0 0; color: var(--kal-text-muted); font-size: 12.5px; line-height: 1.7; }

.kwc-phases {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}
.kwc-phase {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px 14px;
  background: var(--kal-bg-subtle);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  cursor: pointer;
  text-align: left;
  transition: all var(--kal-duration-2);
}
.kwc-phase:hover { border-color: var(--kal-primary-300); }
.kwc-phase.is-active {
  background: var(--kal-primary-50);
  border-color: var(--kal-primary-600);
  box-shadow: 0 4px 16px rgba(134, 26, 18, 0.08);
}
.kwc-phase-label {
  font-family: var(--kal-font-serif);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1.2px;
  color: var(--kal-text-strong);
}
.kwc-phase-desc { font-size: 11.5px; color: var(--kal-text-subtle); line-height: 1.5; letter-spacing: 0.3px; }
.kwc-phase.is-active .kwc-phase-label { color: var(--kal-primary-700); }

.kwc-progress { display: flex; align-items: center; gap: 14px; }
.kwc-progress input[type="range"] { flex: 1; accent-color: var(--kal-primary-600); }
.kwc-progress-num {
  font-family: var(--kal-font-serif);
  font-size: 14px;
  font-weight: 600;
  color: var(--kal-text-strong);
  min-width: 48px;
  text-align: right;
}

.kwc-error {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-left: 2px solid var(--kal-primary-600);
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  font-size: 13px;
  border-radius: var(--kal-radius-sm);
}

.kwc-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px dashed var(--kal-divider);
  margin-top: 8px;
}

@media (max-width: 640px) {
  .kwc-card { padding: 24px 20px; }
}
</style>
