<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import { projectsApi } from '@/api/projects'

const router = useRouter()
const route = useRoute()

const form = ref({
  project_name: '',
  one_liner: '',
  project_type: '',
  competition_target: route.query.prefill_competition || '',
  competition_deadline: '',
  team_deadline: '',
  current_members: 1,
  weekly_hours: '',
  detail: '',
  contact_wechat: '',
  roles: [
    { role_name: '', count: 1, skills: '' }
  ]
})
const submitting = ref(false)
const error = ref('')

const projectTypes = [
  { value: 'innovation', label: '创新' },
  { value: 'creation', label: '创造' },
  { value: 'entrepreneurship', label: '创业' }
]
const competitions = [
  { value: 'dachuan', label: '大创' },
  { value: 'internet', label: '互联网+' },
  { value: 'challenge', label: '挑战杯' },
  { value: 'jingcai', label: '京彩大创' },
  { value: 'other', label: '其他' },
  { value: 'none', label: '无特定比赛' }
]

const valid = computed(() => (
  form.value.project_name.trim() &&
  form.value.one_liner.trim() &&
  form.value.project_type &&
  form.value.competition_target &&
  form.value.competition_deadline &&
  form.value.team_deadline &&
  form.value.roles.every(r => r.role_name.trim() && r.count >= 1)
))

function addRole() {
  form.value.roles.push({ role_name: '', count: 1, skills: '' })
}
function removeRole(i) {
  if (form.value.roles.length > 1) form.value.roles.splice(i, 1)
}
async function submit() {
  if (!valid.value) return alert('请完整填写带 * 的必填项')
  submitting.value = true
  error.value = ''
  try {
    const created = await projectsApi.create({
      projectName: form.value.project_name,
      oneLiner: form.value.one_liner,
      projectType: form.value.project_type,
      competitionShort: form.value.competition_target,
      competitionTarget: form.value.competition_target,
      competitionDeadline: form.value.competition_deadline,
      teamDeadline: form.value.team_deadline,
      currentMembers: form.value.current_members,
      neededCount: form.value.roles.reduce((sum, r) => sum + Number(r.count || 0), 0),
      weeklyHours: Number(form.value.weekly_hours || 0),
      detail: form.value.detail,
      tags: form.value.roles.flatMap(r => r.skills ? r.skills.split(/[，,]/).map(s => s.trim()).filter(Boolean) : []),
      roles: form.value.roles.map(r => ({
        roleName: r.role_name,
        count: Number(r.count || 1),
        skills: r.skills,
      })),
    })
    router.push({
      path: '/me',
      query: {
        tab: 'projects',
        refresh: String(Date.now()),
        created: created.projectId,
      }
    })
  } catch (e) {
    error.value = e.message || '发布失败，请确认已登录且后端已启动。'
  } finally {
    submitting.value = false
  }
}
function cancel() { router.back() }
</script>

<template>
  <div class="kal-container kp">
    <div class="kp-head">
      <button class="kp-back" @click="cancel" aria-label="返回">
        <Icon name="arrow-left" :size="16" :stroke="1.8" />
      </button>
      <div>
        <div class="kal-eyebrow kp-eyebrow">New&nbsp;Project&nbsp;Card</div>
        <h1 class="kp-title">发布项目卡</h1>
        <p class="kp-sub">陈述项目骨架，列明所需角色——让合适的同学循路而来。</p>
      </div>
    </div>

    <form class="kp-form" @submit.prevent="submit">
      <div v-if="error" class="kp-error">{{ error }}</div>
      <!-- 基本信息 -->
      <section class="kp-section">
        <header class="kp-section-head">
          <span class="kp-step">01</span>
          <h2>项目基本信息</h2>
        </header>

        <div class="kp-grid">
          <div class="kp-field kp-col-2">
            <label class="kal-label kal-label-required">项目名称</label>
            <input
              class="kal-input"
              v-model="form.project_name"
              maxlength="20"
              placeholder="请输入项目名称（限 20 字）"
            />
            <div class="kal-hint">{{ form.project_name.length }}/20</div>
          </div>

          <div class="kp-field kp-col-2">
            <label class="kal-label kal-label-required">一句话介绍</label>
            <input
              class="kal-input"
              v-model="form.one_liner"
              maxlength="30"
              placeholder="用一句话概括项目亮点（限 30 字）"
            />
            <div class="kal-hint">{{ form.one_liner.length }}/30</div>
          </div>

          <div class="kp-field">
            <label class="kal-label kal-label-required">项目类型</label>
            <select class="kal-select" v-model="form.project_type">
              <option value="" disabled>请选择</option>
              <option v-for="o in projectTypes" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </div>

          <div class="kp-field">
            <label class="kal-label kal-label-required">目标比赛</label>
            <select class="kal-select" v-model="form.competition_target">
              <option value="" disabled>请选择</option>
              <option v-for="o in competitions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </div>

          <div class="kp-field">
            <label class="kal-label kal-label-required">比赛截止日期</label>
            <input class="kal-input" type="date" v-model="form.competition_deadline" />
          </div>
          <div class="kp-field">
            <label class="kal-label kal-label-required">组队截止日期</label>
            <input class="kal-input" type="date" v-model="form.team_deadline" />
            <div class="kal-hint">建议至少留 14 天用于磨合</div>
          </div>

          <div class="kp-field kp-col-2">
            <label class="kal-label">项目详情</label>
            <textarea
              class="kal-textarea"
              v-model="form.detail"
              maxlength="500"
              placeholder="详细描述项目背景、目标、目前进展、技术栈与计划等（限 500 字）"
            ></textarea>
            <div class="kal-hint">{{ form.detail.length }}/500</div>
          </div>
        </div>
      </section>

      <!-- 团队信息 -->
      <section class="kp-section">
        <header class="kp-section-head">
          <span class="kp-step">02</span>
          <h2>团队信息</h2>
        </header>

        <div class="kp-grid">
          <div class="kp-field">
            <label class="kal-label kal-label-required">已有成员（含自己）</label>
            <input class="kal-input" type="number" min="1" max="20" v-model.number="form.current_members" />
          </div>
          <div class="kp-field">
            <label class="kal-label">每周预计投入（小时）</label>
            <input class="kal-input" type="number" min="1" max="40" v-model.number="form.weekly_hours" placeholder="例如：15" />
          </div>
        </div>
      </section>

      <!-- 所需角色 -->
      <section class="kp-section">
        <header class="kp-section-head">
          <span class="kp-step">03</span>
          <h2>所需角色</h2>
          <span class="kp-section-hint">至少填写 1 个</span>
        </header>

        <div class="kp-roles">
          <div
            v-for="(r, i) in form.roles"
            :key="i"
            class="kp-role"
          >
            <div class="kp-role-head">
              <span class="kp-role-tag">角色 {{ i + 1 }}</span>
              <button
                v-if="form.roles.length > 1"
                type="button"
                class="kp-role-remove"
                @click="removeRole(i)"
              >移除</button>
            </div>
            <div class="kp-grid">
              <div class="kp-field">
                <label class="kal-label kal-label-required">角色名称</label>
                <input class="kal-input" v-model="r.role_name" placeholder="例如：后端开发" />
              </div>
              <div class="kp-field">
                <label class="kal-label kal-label-required">人数</label>
                <input class="kal-input" type="number" min="1" v-model.number="r.count" />
              </div>
              <div class="kp-field kp-col-2">
                <label class="kal-label">技能要求</label>
                <input class="kal-input" v-model="r.skills" placeholder="例如：Python, Django, PostgreSQL" />
              </div>
            </div>
          </div>

          <button type="button" class="kp-role-add" @click="addRole">
            <Icon name="plus" :size="13" :stroke="1.8" />
            <span>添加角色</span>
          </button>
        </div>
      </section>

      <!-- 联系方式 -->
      <section class="kp-section">
        <header class="kp-section-head">
          <span class="kp-step">04</span>
          <h2>联系方式（可选）</h2>
        </header>

        <div class="kp-field">
          <label class="kal-label">微信号</label>
          <input class="kal-input" v-model="form.contact_wechat" placeholder="填写后可加速换微信，仅在私信中展示" />
        </div>
      </section>

      <!-- Actions -->
      <div class="kp-actions">
        <button type="button" class="kal-btn kal-btn-ghost" @click="cancel">取消</button>
        <button type="submit" class="kal-btn kal-btn-lg" :disabled="!valid || submitting">
          {{ submitting ? '发布中…' : '发布项目卡' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.kp { max-width: 880px; }
.kp-head { display: flex; gap: 18px; align-items: flex-start; margin-bottom: 32px; }
.kp-back {
  width: 40px;
  height: 40px;
  border: 1px solid var(--kal-border);
  background: var(--kal-surface);
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--kal-duration-2);
  flex-shrink: 0;
}
.kp-back:hover { background: var(--kal-bg-subtle); color: var(--kal-text); transform: translateX(-2px); }
.kp-eyebrow { color: var(--kal-text-subtle); margin-bottom: 10px; }
.kp-title {
  font-family: var(--kal-font-serif);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 4px;
  color: var(--kal-text-strong);
  margin-bottom: 6px;
}
.kp-sub { font-size: var(--kal-text-md); color: var(--kal-text-muted); letter-spacing: 0.5px; line-height: 1.7; }

.kp-form { display: flex; flex-direction: column; gap: 16px; }
.kp-error {
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  border-left: 2px solid var(--kal-primary-600);
  border-radius: var(--kal-radius-sm);
  padding: 10px 14px;
  font-size: var(--kal-text-sm);
}
.kp-section {
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-lg);
  padding: 28px 32px;
  box-shadow: var(--kal-shadow-xs);
}
.kp-section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--kal-divider);
}
.kp-section-head h2 {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xl);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 2px;
}
.kp-section-hint {
  margin-left: auto;
  font-size: 11px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
}
.kp-step {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 14px;
  font-weight: 500;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
}

.kp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.kp-field { display: flex; flex-direction: column; }
.kp-col-2 { grid-column: 1 / -1; }

.kp-roles { display: flex; flex-direction: column; gap: 14px; }
.kp-role {
  background: var(--kal-bg-subtle);
  border: 1px dashed var(--kal-border-strong);
  border-radius: var(--kal-radius-md);
  padding: 18px 20px;
}
.kp-role-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.kp-role-tag {
  display: inline-flex;
  align-items: center;
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 12px;
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
}
.kp-role-remove {
  background: transparent;
  border: none;
  color: var(--kal-text-subtle);
  font-size: 11px;
  letter-spacing: 1px;
  cursor: pointer;
  text-transform: uppercase;
}
.kp-role-remove:hover { color: var(--kal-primary-700); }
.kp-role-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  background: transparent;
  border: 1px dashed var(--kal-border-strong);
  border-radius: var(--kal-radius-md);
  color: var(--kal-text-muted);
  font-size: var(--kal-text-md);
  font-weight: 500;
  transition: all var(--kal-duration-2);
}
.kp-role-add:hover {
  border-color: var(--kal-primary-500);
  color: var(--kal-primary-700);
  background: var(--kal-primary-50);
}

.kp-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 0 0;
}

@media (max-width: 768px) {
  .kp-section { padding: 20px; }
  .kp-grid { grid-template-columns: 1fr; }
}
</style>
