<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'

const auth = useAuthStore()

const filter = reactive({ keyword: '', role: '', status: '' })
const page = ref(1)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)

const newUser = reactive({
  email: '', name: '', deptName: '', grade: '',
  role: 'student', initialPassword: '',
  perms: []
})

async function load () {
  loading.value = true; error.value = ''
  try {
    const r = await adminApi.listUsers({
      keyword: filter.keyword || undefined,
      role: filter.role || undefined,
      status: filter.status || undefined,
      page: page.value, size: 20
    })
    list.value = r.items; total.value = r.total
  } catch (e) { error.value = e.message }
  finally   { loading.value = false }
}
onMounted(load)

async function search () { page.value = 1; await load() }

const lastCreated = ref(null) // { email, initialPassword, external }

async function createUser () {
  try {
    const r = await adminApi.createUser({
      ...newUser,
      perms: newUser.role === 'admin' || newUser.role === 'super_admin'
        ? (newUser.perms || []) : null
    })
    lastCreated.value = {
      email: r?.user?.email || newUser.email,
      initialPassword: r?.initialPassword || newUser.initialPassword,
      external: r?.external,
    }
    showCreate.value = false
    Object.assign(newUser, { email: '', name: '', deptName: '', grade: '', role: 'student', initialPassword: '', perms: [] })
    await load()
  } catch (e) { alert(e.message) }
}

async function copyCredential () {
  if (!lastCreated.value) return
  const text = `账号：${lastCreated.value.email}\n初始密码：${lastCreated.value.initialPassword}\n首次登录后请立即修改密码。`
  try { await navigator.clipboard.writeText(text); alert('已复制到剪贴板') }
  catch (e) { alert(text) }
}

async function ban (u) {
  const reason = prompt(`确定封禁 ${u.displayName || u.email}？请填写原因：`)
  if (reason === null) return
  await adminApi.updateUserStatus(u.userId, 'banned', reason || '违反社区规则')
  await load()
}
async function unban (u) {
  if (!confirm(`恢复 ${u.displayName || u.email}？`)) return
  await adminApi.updateUserStatus(u.userId, 'active', null)
  await load()
}
async function disable (u) {
  if (!confirm(`停用账号 ${u.displayName || u.email}？`)) return
  await adminApi.updateUserStatus(u.userId, 'disabled', '管理员手动停用')
  await load()
}
async function resetPwd (u) {
  if (!confirm(`重置 ${u.email} 的密码？`)) return
  const r = await adminApi.resetUserPassword(u.userId)
  alert(`新密码：${r.temporaryPassword}\n请尽快通过线下渠道交付给用户。`)
}
async function setRole (u) {
  const role = prompt(`将「${u.displayName || u.email}」设为何种角色？\n可选：student / teacher / admin / super_admin`, u.role)
  if (!role) return
  await adminApi.updateUserRole(u.userId, role, [])
  await load()
}

function statusLabel (s) {
  return ({ active: '正常', disabled: '已停用', banned: '已封禁', pending_first_login: '待首登' })[s] || s
}
function roleLabel (r) {
  return ({ student: '学生', teacher: '教师', admin: '管理员', super_admin: '超级管理员' })[r] || r
}
</script>

<template>
  <div class="ad-page">
    <header class="ad-page-head">
      <div>
        <div class="kal-eyebrow">Console / Users</div>
        <h1 class="ad-page-title">账号管理</h1>
      </div>
      <button class="kal-btn kal-btn-primary" @click="showCreate = true">
        <Icon name="plus" :size="14" /> 手动添加账号
      </button>
    </header>

    <!-- 新建结果 -->
    <Transition name="kal-page">
      <div v-if="lastCreated" class="ad-credential">
        <div class="ad-credential-row">
          <Icon name="shield" :size="14" />
          <span class="ad-credential-eyebrow">{{ lastCreated.external ? '校外邀请账号已创建' : '账号已创建' }}</span>
        </div>
        <div class="ad-credential-grid">
          <div><dt>邮箱</dt><dd>{{ lastCreated.email }}</dd></div>
          <div><dt>初始密码</dt><dd class="ad-cred-pwd">{{ lastCreated.initialPassword }}</dd></div>
        </div>
        <div class="ad-credential-actions">
          <button class="kal-btn kal-btn-sm" @click="copyCredential">复制</button>
          <button class="kal-btn kal-btn-sm kal-btn-secondary" @click="lastCreated = null">关闭</button>
        </div>
        <p class="ad-credential-hint">为安全起见，请通过线下或加密渠道交付该信息。用户首次登录后请引导其修改密码。</p>
      </div>
    </Transition>

    <div class="ad-filter">
      <input class="kal-input" v-model="filter.keyword" placeholder="搜索邮箱 / 姓名" @keyup.enter="search" />
      <select class="kal-input" v-model="filter.role" @change="search">
        <option value="">全部角色</option>
        <option value="student">学生</option>
        <option value="teacher">教师</option>
        <option value="admin">管理员</option>
        <option value="super_admin">超级管理员</option>
      </select>
      <select class="kal-input" v-model="filter.status" @change="search">
        <option value="">全部状态</option>
        <option value="active">正常</option>
        <option value="disabled">已停用</option>
        <option value="banned">已封禁</option>
      </select>
      <button class="kal-btn kal-btn-secondary" @click="search"><Icon name="search" :size="14" /> 搜索</button>
    </div>

    <div v-if="error" class="ad-alert">{{ error }}</div>

    <div class="ad-table-wrap">
      <table class="ad-table">
        <thead>
          <tr>
            <th>账号</th><th>角色</th><th>院系 / 年级</th><th>状态</th><th style="width: 240px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="5" class="ad-empty">加载中…</td></tr>
          <tr v-else-if="!list.length"><td colspan="5" class="ad-empty">暂无账号</td></tr>
          <tr v-for="u in list" :key="u.userId">
            <td>
              <div class="ad-name">{{ u.displayName || u.name }}</div>
              <div class="ad-sub">{{ u.email }}</div>
            </td>
            <td><span class="ad-tag">{{ roleLabel(u.role) }}</span></td>
            <td>{{ u.deptName || '—' }} <span v-if="u.grade" class="ad-grade">· {{ u.grade }}</span></td>
            <td><span class="ad-dot" :class="`is-${u.status}`"></span>{{ statusLabel(u.status) }}</td>
            <td class="ad-ops">
              <button v-if="u.status !== 'banned'" class="ad-link-btn" @click="ban(u)">封禁</button>
              <button v-else class="ad-link-btn" @click="unban(u)">解封</button>
              <button v-if="u.status === 'active'" class="ad-link-btn" @click="disable(u)">停用</button>
              <button class="ad-link-btn" @click="resetPwd(u)">重置密码</button>
              <button v-if="auth.isSuperAdmin" class="ad-link-btn" @click="setRole(u)">改角色</button>
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

    <!-- 创建用户弹层 -->
    <Transition name="kal-page">
      <div v-if="showCreate" class="ad-modal" @click.self="showCreate = false">
        <div class="ad-modal-card">
          <header class="ad-modal-head">
            <h3>手动添加账号</h3>
            <button class="ad-icon-btn" @click="showCreate = false"><Icon name="close" :size="16" /></button>
          </header>
          <div class="ad-modal-body">
            <div class="kal-field">
              <label class="kal-label kal-label-required">邮箱</label>
              <input class="kal-input" v-model="newUser.email" type="email" placeholder="可使用任意邮箱（含校外）" />
              <div class="kal-hint">校内邮箱（@ruc.edu.cn）或校外邮箱均可。校外邮箱用于嘉宾导师 / 评审 / 合作伙伴等邀请场景。</div>
            </div>
            <div class="kal-field">
              <label class="kal-label kal-label-required">姓名</label>
              <input class="kal-input" v-model="newUser.name" />
            </div>
            <div class="kal-grid-2">
              <div class="kal-field">
                <label class="kal-label">院系</label>
                <input class="kal-input" v-model="newUser.deptName" />
              </div>
              <div class="kal-field">
                <label class="kal-label">年级</label>
                <input class="kal-input" v-model="newUser.grade" placeholder="如：2023级" />
              </div>
            </div>
            <div class="kal-field">
              <label class="kal-label">角色</label>
              <select class="kal-input" v-model="newUser.role">
                <option value="student">学生</option>
                <option value="teacher">教师</option>
                <option v-if="auth.isSuperAdmin" value="admin">管理员</option>
                <option v-if="auth.isSuperAdmin" value="super_admin">超级管理员</option>
              </select>
            </div>
            <div class="kal-field">
              <label class="kal-label">初始密码</label>
              <input class="kal-input" v-model="newUser.initialPassword" placeholder="留空则系统自动生成强密码（推荐）" />
              <div class="kal-hint">建议留空：系统会生成形如 <code>Kal-A7q9-Pn3K</code> 的随机强密码，并在创建后展示给你一次。</div>
            </div>
          </div>
          <footer class="ad-modal-foot">
            <button class="kal-btn kal-btn-secondary" @click="showCreate = false">取消</button>
            <button class="kal-btn kal-btn-primary" @click="createUser">创建</button>
          </footer>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped src="./admin-shared.css"></style>
