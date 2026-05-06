<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import { personalCardsApi } from '@/api/projects'
import { normalizePersonalCard } from '@/api/normalize'

const router = useRouter()

const form = ref({
  display_name: '',
  target_role: '',
  weekly_hours: '',
  vacation_available: false,
  skills: [],
  self_intro: '',
  interested_competitions: [],
  contact_wechat: '',
  privacy_show_dept: true,
  privacy_show_grade: true
})
const skillInput = ref('')
const submitting = ref(false)
const error = ref('')

const targetRoles = [
  { value: 'tech', label: '技术开发' },
  { value: 'product', label: '产品设计' },
  { value: 'design', label: '视觉设计' },
  { value: 'business', label: '商业运营' },
  { value: 'operation', label: '运营策划' },
  { value: 'other', label: '其他' }
]

const competitions = [
  { value: 'dachuan', label: '大创' },
  { value: 'internet', label: '互联网+' },
  { value: 'challenge', label: '挑战杯' },
  { value: 'jingcai', label: '京彩大创' },
  { value: 'other', label: '其他' }
]

function addSkill() {
  const s = skillInput.value.trim()
  if (!s) return
  if (form.value.skills.includes(s)) return
  if (form.value.skills.length >= 8) return
  form.value.skills.push(s)
  skillInput.value = ''
}
function removeSkill(s) {
  form.value.skills = form.value.skills.filter(x => x !== s)
}
function toggleCompetition(value) {
  const list = form.value.interested_competitions
  const i = list.indexOf(value)
  if (i >= 0) list.splice(i, 1)
  else list.push(value)
}
function isCompChecked(value) {
  return form.value.interested_competitions.includes(value)
}

const valid = computed(() =>
  form.value.display_name.trim() &&
  form.value.target_role &&
  form.value.weekly_hours >= 1 &&
  form.value.skills.length > 0
)

async function loadMine() {
  try {
    const mine = await personalCardsApi.mine()
    if (!mine) return
    const card = normalizePersonalCard(mine)
    form.value = {
      ...form.value,
      display_name: card.display_name,
      target_role: card.target_role,
      weekly_hours: card.weekly_hours,
      vacation_available: card.vacation_available,
      skills: card.skills,
      self_intro: card.self_intro,
      interested_competitions: card.interested_competitions,
    }
  } catch (e) {
    // 未登录或后端未启动时保持空表单。
  }
}

async function submit() {
  if (!valid.value) return alert('请完整填写必填项（昵称 / 目标角色 / 每周时间 / 至少 1 个技能）')
  submitting.value = true
  error.value = ''
  try {
    await personalCardsApi.upsert({
      displayName: form.value.display_name,
      targetRole: form.value.target_role,
      weeklyHours: Number(form.value.weekly_hours || 0),
      vacationAvailable: form.value.vacation_available,
      skills: form.value.skills,
      selfIntro: form.value.self_intro,
      interestedCompetitions: form.value.interested_competitions,
      visibility: 'public',
    })
    router.push('/me')
  } catch (e) {
    error.value = e.message || '保存失败，请确认已登录且后端已启动。'
  } finally {
    submitting.value = false
  }
}

onMounted(loadMine)
</script>

<template>
  <div class="kal-container kpf">
    <div class="kpf-head">
      <button class="kpf-back" @click="router.back()" aria-label="返回">
        <Icon name="arrow-left" :size="16" :stroke="1.8" />
      </button>
      <div>
        <div class="kal-eyebrow kpf-eyebrow">My&nbsp;Personal&nbsp;Card</div>
        <h1 class="kpf-title">发布个人卡</h1>
        <p class="kpf-sub">陈列你的专长与节奏——让项目方读懂你，再以邀请相赴。</p>
      </div>
    </div>

    <div class="kpf-layout">
      <!-- Form -->
      <form class="kpf-form" @submit.prevent="submit">
        <div v-if="error" class="kpf-error">{{ error }}</div>
        <section class="kpf-section">
          <header class="kpf-section-head">
            <span class="kpf-step">01</span><h2>基本信息</h2>
          </header>
          <div class="kpf-grid">
            <div class="kpf-col-2">
              <label class="kal-label kal-label-required">展示昵称</label>
              <input class="kal-input" v-model="form.display_name" placeholder="将在个人卡上显示" />
            </div>
            <div>
              <label class="kal-label kal-label-required">目标角色</label>
              <select class="kal-select" v-model="form.target_role">
                <option value="" disabled>请选择</option>
                <option v-for="o in targetRoles" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div>
              <label class="kal-label kal-label-required">每周可投入（小时）</label>
              <input class="kal-input" type="number" min="1" max="40" v-model.number="form.weekly_hours" placeholder="例如：15" />
            </div>
            <div class="kpf-col-2">
              <label class="kpf-toggle">
                <input type="checkbox" v-model="form.vacation_available" />
                <span class="kpf-toggle-text">寒暑假也可投入</span>
              </label>
            </div>
          </div>
        </section>

        <section class="kpf-section">
          <header class="kpf-section-head">
            <span class="kpf-step">02</span><h2>技能与自我介绍</h2>
          </header>

          <label class="kal-label kal-label-required">技能标签（最多 8 个）</label>
          <div class="kpf-skills">
            <span
              v-for="s in form.skills"
              :key="s"
              class="kpf-skill"
            >
              <span>{{ s }}</span>
              <button type="button" class="kpf-skill-remove" @click="removeSkill(s)" aria-label="移除">
                <Icon name="close" :size="11" :stroke="2" />
              </button>
            </span>
            <div class="kpf-skill-input">
              <input
                v-model="skillInput"
                @keydown.enter.prevent="addSkill"
                placeholder="输入后回车添加，如 Python / UI 设计"
              />
              <button type="button" @click="addSkill">+</button>
            </div>
          </div>
          <div class="kal-hint">已添加 {{ form.skills.length }} / 8</div>

          <div style="margin-top: 18px;">
            <label class="kal-label">自我介绍</label>
            <textarea
              class="kal-textarea"
              v-model="form.self_intro"
              maxlength="300"
              placeholder="介绍你的经验、特长、项目经历等（限 300 字）"
            ></textarea>
            <div class="kal-hint">{{ form.self_intro.length }}/300</div>
          </div>
        </section>

        <section class="kpf-section">
          <header class="kpf-section-head">
            <span class="kpf-step">03</span><h2>感兴趣的比赛</h2>
          </header>
          <div class="kpf-comp-list">
            <label
              v-for="c in competitions"
              :key="c.value"
              class="kpf-comp"
              :class="{ 'kpf-comp--active': isCompChecked(c.value) }"
            >
              <input
                type="checkbox"
                :value="c.value"
                :checked="isCompChecked(c.value)"
                @change="toggleCompetition(c.value)"
              />
              <span>{{ c.label }}</span>
            </label>
          </div>
        </section>

        <section class="kpf-section">
          <header class="kpf-section-head">
            <span class="kpf-step">04</span><h2>联系方式与隐私</h2>
          </header>

          <div>
            <label class="kal-label">微信号（可选）</label>
            <input class="kal-input" v-model="form.contact_wechat" placeholder="填写后会在私信中显示，方便快速沟通" />
          </div>

          <div class="kpf-privacy">
            <label class="kpf-toggle">
              <input type="checkbox" v-model="form.privacy_show_dept" />
              <span class="kpf-toggle-text">在个人卡上展示<strong>院系</strong></span>
            </label>
            <label class="kpf-toggle">
              <input type="checkbox" v-model="form.privacy_show_grade" />
              <span class="kpf-toggle-text">在个人卡上展示<strong>年级</strong></span>
            </label>
          </div>
        </section>

        <div class="kpf-actions">
          <button type="button" class="kal-btn kal-btn-ghost" @click="router.back()">取消</button>
          <button type="submit" class="kal-btn kal-btn-lg" :disabled="!valid || submitting">
            {{ submitting ? '保存中…' : '发布个人卡' }}
          </button>
        </div>
      </form>

      <!-- Live preview -->
      <aside class="kpf-preview">
        <h3 class="kpf-preview-title">实时预览</h3>
        <div class="kpf-preview-card">
          <div class="kpf-preview-head">
            <span class="kal-avatar kal-avatar-lg">{{ form.display_name?.[0] || '我' }}</span>
            <div>
              <div class="kpf-preview-name">{{ form.display_name || '你的昵称' }}</div>
              <div class="kpf-preview-meta">
                <span v-if="form.privacy_show_dept">信息学院</span>
                <span v-if="form.privacy_show_grade"> · 2023级</span>
                <span v-if="form.target_role"> · {{ targetRoles.find(r => r.value === form.target_role)?.label }}</span>
              </div>
            </div>
          </div>
          <div class="kpf-preview-skills">
            <span v-for="s in form.skills" :key="s" class="kpf-preview-chip">{{ s }}</span>
            <span v-if="form.skills.length === 0" class="kpf-preview-empty">添加技能后将出现在此处</span>
          </div>
          <p class="kpf-preview-intro">
            {{ form.self_intro || '自我介绍将出现在这里…' }}
          </p>
          <div class="kpf-preview-foot">
            <span>每周可投入 {{ form.weekly_hours || 0 }}h</span>
            <span v-if="form.vacation_available">· 寒暑假可投入</span>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.kpf { max-width: 1100px; }
.kpf-head { display: flex; align-items: flex-start; gap: 18px; margin-bottom: 32px; }
.kpf-back {
  width: 40px;
  height: 40px;
  border: 1px solid var(--kal-border);
  background: var(--kal-surface);
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.kpf-back:hover { background: var(--kal-bg-subtle); color: var(--kal-text); }
.kpf-eyebrow { color: var(--kal-text-subtle); margin-bottom: 10px; }
.kpf-title {
  font-family: var(--kal-font-serif);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 4px;
  color: var(--kal-text-strong);
  margin-bottom: 6px;
}
.kpf-sub { color: var(--kal-text-muted); letter-spacing: 0.5px; line-height: 1.7; }

.kpf-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
.kpf-form { display: flex; flex-direction: column; gap: 16px; }
.kpf-error {
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  border-left: 2px solid var(--kal-primary-600);
  border-radius: var(--kal-radius-sm);
  padding: 10px 14px;
  font-size: var(--kal-text-sm);
}
.kpf-section {
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
  padding: 28px;
  box-shadow: var(--kal-shadow-xs);
}
.kpf-section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--kal-divider);
}
.kpf-section-head h2 {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xl);
  font-weight: 600;
  letter-spacing: 2px;
}
.kpf-step {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 14px;
  font-weight: 500;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
}
.kpf-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.kpf-col-2 { grid-column: 1 / -1; }

.kpf-toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  font-size: var(--kal-text-md);
  color: var(--kal-text);
}
.kpf-toggle input { width: 18px; height: 18px; accent-color: var(--kal-primary-600); }
.kpf-toggle-text strong { color: var(--kal-text-strong); }

.kpf-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  min-height: 50px;
  background: var(--kal-bg-subtle);
}
.kpf-skill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--kal-bg);
  color: var(--kal-text);
  font-size: 11px;
  letter-spacing: 0.5px;
  border-radius: var(--kal-radius-xs);
  border: 1px solid var(--kal-border);
}
.kpf-skill-remove {
  background: transparent;
  border: none;
  color: var(--kal-text-subtle);
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  opacity: 0.6;
}
.kpf-skill-remove:hover { opacity: 1; color: var(--kal-primary-700); }
.kpf-skill-input {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 200px;
}
.kpf-skill-input input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: var(--kal-text-md);
  padding: 4px 0;
}
.kpf-skill-input button {
  border: none;
  background: var(--kal-ink);
  color: #fff;
  width: 22px; height: 22px;
  border-radius: var(--kal-radius-full);
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.kpf-comp-list { display: flex; flex-wrap: wrap; gap: 10px; }
.kpf-comp {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  border: 1.5px solid var(--kal-border);
  border-radius: var(--kal-radius-full);
  cursor: pointer;
  user-select: none;
  font-size: var(--kal-text-md);
  color: var(--kal-text);
  transition: all var(--kal-duration-2);
}
.kpf-comp:hover { border-color: var(--kal-text); color: var(--kal-text); }
.kpf-comp--active {
  background: var(--kal-ink);
  color: #fff;
  border-color: var(--kal-ink);
  font-weight: 500;
}
.kpf-comp input { display: none; }

.kpf-privacy {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 18px;
  padding: 16px;
  background: var(--kal-bg-subtle);
  border-radius: var(--kal-radius-md);
}

.kpf-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Preview */
.kpf-preview {
  position: sticky;
  top: calc(var(--kal-header-height) + 16px);
  align-self: start;
}
.kpf-preview-title {
  font-size: var(--kal-text-sm);
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 12px;
}
.kpf-preview-card {
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
  padding: 24px;
  box-shadow: var(--kal-shadow-sm);
}
.kpf-preview-head { display: flex; gap: 14px; align-items: center; margin-bottom: 16px; }
.kpf-preview-name {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xl);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 1px;
}
.kpf-preview-meta { font-size: var(--kal-text-sm); color: var(--kal-text-muted); margin-top: 4px; }
.kpf-preview-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
  min-height: 28px;
}
.kpf-preview-chip {
  display: inline-flex;
  padding: 3px 10px;
  font-size: 11px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text);
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}
.kpf-preview-empty { color: var(--kal-text-subtle); font-size: var(--kal-text-sm); }
.kpf-preview-intro {
  color: var(--kal-text-muted);
  font-size: var(--kal-text-md);
  line-height: 1.7;
  margin-bottom: 16px;
  min-height: 60px;
}
.kpf-preview-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  padding-top: 14px;
  border-top: 1px solid var(--kal-divider);
}

@media (min-width: 1024px) {
  .kpf-layout { grid-template-columns: 1fr 360px; }
}
@media (max-width: 768px) {
  .kpf-section { padding: 20px; }
  .kpf-grid { grid-template-columns: 1fr; }
}
</style>
