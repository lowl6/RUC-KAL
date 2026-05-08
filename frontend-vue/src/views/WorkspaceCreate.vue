<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { workspaceApi, PHASES } from '@/api/workspace'
import Icon from '@/components/Icon.vue'

const router = useRouter()
const form = ref({
  title: '',
  summary: '',
  competitionShort: '',
  competitionTarget: '',
  phase: 'idea',
  progress: 0,
})
const submitting = ref(false)
const error = ref('')

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

      <div class="kal-grid-2">
        <div class="kal-field">
          <label class="kal-label" for="kwc-comp">关联比赛（可空）</label>
          <input id="kwc-comp" class="kal-input" v-model="form.competitionShort" placeholder="如：中国国际大学生创新大赛" />
        </div>
        <div class="kal-field">
          <label class="kal-label" for="kwc-target">目标</label>
          <input id="kwc-target" class="kal-input" v-model="form.competitionTarget" placeholder="如：校赛一等奖 / 推荐至省赛" />
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
