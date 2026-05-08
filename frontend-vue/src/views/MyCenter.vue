<script setup>
import { computed, onActivated, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { projectsApi, personalCardsApi } from '@/api/projects'
import { workspaceApi, PHASE_LABEL, STATUSES as WS_STATUSES } from '@/api/workspace'
import { normalizeProject, normalizePersonalCard, normalizeUser } from '@/api/normalize'
import Icon from '@/components/Icon.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const user = useUserStore()
const VALID_TABS = ['workspaces', 'projects', 'personal', 'settings']
const tab = ref(VALID_TABS.includes(route.query.tab) ? route.query.tab : 'workspaces')

const myProjects = ref([])
const myWorkspaces = ref([])
const myCard = ref(null)
const loading = ref(false)
const error = ref('')
const me = computed(() => auth.me ? normalizeUser(auth.me) : null)
const initials = computed(() => me.value?.display_name?.[0] || '我')
const unreadMessages = computed(() => user.unreadMessages)

const notifyEmail = ref(false)
const notifySaving = ref(false)
async function toggleNotifyEmail (v) {
  notifySaving.value = true
  try {
    await auth.setNotifyEmail(v)
    notifyEmail.value = v
  } catch (e) {
    notifyEmail.value = !v
    alert(e.message || '保存失败')
  } finally {
    notifySaving.value = false
  }
}

const tabs = [
  { value: 'workspaces', label: '我的项目',   icon: 'compass'   },
  { value: 'projects',   label: '我的项目卡', icon: 'briefcase' },
  { value: 'personal',   label: '我的个人卡', icon: 'target'    },
  { value: 'settings',   label: '账户设置',   icon: 'settings'  }
]
const wsStatusMap = WS_STATUSES
const phaseLabel = PHASE_LABEL

function gotoWorkspace (id) { router.push(`/workspaces/${id}`) }
function gotoCreateWorkspace () { router.push('/workspaces/new') }

const statusMap = {
  recruiting: { label: '组队中' },
  completed:  { label: '已组齐' },
  closed:     { label: '已结束' },
  hidden:     { label: '已隐藏' },
  deleted:    { label: '已删除' }
}

function acceptApplication(p, a) { a.status = 'accepted'; alert(`已同意 ${a.name} 的申请`) }
function rejectApplication(p, a) { a.status = 'rejected'; alert(`已婉拒 ${a.name} 的申请`) }

async function loadMine() {
  loading.value = true
  error.value = ''
  try {
    const [projects, card, workspaces] = await Promise.all([
      projectsApi.mine().catch(() => []),
      personalCardsApi.mine().catch(() => null),
      workspaceApi.mine().catch(() => []),
    ])
    myProjects.value = (projects || []).map(normalizeProject)
    myCard.value = card ? normalizePersonalCard(card) : null
    myWorkspaces.value = workspaces || []
  } catch (e) {
    error.value = '个人数据加载失败：' + (e.message || '请重新登录')
    myProjects.value = []
    myWorkspaces.value = []
    myCard.value = null
  } finally {
    loading.value = false
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}

async function refreshMine() {
  await loadMine()
  notifyEmail.value = !!auth.me?.notifyEmail
  user.refreshUnread()
}

watch(() => route.query.tab, (value) => {
  if (VALID_TABS.includes(value)) tab.value = value
})

watch(() => route.query.refresh, async () => {
  if (route.path === '/me') {
    await refreshMine()
  }
})

onMounted(refreshMine)
onActivated(refreshMine)
</script>

<template>
  <div class="kal-container kmc">
    <!-- 用户信息卡 -->
    <header class="kmc-profile">
      <div class="kmc-profile-left">
        <span class="kal-avatar kal-avatar-lg">{{ initials }}</span>
        <div>
          <div class="kal-eyebrow kmc-profile-eyebrow">My&nbsp;Profile</div>
          <h1 class="kmc-profile-name">{{ me?.display_name }}</h1>
          <div class="kmc-profile-meta">
            <span>{{ me?.dept_name }}</span>
            <span class="kmc-profile-dot"></span>
            <span>{{ me?.grade }}</span>
            <span class="kmc-profile-dot"></span>
            <span class="kmc-profile-email">{{ me?.email }}</span>
          </div>
        </div>
      </div>
      <div class="kmc-profile-stats">
        <div>
          <dt>{{ myWorkspaces.length }}</dt>
          <dd>项目</dd>
        </div>
        <div>
          <dt>{{ myProjects.length }}</dt>
          <dd>项目卡</dd>
        </div>
        <div>
          <dt>{{ unreadMessages }}</dt>
          <dd>未读私信</dd>
        </div>
      </div>
    </header>

    <div v-if="error" class="kal-card kmc-notice">{{ error }}</div>
    <div v-if="loading" class="kal-card kmc-notice">正在读取个人中心数据…</div>

    <!-- Tabs -->
    <nav class="kmc-tabs">
      <button
        v-for="t in tabs"
        :key="t.value"
        class="kmc-tab"
        :class="{ 'kmc-tab--active': tab === t.value }"
        @click="tab = t.value"
      >
        <Icon :name="t.icon" :size="14" />
        <span>{{ t.label }}</span>
      </button>
    </nav>

    <!-- 我的项目（工作空间） -->
    <section v-if="tab === 'workspaces'" class="kmc-section">
      <div class="kmc-section-head">
        <div>
          <h2 class="kmc-section-title">
            <span class="kal-serial">i.</span>
            <span>我的项目</span>
          </h2>
          <p class="kmc-section-sub">追踪进度、维护里程碑、向工作人员发起求助。所有成员共享同一份项目数据。</p>
        </div>
        <button class="kal-btn" @click="gotoCreateWorkspace">
          <Icon name="plus" :size="14" :stroke="2" />
          <span>新建项目</span>
        </button>
      </div>

      <div v-if="!myWorkspaces.length" class="kal-card kmc-empty">
        <div class="kmc-empty-eyebrow">No&nbsp;Workspace</div>
        <h3>还没有进行中的项目</h3>
        <p>当你和队友确定要做某件事，就把它登记成一个「项目」——拥有里程碑、进度条与一条专属的工作人员沟通通道。</p>
        <button class="kal-btn kal-btn-sm" @click="gotoCreateWorkspace">
          <Icon name="plus" :size="13" :stroke="2" /><span>建立第一个项目</span>
        </button>
      </div>

      <div v-else class="kmc-ws-grid">
        <article
          v-for="w in myWorkspaces"
          :key="w.workspaceId"
          class="kal-card kal-card-hoverable kmc-ws"
          @click="gotoWorkspace(w.workspaceId)"
          tabindex="0"
          role="button"
        >
          <header class="kmc-ws-head">
            <div class="kmc-ws-eyebrow">
              <span class="kmc-ws-phase">{{ phaseLabel[w.phase] || w.phase }}</span>
              <span class="kmc-ws-sep">/</span>
              <span>{{ w.competitionShort || '自由项目' }}</span>
            </div>
            <span class="kmc-status">
              <span class="kmc-status-dot" :class="`kmc-status-dot--${wsStatusMap[w.status]?.dot || 'closed'}`"></span>
              <span>{{ wsStatusMap[w.status]?.label || w.status }}</span>
            </span>
          </header>
          <h3 class="kmc-ws-title">{{ w.title }}</h3>
          <p class="kmc-ws-summary">{{ w.summary || '暂无项目简述' }}</p>

          <div class="kmc-ws-progress">
            <div class="kmc-ws-progress-bar">
              <span :style="{ width: (w.progress || 0) + '%' }"></span>
            </div>
            <span class="kmc-ws-progress-num">{{ w.progress || 0 }}%</span>
          </div>

          <footer class="kmc-ws-meta">
            <span class="kmc-meta-item">
              <Icon name="users" :size="13" />
              <span>{{ w.memberCount }} 位成员</span>
            </span>
            <span class="kmc-meta-item">
              <Icon name="check" :size="13" />
              <span>里程碑 {{ w.milestoneDone }} / {{ w.milestoneTotal }}</span>
            </span>
            <span class="kmc-meta-item kmc-ws-tickets" :class="{ 'is-hot': w.openTickets > 0 }">
              <Icon name="message" :size="13" />
              <span>{{ w.openTickets > 0 ? `${w.openTickets} 条未结工单` : '无活跃工单' }}</span>
            </span>
          </footer>
        </article>
      </div>
    </section>

    <!-- 我的项目卡 -->
    <section v-if="tab === 'projects'" class="kmc-section">
      <div class="kmc-section-head">
        <div>
          <h2 class="kmc-section-title">
            <span class="kal-serial">ii.</span>
            <span>我发布的项目卡</span>
          </h2>
          <p class="kmc-section-sub">「项目卡」= 招募广告。已组队成功的同学可在「我的项目」追踪真实进度。</p>
        </div>
        <button class="kal-btn" @click="router.push('/projects/new')">
          <Icon name="plus" :size="14" :stroke="2" />
          <span>新建项目卡</span>
        </button>
      </div>

      <div class="kmc-cards">
        <article
          v-for="p in myProjects"
          :key="p.project_id"
          class="kal-card kmc-project"
        >
          <div class="kmc-project-head">
            <h3 class="kmc-project-title">{{ p.project_name }}</h3>
            <span class="kmc-status">
              <span class="kmc-status-dot" :class="`kmc-status-dot--${p.status}`"></span>
              <span>{{ statusMap[p.status]?.label || p.status }}</span>
            </span>
          </div>
          <p class="kmc-project-desc">{{ p.one_liner }}</p>

          <div class="kmc-project-meta">
            <span class="kmc-meta-item">
              <Icon name="trophy" :size="13" />
              <span>{{ p.competition_short }}</span>
            </span>
            <span class="kmc-meta-item">
              <Icon name="users" :size="13" />
              <span>已有 {{ p.current_members }} · 招募 {{ p.needed_count }}</span>
            </span>
            <span class="kmc-meta-item">
              <Icon name="eye" :size="13" />
              <span>{{ p.view_count }}</span>
            </span>
            <span class="kmc-meta-item">
              <Icon name="mail" :size="13" />
              <span>{{ p.applications.length }}</span>
            </span>
          </div>

          <div v-if="p.applications.length > 0" class="kmc-apps">
            <header class="kmc-apps-head">
              <span class="kal-eyebrow">Applications · {{ p.applications.length }}</span>
            </header>
            <div
              v-for="a in p.applications"
              :key="a.id"
              class="kmc-app"
            >
              <span class="kal-avatar kal-avatar-sm">{{ a.name[0] }}</span>
              <div class="kmc-app-body">
                <div class="kmc-app-name">{{ a.name }}</div>
                <div class="kmc-app-skills">{{ a.skills.join(' · ') }}</div>
              </div>
              <div class="kmc-app-time">{{ a.time }}</div>
              <div class="kmc-app-actions" v-if="a.status === 'pending'">
                <button class="kal-btn kal-btn-sm" @click="acceptApplication(p, a)">同意</button>
                <button class="kal-btn kal-btn-sm kal-btn-ghost" @click="rejectApplication(p, a)">婉拒</button>
              </div>
              <span v-else class="kmc-app-result" :class="{ 'kmc-app-result--ok': a.status === 'accepted' }">
                {{ a.status === 'accepted' ? '已同意' : '已婉拒' }}
              </span>
            </div>
          </div>

          <footer class="kmc-project-actions">
            <button class="kal-btn kal-btn-sm kal-btn-ghost" @click="router.push(`/projects/${p.project_id}`)">
              <span>查看详情</span>
              <Icon name="arrow-right" :size="13" :stroke="1.8" />
            </button>
            <button v-if="p.status === 'recruiting'" class="kal-btn kal-btn-sm kal-btn-secondary">编辑</button>
            <button v-if="p.status === 'recruiting'" class="kal-btn kal-btn-sm kal-btn-secondary">标记已组齐</button>
          </footer>
        </article>
      </div>
    </section>

    <!-- 我的个人卡 -->
    <section v-if="tab === 'personal'" class="kmc-section">
      <div class="kmc-section-head">
        <h2 class="kmc-section-title">
          <span class="kal-serial">iii.</span>
          <span>我的个人卡</span>
        </h2>
        <button class="kal-btn" @click="router.push('/personal-cards/edit')">
          <Icon name="edit" :size="14" />
          <span>编辑</span>
        </button>
      </div>

      <article class="kal-card kmc-personal">
        <div class="kmc-personal-head">
          <span class="kal-avatar kal-avatar-lg">{{ myCard?.initial || '我' }}</span>
          <div class="kmc-personal-info">
            <div class="kmc-personal-name">{{ myCard?.display_name || '尚未发布个人卡' }}</div>
            <div class="kmc-personal-meta">{{ myCard?.dept_name || me?.dept_name }} · {{ myCard?.grade || me?.grade }} · {{ myCard?.target_role || '未填写目标角色' }}</div>
          </div>
          <div class="kmc-personal-toggle">
            <span class="kmc-personal-status">
              <span class="kmc-status-dot kmc-status-dot--recruiting"></span>
              <span>公开可见</span>
            </span>
            <label class="kmc-switch">
              <input type="checkbox" checked />
              <span class="kmc-switch-slider"></span>
            </label>
          </div>
        </div>

        <hr class="kal-rule kmc-rule" />

        <p class="kmc-personal-intro">{{ myCard?.self_intro || '发布个人卡后，这里会展示你的自我介绍。' }}</p>

        <div class="kmc-personal-skills">
          <span v-for="s in (myCard?.skills || [])" :key="s" class="kmc-skill">{{ s }}</span>
        </div>

        <div class="kmc-personal-stats">
          <div>
            <dt>每周可投入</dt>
            <dd>{{ myCard?.weekly_hours || 0 }} 小时</dd>
          </div>
          <div>
            <dt>寒暑假</dt>
            <dd>{{ myCard?.vacation_available ? '可投入' : '时间有限' }}</dd>
          </div>
          <div>
            <dt>感兴趣比赛</dt>
            <dd>{{ (myCard?.interested_competitions || []).join(' · ') || '未填写' }}</dd>
          </div>
        </div>
      </article>
    </section>

    <!-- 设置 -->
    <section v-if="tab === 'settings'" class="kmc-section">
      <article class="kal-card kmc-settings">
        <header class="kmc-settings-head">
          <span class="kal-eyebrow">Notifications</span>
          <h3>通知设置</h3>
        </header>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">收到新申请</div>
            <div class="kmc-settings-desc">有同学申请加入你的项目时通知</div>
          </div>
          <label class="kmc-switch"><input type="checkbox" checked /><span class="kmc-switch-slider"></span></label>
        </div>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">收到新私信 · 站内提醒</div>
            <div class="kmc-settings-desc">登录后顶栏自动出现红点；进入会话即标记已读</div>
          </div>
          <label class="kmc-switch"><input type="checkbox" checked disabled /><span class="kmc-switch-slider"></span></label>
        </div>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">收到新私信 · 邮件提醒</div>
            <div class="kmc-settings-desc">开启后，每当有新私信会向你的校内邮箱发送通知</div>
          </div>
          <label class="kmc-switch">
            <input type="checkbox" :checked="notifyEmail" :disabled="notifySaving" @change="toggleNotifyEmail($event.target.checked)" />
            <span class="kmc-switch-slider"></span>
          </label>
        </div>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">截止日期提醒</div>
            <div class="kmc-settings-desc">比赛 / 组队截止前 3 天提醒</div>
          </div>
          <label class="kmc-switch"><input type="checkbox" checked /><span class="kmc-switch-slider"></span></label>
        </div>

        <header class="kmc-settings-head kmc-mt">
          <span class="kal-eyebrow">Privacy</span>
          <h3>隐私设置</h3>
        </header>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">展示院系</div>
            <div class="kmc-settings-desc">个人卡 / 项目卡是否展示院系</div>
          </div>
          <label class="kmc-switch"><input type="checkbox" checked /><span class="kmc-switch-slider"></span></label>
        </div>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">展示年级</div>
            <div class="kmc-settings-desc">个人卡 / 项目卡是否展示年级</div>
          </div>
          <label class="kmc-switch"><input type="checkbox" checked /><span class="kmc-switch-slider"></span></label>
        </div>

        <header class="kmc-settings-head kmc-mt">
          <span class="kal-eyebrow">Account</span>
          <h3>账户</h3>
        </header>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">微信号预填</div>
            <div class="kmc-settings-desc">私信"发送微信"按钮使用该号码</div>
          </div>
          <input class="kal-input kmc-settings-input" :value="me?.wechat_id" readonly />
        </div>
        <div class="kmc-settings-row">
          <div>
            <div class="kmc-settings-label">退出登录</div>
            <div class="kmc-settings-desc">将清除当前登录态并返回登录页</div>
          </div>
          <button class="kal-btn kal-btn-sm kal-btn-secondary" @click="logout">
            <Icon name="logout" :size="13" />
            <span>退出登录</span>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.kmc { max-width: 1200px; }

.kmc-profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding: 32px 40px;
  margin-bottom: 28px;
  background: var(--kal-paper);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
}
.kmc-profile-left { display: flex; align-items: center; gap: 20px; }
.kmc-profile-eyebrow { color: var(--kal-text-subtle); margin-bottom: 8px; }
.kmc-profile-name {
  font-family: var(--kal-font-serif);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--kal-text-strong);
  margin-bottom: 6px;
}
.kmc-profile-meta {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--kal-text-muted);
  letter-spacing: 0.5px;
}
.kmc-profile-dot { width: 3px; height: 3px; border-radius: 50%; background: currentColor; }
.kmc-profile-email { color: var(--kal-text); }
.kmc-profile-stats {
  display: flex;
  gap: 36px;
  padding-left: 36px;
  border-left: 1px solid var(--kal-border);
}
.kmc-profile-stats > div { display: flex; flex-direction: column; align-items: flex-start; }
.kmc-profile-stats dt {
  font-family: var(--kal-font-serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 0.5px;
  line-height: 1;
  margin-bottom: 4px;
}
.kmc-profile-stats dd { font-size: 11px; color: var(--kal-text-subtle); margin: 0; letter-spacing: 1.5px; }
.kmc-notice {
  padding: 12px 16px;
  margin-bottom: 16px;
  color: var(--kal-text-muted);
  font-size: var(--kal-text-sm);
}

.kmc-tabs {
  display: flex;
  gap: 0;
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  padding: 4px;
  margin-bottom: 24px;
  width: fit-content;
}
.kmc-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  background: transparent;
  border: none;
  font-size: var(--kal-text-md);
  font-weight: 500;
  color: var(--kal-text-muted);
  border-radius: var(--kal-radius-xs);
  cursor: pointer;
  transition: all var(--kal-duration-2);
  letter-spacing: 1px;
}
.kmc-tab:hover { color: var(--kal-text); }
.kmc-tab--active {
  background: var(--kal-ink);
  color: #fff;
}

.kmc-section { display: flex; flex-direction: column; }
.kmc-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.kmc-section-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-2xl);
  font-weight: 600;
  letter-spacing: 2.5px;
  color: var(--kal-text-strong);
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.kmc-section-title .kal-serial { font-size: 0.7em; }

/* ---------- Status dot ---------- */
.kmc-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--kal-text-muted);
}
.kmc-status-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--kal-text-subtle); }
.kmc-status-dot--recruiting { background: var(--kal-success); }
.kmc-status-dot--completed  { background: var(--kal-sand); }
.kmc-status-dot--closed     { background: var(--kal-gray-400); }

/* ---------- Project ---------- */
.kmc-cards { display: flex; flex-direction: column; gap: 20px; }
.kmc-project { padding: 28px 32px; }
.kmc-project-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 8px; }
.kmc-project-title {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-xl);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 1.5px;
}
.kmc-project-desc { color: var(--kal-text-muted); margin-bottom: 18px; line-height: 1.7; }
.kmc-project-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
  font-size: var(--kal-text-sm);
  color: var(--kal-text-muted);
  padding-bottom: 18px;
  border-bottom: 1px solid var(--kal-divider);
}
.kmc-meta-item { display: inline-flex; align-items: center; gap: 6px; }

.kmc-apps {
  margin-top: 20px;
  padding: 20px 22px;
  background: var(--kal-bg-subtle);
  border-radius: var(--kal-radius-sm);
}
.kmc-apps-head { margin-bottom: 14px; }
.kmc-apps-head .kal-eyebrow { color: var(--kal-text-subtle); }
.kmc-app {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid var(--kal-divider);
}
.kmc-app:last-child { border-bottom: none; }
.kmc-app-body { flex: 1; min-width: 0; }
.kmc-app-name { font-weight: 500; font-size: var(--kal-text-sm); color: var(--kal-text); letter-spacing: 0.5px; }
.kmc-app-skills { font-size: 11px; color: var(--kal-text-subtle); margin-top: 3px; letter-spacing: 0.5px; }
.kmc-app-time { font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 0.5px; }
.kmc-app-actions { display: flex; gap: 6px; }
.kmc-app-result {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: var(--kal-radius-xs);
  background: var(--kal-bg);
  color: var(--kal-text-subtle);
  letter-spacing: 1px;
}
.kmc-app-result--ok { color: var(--kal-success); background: var(--kal-success-bg); }

.kmc-project-actions { display: flex; gap: 8px; margin-top: 20px; flex-wrap: wrap; }

/* ---------- Personal ---------- */
.kmc-personal { padding: 32px 36px; }
.kmc-personal-head { display: flex; align-items: center; gap: 18px; }
.kmc-personal-info { flex: 1; min-width: 0; }
.kmc-personal-name {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-2xl);
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--kal-text-strong);
}
.kmc-personal-meta { font-size: var(--kal-text-sm); color: var(--kal-text-muted); margin-top: 4px; letter-spacing: 0.5px; }
.kmc-personal-toggle {
  display: flex;
  align-items: center;
  gap: 14px;
}
.kmc-personal-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--kal-text-muted);
  letter-spacing: 1px;
  text-transform: uppercase;
  font-weight: 500;
}
.kmc-rule { margin: 22px 0; }
.kmc-personal-intro {
  color: var(--kal-text-muted);
  line-height: 1.85;
  margin-bottom: 20px;
}
.kmc-personal-skills { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 24px; }
.kmc-skill {
  display: inline-flex;
  padding: 3px 10px;
  font-size: 11px;
  background: var(--kal-bg-subtle);
  color: var(--kal-text);
  border-radius: var(--kal-radius-xs);
  letter-spacing: 0.5px;
}
.kmc-personal-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid var(--kal-divider);
}
.kmc-personal-stats > div { display: flex; flex-direction: column; gap: 6px; }
.kmc-personal-stats dt {
  font-size: 10px;
  color: var(--kal-text-subtle);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-weight: 500;
}
.kmc-personal-stats dd {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-md);
  color: var(--kal-text-strong);
  font-weight: 500;
  letter-spacing: 0.5px;
  margin: 0;
}

/* ---------- Switch ---------- */
.kmc-switch { position: relative; display: inline-block; width: 40px; height: 22px; }
.kmc-switch input { opacity: 0; width: 0; height: 0; }
.kmc-switch-slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background: var(--kal-gray-300);
  border-radius: 24px;
  transition: .3s;
}
.kmc-switch-slider::before {
  content: '';
  position: absolute;
  height: 16px; width: 16px;
  left: 3px; bottom: 3px;
  background: #fff;
  border-radius: 50%;
  transition: .3s;
  box-shadow: var(--kal-shadow-xs);
}
.kmc-switch input:checked + .kmc-switch-slider { background: var(--kal-ink); }
.kmc-switch input:checked + .kmc-switch-slider::before { transform: translateX(18px); }

/* ---------- Settings ---------- */
.kmc-settings { padding: 32px 36px; }
.kmc-settings-head { padding-bottom: 12px; margin-bottom: 8px; border-bottom: 1px solid var(--kal-divider); }
.kmc-settings-head .kal-eyebrow { color: var(--kal-text-subtle); margin-bottom: 6px; }
.kmc-settings-head h3 {
  font-family: var(--kal-font-serif);
  font-size: var(--kal-text-lg);
  font-weight: 600;
  color: var(--kal-text-strong);
  letter-spacing: 2px;
}
.kmc-mt { margin-top: 32px; }
.kmc-settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px dashed var(--kal-divider);
}
.kmc-settings-row:last-child { border-bottom: none; }
.kmc-settings-label { font-weight: 500; color: var(--kal-text); font-size: var(--kal-text-md); letter-spacing: 0.5px; }
.kmc-settings-desc { font-size: 11px; color: var(--kal-text-subtle); margin-top: 4px; letter-spacing: 0.5px; }
.kmc-settings-input { width: auto; max-width: 220px; padding: 6px 10px; font-size: var(--kal-text-sm); }

.kmc-section-head > div { display: flex; flex-direction: column; gap: 6px; }
.kmc-section-sub { color: var(--kal-text-subtle); font-size: 12.5px; line-height: 1.7; max-width: 560px; letter-spacing: 0.3px; }

/* ========== 我的项目（工作空间） ========== */
.kmc-ws-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 18px;
}
.kmc-ws {
  padding: 24px 26px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  transition: transform var(--kal-duration-2), box-shadow var(--kal-duration-2), border-color var(--kal-duration-2);
}
.kmc-ws::before {
  content: '';
  position: absolute;
  left: 0; top: 22px; bottom: 22px;
  width: 2px;
  background: var(--kal-primary-600);
  border-radius: 2px;
  opacity: 0.6;
  transition: opacity var(--kal-duration-2), background var(--kal-duration-2);
}
.kmc-ws:hover::before, .kmc-ws:focus-visible::before { opacity: 1; }
.kmc-ws:focus-visible { outline: none; border-color: var(--kal-primary-300); }
.kmc-ws-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.kmc-ws-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 1.5px;
  color: var(--kal-text-subtle);
  text-transform: uppercase;
}
.kmc-ws-phase {
  display: inline-block;
  padding: 2px 8px;
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  border-radius: var(--kal-radius-xs);
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: none;
  font-size: 11px;
}
.kmc-ws-sep { color: var(--kal-gray-300); }
.kmc-ws-title {
  font-family: var(--kal-font-serif);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--kal-text-strong);
  margin: 0;
  line-height: 1.4;
}
.kmc-ws-summary {
  color: var(--kal-text-muted);
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.kmc-ws-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}
.kmc-ws-progress-bar {
  flex: 1;
  height: 4px;
  background: var(--kal-bg-subtle);
  border-radius: 4px;
  overflow: hidden;
}
.kmc-ws-progress-bar > span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--kal-primary-500), var(--kal-primary-700));
  border-radius: 4px;
  transition: width var(--kal-duration-3) var(--kal-ease-out);
}
.kmc-ws-progress-num {
  font-family: var(--kal-font-serif);
  font-size: 13px;
  font-weight: 600;
  color: var(--kal-text-strong);
  min-width: 38px;
  text-align: right;
  letter-spacing: 0.5px;
}
.kmc-ws-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: var(--kal-text-muted);
  padding-top: 12px;
  border-top: 1px dashed var(--kal-divider);
}
.kmc-ws-tickets.is-hot { color: var(--kal-primary-700); font-weight: 500; }

/* ========== 空状态 ========== */
.kmc-empty {
  padding: 48px 36px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.kmc-empty-eyebrow {
  font-size: 11px;
  letter-spacing: 0.32em;
  color: var(--kal-text-subtle);
  text-transform: uppercase;
}
.kmc-empty h3 {
  font-family: var(--kal-font-serif);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--kal-text-strong);
  margin: 0;
}
.kmc-empty p {
  color: var(--kal-text-muted);
  font-size: 13.5px;
  line-height: 1.85;
  max-width: 540px;
  margin: 0;
}

@media (max-width: 768px) {
  .kmc-profile { flex-direction: column; align-items: flex-start; padding: 22px; gap: 22px; }
  .kmc-profile-stats { padding-left: 0; padding-top: 22px; border-left: none; border-top: 1px solid var(--kal-border); width: 100%; gap: 28px; }
  .kmc-tabs { width: 100%; overflow-x: auto; }
  .kmc-project, .kmc-personal, .kmc-settings { padding: 22px; }
  .kmc-ws-grid { grid-template-columns: 1fr; }
}
</style>
