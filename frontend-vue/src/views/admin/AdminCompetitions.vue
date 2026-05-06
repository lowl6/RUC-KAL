<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api/admin'
import Icon from '@/components/Icon.vue'

const list = ref([])
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const search = ref('')

const showEdit = ref(false)
const blank = () => ({
  competitionId: null,
  name: '', shortName: '', initial: '',
  level: 'national', organizer: '',
  registerStart: new Date().toISOString().slice(0, 10),
  registerEnd: '',
  status: 'upcoming',
  description: '',
  posterUrl: '',
  prize: '',
  scheduleNote: '',
  contactEmail: '',
  contactPhone: '',
  officialLinks: [],
  qrCodes: []
})
const editing = reactive(blank())

const filtered = computed(() => {
  if (!search.value) return list.value
  const k = search.value.toLowerCase()
  return list.value.filter(c =>
    (c.name || '').toLowerCase().includes(k) ||
    (c.organizer || '').toLowerCase().includes(k)
  )
})

async function load () {
  loading.value = true; error.value = ''
  try { list.value = await adminApi.listCompetitions() }
  catch (e) { error.value = e.message }
  finally   { loading.value = false }
}
onMounted(load)

function fillEditing (c) {
  Object.assign(editing, blank())
  if (!c) return
  Object.assign(editing, {
    competitionId: c.competitionId,
    name: c.name || '',
    shortName: c.shortName || '',
    initial: c.initial || '',
    level: c.level || 'national',
    organizer: c.organizer || '',
    registerStart: c.registerStart || '',
    registerEnd: c.registerEnd || '',
    status: c.status || 'upcoming',
    description: c.description || '',
    posterUrl: c.posterUrl || '',
    prize: c.prize || '',
    scheduleNote: c.scheduleNote || '',
    contactEmail: c.contactEmail || '',
    contactPhone: c.contactPhone || '',
    officialLinks: Array.isArray(c.officialLinks) ? c.officialLinks.map(x => ({ ...x })) : [],
    qrCodes: Array.isArray(c.qrCodes) ? c.qrCodes.map(x => ({ ...x })) : []
  })
}

function open (c) { fillEditing(c); showEdit.value = true }
function close () { showEdit.value = false }

function addLink () { editing.officialLinks.push({ label: '', url: '' }) }
function removeLink (i) { editing.officialLinks.splice(i, 1) }
function addQr () { editing.qrCodes.push({ label: '', imageUrl: '' }) }
function removeQr (i) { editing.qrCodes.splice(i, 1) }

async function save () {
  if (!editing.name?.trim()) { alert('请填写比赛名称'); return }
  saving.value = true
  try {
    const payload = {
      ...editing,
      status: deriveStatus(editing.registerStart, editing.registerEnd),
      officialLinks: editing.officialLinks.filter(x => x.url?.trim()),
      qrCodes: editing.qrCodes.filter(x => x.imageUrl?.trim())
    }
    await adminApi.upsertCompetition(payload)
    showEdit.value = false
    await load()
  } catch (e) {
    alert(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove (c) {
  if (!confirm(`确定删除「${c.name}」？该操作不可撤销，关联资讯也会被清理。`)) return
  await adminApi.deleteCompetition(c.competitionId)
  await load()
}

const levelLabel = (l) => ({ national: '国赛', provincial: '省赛', school: '校赛' })[l] || l
const statusLabel = (s) => ({ upcoming: '未开始', active: '进行中', urgent: '冲刺中', ended: '已结束' })[s] || s

/** 与后端 CompetitionDtos.computeStatus 一致：仅根据起止日期派生状态。 */
function deriveStatus (start, end) {
  if (!start || !end) return 'upcoming'
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const s = new Date(start); s.setHours(0, 0, 0, 0)
  const e = new Date(end);   e.setHours(0, 0, 0, 0)
  if (today < s) return 'upcoming'
  if (today > e) return 'ended'
  const days = Math.round((e - today) / 86400000)
  return days <= 7 ? 'urgent' : 'active'
}
const autoStatus = computed(() => deriveStatus(editing.registerStart, editing.registerEnd))
</script>

<template>
  <div class="ad-page">
    <header class="ad-page-head">
      <div>
        <div class="kal-eyebrow">Console / Competitions</div>
        <h1 class="ad-page-title">赛事发布</h1>
        <p class="ad-page-sub">标准化录入赛事信息：基本字段 · 官方链接 · 报名二维码 · 联系方式 · 奖项设置。</p>
      </div>
      <button class="kal-btn kal-btn-primary" @click="open(null)">
        <Icon name="plus" :size="14" /> 新增赛事
      </button>
    </header>

    <div v-if="error" class="ad-alert">{{ error }}</div>

    <div class="ad-filter">
      <input class="kal-input" v-model="search" placeholder="搜索：赛事名称 / 主办方" />
    </div>

    <div class="ad-table-wrap">
      <table class="ad-table">
        <thead>
          <tr>
            <th>赛事</th><th>主办</th><th>层级</th><th>报名窗口</th><th>项目数</th><th>状态</th>
            <th style="width: 200px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="ad-empty">加载中…</td></tr>
          <tr v-else-if="!filtered.length"><td colspan="7" class="ad-empty">暂无比赛</td></tr>
          <tr v-for="c in filtered" :key="c.competitionId">
            <td>
              <div class="ad-name">{{ c.name }}</div>
              <div class="ad-sub">{{ c.competitionId }}</div>
            </td>
            <td>{{ c.organizer || '—' }}</td>
            <td><span class="ad-tag">{{ levelLabel(c.level) }}</span></td>
            <td class="ad-mono">{{ c.registerStart || '—' }} → {{ c.registerEnd || '—' }}</td>
            <td>{{ c.projectCount || 0 }}</td>
            <td><span class="ad-dot" :class="`is-${c.status}`"></span>{{ statusLabel(c.status) }}</td>
            <td class="ad-ops">
              <button class="ad-link-btn" @click="open(c)">编辑</button>
              <button class="ad-link-btn ad-link-danger" @click="remove(c)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <Transition name="kal-page">
      <div v-if="showEdit" class="ad-modal" @click.self="close">
        <div class="ad-modal-card ad-modal-card--lg">
          <header class="ad-modal-head">
            <h3>{{ editing.competitionId ? '编辑赛事' : '新增赛事' }}</h3>
            <button class="ad-icon-btn" @click="close"><Icon name="close" :size="16" /></button>
          </header>
          <div class="ad-modal-body">
            <!-- 基本信息 -->
            <section class="ad-form-section">
              <header class="ad-form-section-head">
                <span class="kal-eyebrow">Section&nbsp;01</span>
                <h4>基本信息</h4>
              </header>
              <div class="kal-field">
                <label class="kal-label kal-label-required">赛事名称</label>
                <input class="kal-input" v-model="editing.name" placeholder="如：中国国际大学生创新大赛" />
              </div>
              <div class="kal-grid-2">
                <div class="kal-field">
                  <label class="kal-label">简称</label>
                  <input class="kal-input" v-model="editing.shortName" placeholder="如：互联网+" />
                </div>
                <div class="kal-field">
                  <label class="kal-label">单字标识（用于卡片）</label>
                  <input class="kal-input" v-model="editing.initial" maxlength="2" placeholder="如：创" />
                </div>
              </div>
              <div class="kal-grid-2">
                <div class="kal-field">
                  <label class="kal-label">层级</label>
                  <select class="kal-input" v-model="editing.level">
                    <option value="national">国赛</option>
                    <option value="provincial">省赛</option>
                    <option value="school">校赛</option>
                  </select>
                </div>
                <div class="kal-field">
                  <label class="kal-label">状态（按日期自动派生）</label>
                  <div class="ad-status-auto">
                    <span class="ad-dot" :class="`is-${autoStatus}`"></span>
                    <span>{{ statusLabel(autoStatus) }}</span>
                    <span class="ad-status-hint">仅依赖下方报名起止日期，无需手动维护</span>
                  </div>
                </div>
              </div>
              <div class="kal-field">
                <label class="kal-label">主办方</label>
                <input class="kal-input" v-model="editing.organizer" placeholder="如：教育部" />
              </div>
              <div class="kal-grid-2">
                <div class="kal-field">
                  <label class="kal-label">报名开始</label>
                  <input class="kal-input" type="date" v-model="editing.registerStart" />
                </div>
                <div class="kal-field">
                  <label class="kal-label">报名截止</label>
                  <input class="kal-input" type="date" v-model="editing.registerEnd" />
                </div>
              </div>
            </section>

            <!-- 内容 -->
            <section class="ad-form-section">
              <header class="ad-form-section-head">
                <span class="kal-eyebrow">Section&nbsp;02</span>
                <h4>内容介绍</h4>
              </header>
              <div class="kal-field">
                <label class="kal-label">赛事简介</label>
                <textarea class="kal-input" rows="3" v-model="editing.description"
                  placeholder="一句话讲清楚：这是一个怎样的比赛，鼓励什么方向的项目"></textarea>
              </div>
              <div class="kal-field">
                <label class="kal-label">奖项设置</label>
                <textarea class="kal-input" rows="2" v-model="editing.prize"
                  placeholder="如：国家级金、银、铜奖；优秀项目入选成果展"></textarea>
              </div>
              <div class="kal-field">
                <label class="kal-label">赛程说明</label>
                <textarea class="kal-input" rows="3" v-model="editing.scheduleNote"
                  placeholder="如：初赛 6 月 / 复赛 8 月 / 总决赛 10 月"></textarea>
              </div>
              <div class="kal-field">
                <label class="kal-label">海报图片 URL（可选）</label>
                <input class="kal-input" v-model="editing.posterUrl" placeholder="https://..." />
              </div>
            </section>

            <!-- 官方链接 -->
            <section class="ad-form-section">
              <header class="ad-form-section-head">
                <span class="kal-eyebrow">Section&nbsp;03</span>
                <h4>官方链接（可多个）</h4>
              </header>
              <div v-if="!editing.officialLinks.length" class="ad-empty-line">
                尚未填写。点击下方「+ 新增链接」添加官方主页、报名通知、申报手册等。
              </div>
              <div v-for="(l, i) in editing.officialLinks" :key="i" class="ad-row">
                <span class="ad-row-num">{{ String(i + 1).padStart(2, '0') }}</span>
                <input class="kal-input ad-row-label" v-model="l.label" placeholder="名称（如：官方主页）" />
                <input class="kal-input ad-row-val" v-model="l.url" placeholder="https://..." />
                <button class="ad-icon-btn ad-row-del" @click="removeLink(i)" title="删除">
                  <Icon name="close" :size="14" />
                </button>
              </div>
              <button class="ad-add-btn" @click="addLink">
                <Icon name="plus" :size="14" /><span>新增链接</span>
              </button>
            </section>

            <!-- 二维码 -->
            <section class="ad-form-section">
              <header class="ad-form-section-head">
                <span class="kal-eyebrow">Section&nbsp;04</span>
                <h4>二维码（可多个）</h4>
              </header>
              <div v-if="!editing.qrCodes.length" class="ad-empty-line">
                尚未填写。可放官方公众号、报名群、客服企业微信等二维码图片 URL。
              </div>
              <div v-for="(q, i) in editing.qrCodes" :key="i" class="ad-row ad-row--qr">
                <span class="ad-row-num">{{ String(i + 1).padStart(2, '0') }}</span>
                <div class="ad-qr-preview">
                  <img v-if="q.imageUrl" :src="q.imageUrl" :alt="q.label" />
                  <span v-else>预览</span>
                </div>
                <div class="ad-row-fields">
                  <input class="kal-input" v-model="q.label" placeholder="名称（如：官方公众号 / 报名群）" />
                  <input class="kal-input" v-model="q.imageUrl" placeholder="二维码图片 URL（http(s)://...）" />
                </div>
                <button class="ad-icon-btn ad-row-del" @click="removeQr(i)" title="删除">
                  <Icon name="close" :size="14" />
                </button>
              </div>
              <button class="ad-add-btn" @click="addQr">
                <Icon name="plus" :size="14" /><span>新增二维码</span>
              </button>
            </section>

            <!-- 联系方式 -->
            <section class="ad-form-section">
              <header class="ad-form-section-head">
                <span class="kal-eyebrow">Section&nbsp;05</span>
                <h4>联系方式</h4>
              </header>
              <div class="kal-grid-2">
                <div class="kal-field">
                  <label class="kal-label">联系邮箱</label>
                  <input class="kal-input" v-model="editing.contactEmail" placeholder="kal-competition@ruc.edu.cn" />
                </div>
                <div class="kal-field">
                  <label class="kal-label">联系电话</label>
                  <input class="kal-input" v-model="editing.contactPhone" placeholder="010-XXXX-XXXX" />
                </div>
              </div>
            </section>
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

.ad-form-section {
  padding: 18px 0;
  border-top: 1px dashed var(--kal-divider);
  display: flex; flex-direction: column; gap: 12px;
}
.ad-form-section:first-child { border-top: 0; padding-top: 0; }
.ad-form-section-head { display: flex; flex-direction: column; gap: 4px; margin-bottom: 4px; }
.ad-form-section-head .kal-eyebrow { color: var(--kal-text-subtle); font-size: 10.5px; letter-spacing: 2px; }
.ad-form-section-head h4 {
  font-family: var(--kal-font-serif);
  font-weight: 600; font-size: 15px;
  letter-spacing: 1.5px; color: var(--kal-text-strong);
}

.ad-row {
  display: flex; align-items: center; gap: 10px;
  padding: 10px; background: var(--kal-bg-subtle);
  border: 1px solid var(--kal-border); border-radius: var(--kal-radius-sm);
}
.ad-row--qr { align-items: stretch; }
.ad-row-num {
  font-family: var(--kal-font-serif); font-style: italic;
  font-size: 12px; color: var(--kal-text-subtle); letter-spacing: 1px;
  width: 22px; flex-shrink: 0; padding-top: 8px;
}
.ad-row-label { width: 140px; flex-shrink: 0; }
.ad-row-val { flex: 1; }
.ad-row-fields { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.ad-row-del { flex-shrink: 0; }

.ad-qr-preview {
  width: 70px; height: 70px; flex-shrink: 0;
  background: #fff; border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  color: var(--kal-text-subtle); font-size: 11px; letter-spacing: 1px;
}
.ad-qr-preview img { width: 100%; height: 100%; object-fit: cover; }

.ad-add-btn {
  align-self: flex-start;
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  background: transparent;
  border: 1px dashed var(--kal-border-strong);
  border-radius: var(--kal-radius-sm);
  color: var(--kal-text-muted);
  font-size: 12.5px; letter-spacing: 0.5px;
  cursor: pointer;
}
.ad-add-btn:hover { border-color: var(--kal-primary-600); color: var(--kal-primary-700); }

.ad-empty-line {
  font-size: 12.5px; color: var(--kal-text-subtle);
  background: var(--kal-bg-subtle);
  padding: 12px 14px;
  border-left: 2px solid var(--kal-border-strong);
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}

.ad-status-auto {
  display: flex; align-items: center; gap: 8px;
  height: 38px; padding: 0 12px;
  background: var(--kal-primary-50);
  border: 1px dashed var(--kal-primary-200);
  border-radius: var(--kal-radius-sm);
  font-size: 13px;
  color: var(--ruc-red-dark);
  letter-spacing: 0.5px;
}
.ad-status-auto .ad-dot { margin: 0; }
.ad-status-auto .ad-status-hint {
  margin-left: auto;
  font-size: 11.5px; color: var(--kal-text-subtle);
  letter-spacing: 1px;
}
</style>
