<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import Icon from '@/components/Icon.vue'
import Captcha from '@/components/Captcha.vue'

const router = useRouter()
const route  = useRoute()
const auth = useAuthStore()

const mode = ref('login') // 'login' | 'register' | 'reset'
const currentYear = new Date().getFullYear()
const admissionYears = Array.from({ length: 9 }, (_, i) => String(currentYear - i))
// 仅这三类可以自助注册；「工作人员」「校外」必须由管理员后台手动开通
const degreeTypes = ['本科', '研究生', '教师']
const form = ref({
  name: '', email: '', password: '',
  deptName: '', admissionYear: String(currentYear), degreeType: '本科', emailCode: ''
})
const isTeacher = computed(() => form.value.degreeType === '教师')
const captcha = ref({ id: '', code: '' })
const captchaRef = ref(null)
const degreeOpen = ref(false)
const loading = ref(false)
const error = ref('')

// 邮箱验证码倒计时
const codeCountdown = ref(0)
const codeSending = ref(false)
let codeTimer = null

const emailSyntaxValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email.trim()))
const rucEmailValid = computed(() => /@ruc\.edu\.cn$/i.test(form.value.email.trim()))
const emailValid = computed(() => mode.value === 'register' ? rucEmailValid.value : emailSyntaxValid.value)
const formValid = computed(() => {
  if (!emailValid.value) return false
  if (form.value.password.length < (mode.value === 'register' ? 8 : 6)) return false
  if (mode.value === 'login' && !captcha.value.code) return false
  if (mode.value === 'register') {
    if (form.value.name.trim().length < 2) return false
    if (!form.value.degreeType) return false
    // 教师无需"入学年份"；学生必须选择
    if (!isTeacher.value && !form.value.admissionYear) return false
    if (!form.value.emailCode || form.value.emailCode.length < 4) return false
  }
  if (mode.value === 'reset') {
    if (form.value.password.length < 8) return false
    if (!form.value.emailCode || form.value.emailCode.length < 4) return false
  }
  return true
})

const admissionLabel = computed(() =>
  isTeacher.value
    ? '教师'
    : `${form.value.admissionYear} 入学 · ${form.value.degreeType}`
)

async function sendEmailCode () {
  error.value = ''
  if (!emailValid.value) {
    return error.value = mode.value === 'register' ? '请使用 @ruc.edu.cn 校内邮箱' : '请填写正确的邮箱'
  }
  if (!captcha.value.code) return error.value = '请先完成右侧图形验证码'
  codeSending.value = true
  try {
    await authApi.emailCode({
      email: form.value.email.trim(),
      captchaId: captcha.value.id,
      captchaCode: captcha.value.code,
      purpose: mode.value === 'reset' ? 'reset' : 'register',
    })
    codeCountdown.value = 60
    codeTimer = setInterval(() => {
      codeCountdown.value -= 1
      if (codeCountdown.value <= 0) { clearInterval(codeTimer); codeTimer = null }
    }, 1000)
    captchaRef.value?.refresh()
    captcha.value.code = ''
  } catch (e) {
    error.value = e.message || '发送失败，请稍后再试'
    captchaRef.value?.refresh()
    captcha.value.code = ''
  } finally {
    codeSending.value = false
  }
}

async function submit () {
  error.value = ''
  if (!emailValid.value) {
    return error.value = mode.value === 'register' ? '请使用 @ruc.edu.cn 校内邮箱' : '请填写正确的邮箱'
  }
  if (form.value.password.length < (mode.value === 'register' ? 8 : 6))
    return error.value = mode.value === 'login' ? '密码至少 6 位' : '密码至少 8 位'
  if (mode.value === 'login' && !captcha.value.code) return error.value = '请输入右侧图形验证码'
  if (mode.value === 'register' && !form.value.name.trim()) return error.value = '请填写姓名'

  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({
        email: form.value.email.trim(),
        password: form.value.password,
        captchaId: captcha.value.id,
        captchaCode: captcha.value.code,
      })
    } else if (mode.value === 'register') {
      await auth.register({
        email: form.value.email.trim(),
        password: form.value.password,
        name: form.value.name.trim(),
        deptName: form.value.deptName,
        grade: isTeacher.value ? '教师' : admissionLabel.value,
        degreeType: form.value.degreeType,
        emailCode: form.value.emailCode.trim(),
        captchaId: captcha.value.id,
        captchaCode: captcha.value.code,
      })
    } else {
      await auth.resetPassword({
        email: form.value.email.trim(),
        password: form.value.password,
        emailCode: form.value.emailCode.trim(),
      })
    }
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message || '提交失败'
    captchaRef.value?.refresh()
    captcha.value.code = ''
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => { if (codeTimer) clearInterval(codeTimer) })

/* 隐藏的管理员入口：连击 LOGO 5 次进入 /admin/login */
const brandClicks = ref(0)
let brandTimer = null
function brandClick () {
  brandClicks.value += 1
  clearTimeout(brandTimer)
  brandTimer = setTimeout(() => brandClicks.value = 0, 1500)
  if (brandClicks.value >= 5) {
    brandClicks.value = 0
    router.push('/admin/login')
  }
}

function chooseDegree (value) {
  form.value.degreeType = value
  degreeOpen.value = false
}

function onDocClick (e) {
  if (!degreeOpen.value) return
  if (e.target.closest && e.target.closest('.kl-degree')) return
  degreeOpen.value = false
}
onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))

const features = [
  { num: '01', title: '项目卡', desc: '面向他人，描述构想与所需角色' },
  { num: '02', title: '个人卡', desc: '陈列你的专长与可投入节奏' },
  { num: '03', title: '私信',   desc: '保持距离，沟通后再交换微信' },
  { num: '04', title: '论坛',   desc: '攻略、问答、复盘的公共留白' }
]
</script>

<template>
  <div class="kl">
    <!-- 左侧：编辑式封面 -->
    <aside class="kl-aside" aria-hidden="true">
      <div class="kl-aside-inner">
        <div class="kl-brand" @click="brandClick" title="知行创坊">
          <span class="kl-brand-mark">KAL</span>
          <div>
            <h1 class="kl-brand-title">知行创坊</h1>
            <p class="kl-brand-sub">KNOWACT&nbsp;LAB · RUC</p>
          </div>
        </div>

        <div class="kl-quote">
          <span class="kl-quote-eyebrow">A&nbsp;Workshop&nbsp;of&nbsp;Knowing&nbsp;&amp;&nbsp;Doing</span>
          <h2 class="kl-quote-title">
            <span>知</span>所往，<span>行</span>所至，<br/>
            <span>创</span>所信，<span>坊</span>所聚。
          </h2>
          <p class="kl-quote-desc">人大学子的三创合作之所——构想在此凝结，协作在此生长。</p>
        </div>

        <ul class="kl-feats">
          <li v-for="f in features" :key="f.num">
            <span class="kl-feat-num">{{ f.num }}</span>
            <span class="kl-feat-text">
              <strong>{{ f.title }}</strong>
              <em>{{ f.desc }}</em>
            </span>
          </li>
        </ul>

        <div class="kl-footer">
          © Renmin University of China
        </div>
      </div>
    </aside>

    <!-- 右侧：表单 -->
    <main class="kl-main">
      <div class="kl-card">
        <div class="kal-eyebrow kl-card-eyebrow">Sign&nbsp;In&nbsp;/&nbsp;Register</div>
        <h2 class="kl-title">{{ mode === 'login' ? '登录' : (mode === 'register' ? '注册' : '找回密码') }}</h2>
        <p class="kl-desc">
          <span v-if="mode === 'register'">仅限 <strong>@ruc.edu.cn</strong> 校内邮箱自助注册。</span>
          <span v-else-if="mode === 'reset'">通过注册邮箱接收验证码，重设后将自动登录。</span>
          <span v-else>请输入账号邮箱与密码。</span>
          <span v-if="mode === 'login'">尚未注册？</span>
          <span v-else>已有账号？</span>
          <a href="#" class="kl-toggle" @click.prevent="mode = mode === 'login' ? 'register' : 'login'; error = ''; form.emailCode = ''">
            {{ mode === 'login' ? '前往注册' : '返回登录' }}
          </a>
        </p>

        <form class="kl-form" @submit.prevent="submit">
          <div v-if="mode === 'register'" class="kl-field">
            <label class="kal-label kal-label-required" for="kl-name">姓名</label>
            <input id="kl-name" class="kal-input" v-model="form.name" placeholder="如实填写真实姓名" autocomplete="name" />
          </div>

          <div class="kl-field">
            <label class="kal-label kal-label-required" for="kl-email">校内邮箱</label>
            <input
              id="kl-email"
              class="kal-input"
              v-model="form.email"
              type="email"
              placeholder="example@ruc.edu.cn"
              autocomplete="email"
            />
          </div>

          <div class="kl-field">
            <label class="kal-label kal-label-required" for="kl-pwd">密码</label>
            <input
              id="kl-pwd"
              class="kal-input"
              v-model="form.password"
              type="password"
              :placeholder="mode === 'login' ? '密码' : '至少 8 位，建议混合字母数字'"
              :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            />
            <a v-if="mode === 'login'" href="#" class="kl-forgot"
               @click.prevent="mode = 'reset'; error = ''; form.emailCode = ''; captchaRef?.refresh?.(); captcha.code = ''">
              忘记密码？
            </a>
          </div>

          <div v-if="mode === 'register'" class="kl-row">
            <div class="kl-field">
              <label class="kal-label" for="kl-dept">学院</label>
              <input id="kl-dept" class="kal-input" v-model="form.deptName" placeholder="如：信息学院" />
            </div>
            <div class="kl-field">
              <label class="kal-label kal-label-required" for="kl-admission-year">
                {{ isTeacher ? '身份' : '入学时间' }}
              </label>
              <div class="kl-admission">
                <select
                  v-if="!isTeacher"
                  id="kl-admission-year"
                  class="kl-admission-year"
                  v-model="form.admissionYear"
                >
                  <option v-for="y in admissionYears" :key="y" :value="y">{{ y }}</option>
                </select>
                <div v-else class="kl-admission-teacher">
                  <Icon name="shield" :size="12" />
                  <span>本校教师</span>
                </div>
                <div class="kl-degree">
                  <button
                    type="button"
                    class="kl-degree-trigger"
                    :class="{ 'is-open': degreeOpen }"
                    aria-haspopup="listbox"
                    :aria-expanded="degreeOpen"
                    @click="degreeOpen = !degreeOpen"
                  >
                    <span>{{ form.degreeType }}</span>
                    <Icon name="chevron-down" :size="12" />
                  </button>
                  <Transition name="kal-page">
                    <div v-if="degreeOpen" class="kl-degree-drawer" role="listbox">
                      <button
                        v-for="t in degreeTypes"
                        :key="t"
                        type="button"
                        class="kl-degree-option"
                        :class="{ 'is-active': form.degreeType === t }"
                        role="option"
                        :aria-selected="form.degreeType === t"
                        @click="chooseDegree(t)"
                      >
                        <span>{{ t }}</span>
                        <Icon v-if="form.degreeType === t" name="check" :size="12" />
                      </button>
                    </div>
                  </Transition>
                </div>
              </div>
              <p class="kl-hint">将显示为：{{ admissionLabel }}</p>
            </div>
          </div>

          <!-- 图形验证码 -->
          <div class="kl-field">
            <label class="kal-label kal-label-required">图形验证码</label>
            <Captcha ref="captchaRef" v-model="captcha" />
          </div>

          <!-- 邮箱验证码（注册 / 找回密码） -->
          <div v-if="mode === 'register' || mode === 'reset'" class="kl-field">
            <label class="kal-label kal-label-required" for="kl-ecode">邮箱验证码</label>
            <div class="kl-emailcode">
              <input
                id="kl-ecode"
                class="kal-input"
                v-model="form.emailCode"
                placeholder="6 位数字"
                maxlength="6"
                autocomplete="one-time-code"
              />
              <button
                type="button"
                class="kal-btn kal-btn-secondary kl-emailcode-btn"
                :disabled="codeSending || codeCountdown > 0"
                @click="sendEmailCode"
              >
                <span v-if="codeSending">发送中…</span>
                <span v-else-if="codeCountdown > 0">{{ codeCountdown }}s 后重试</span>
                <span v-else>发送验证码</span>
              </button>
            </div>
            <p class="kl-hint">验证码将发至上方邮箱，10 分钟内有效。</p>
          </div>

          <Transition name="kal-page">
            <div v-if="error" class="kl-error">
              <Icon name="alert" :size="14" />
              <span>{{ error }}</span>
            </div>
          </Transition>

          <button class="kal-btn kal-btn-lg kal-btn-block kl-submit" :disabled="!formValid || loading">
            <span v-if="loading" class="kl-spin"><Icon name="settings" :size="14" /></span>
            <span>{{ loading ? '验证中…' : (mode === 'login' ? '进入工作坊' : (mode === 'register' ? '注册并登录' : '重置并登录')) }}</span>
            <Icon v-if="!loading" name="arrow-right" :size="14" :stroke="2" />
          </button>
        </form>

        <div class="kl-divider"><span>其他方式</span></div>

        <button class="kal-btn kal-btn-secondary kal-btn-block" disabled>
          <Icon name="shield" :size="14" />
          <span>人大统一身份认证 · 即将上线</span>
        </button>

        <p class="kl-note">
          登录即视为同意 <a href="#">服务条款</a> 与 <a href="#">隐私政策</a>
          <br/><br/>
          <span class="kl-meta">工作人员 / 校外嘉宾导师请联系管理员邀请开通账号</span>
        </p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.kl {
  display: flex;
  width: 100%;
  min-height: 100vh;
  background: var(--kal-bg);
}

/* ---------- 左侧封面 ---------- */
.kl-aside {
  flex: 1;
  position: relative;
  display: none;
  background: var(--kal-ink);
  color: #fff;
  overflow: hidden;
}
.kl-aside::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(700px 500px at 100% 0%, rgba(134, 26, 18, 0.55), transparent 60%),
    radial-gradient(500px 400px at 0% 100%, rgba(163, 123, 61, 0.20), transparent 60%);
  pointer-events: none;
}
.kl-aside::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 4px 4px;
  pointer-events: none;
  opacity: 0.6;
}
.kl-aside-inner {
  position: relative;
  z-index: 1;
  padding: 56px 56px 48px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 56px;
}
.kl-brand { display: flex; align-items: center; gap: 14px; cursor: pointer; user-select: none; }
.kl-brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.94);
  color: var(--kal-ink);
  border-radius: var(--kal-radius-sm);
  font-family: var(--kal-font-serif);
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 2px;
}
.kl-brand-title {
  font-family: var(--kal-font-serif);
  font-weight: 600;
  font-size: 20px;
  letter-spacing: 6px;
  margin: 0;
}
.kl-brand-sub {
  font-size: 10px;
  letter-spacing: 3.5px;
  opacity: 0.55;
  margin: 0;
  margin-top: 4px;
}

.kl-quote-eyebrow {
  display: inline-flex;
  font-size: 11px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 500;
  margin-bottom: 28px;
  padding-left: 32px;
  position: relative;
}
.kl-quote-eyebrow::before {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  width: 24px; height: 1px;
  background: rgba(255, 255, 255, 0.4);
}
.kl-quote-title {
  font-family: var(--kal-font-serif);
  font-size: 46px;
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: 4px;
  margin: 0 0 24px;
}
.kl-quote-title span {
  font-weight: 700;
  color: #fff;
  font-size: 1.05em;
  padding: 0 2px;
  position: relative;
}
.kl-quote-title span::after {
  content: '';
  position: absolute;
  inset: auto 4px -4px 4px;
  height: 6px;
  background: rgba(217, 106, 88, 0.35);
  z-index: -1;
}
.kl-quote-desc {
  font-size: 14px;
  opacity: 0.7;
  line-height: 1.85;
  max-width: 400px;
  margin: 0;
  letter-spacing: 1px;
}

.kl-feats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 32px;
  margin-top: auto;
  padding-top: 32px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.kl-feats li {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.kl-feat-num {
  font-family: var(--kal-font-serif);
  font-style: italic;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
  flex-shrink: 0;
  margin-top: 1px;
}
.kl-feat-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.kl-feat-text strong {
  font-family: var(--kal-font-serif);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #fff;
}
.kl-feat-text em {
  font-style: normal;
  font-size: 12px;
  opacity: 0.55;
  letter-spacing: 0.5px;
  line-height: 1.6;
}

.kl-footer {
  font-size: 11px;
  opacity: 0.4;
  letter-spacing: 2.5px;
  margin-top: 24px;
}

/* ---------- 右侧表单 ---------- */
.kl-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: var(--kal-bg);
}
.kl-card {
  width: 100%;
  max-width: 460px;
  background: var(--kal-surface);
  border-radius: var(--kal-radius-md);
  padding: 48px 44px 40px;
  border: 1px solid var(--kal-border);
}
.kl-card-eyebrow { color: var(--kal-text-subtle); margin-bottom: 16px; }
.kl-title {
  font-family: var(--kal-font-serif);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 4px;
  color: var(--kal-text-strong);
  margin-bottom: 10px;
}
.kl-desc { font-size: 13px; color: var(--kal-text-muted); margin-bottom: 32px; line-height: 1.7; }
.kl-form { display: flex; flex-direction: column; gap: 18px; }
.kl-field { display: flex; flex-direction: column; }
.kl-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.kl-toggle { color: var(--kal-primary-700); border-bottom: 1px solid currentColor; padding-bottom: 1px; }
.kl-forgot {
  align-self: flex-end;
  margin-top: 7px;
  font-size: 11.5px;
  letter-spacing: 1px;
  color: var(--kal-text-subtle);
  transition: color var(--kal-duration-2);
}
.kl-forgot:hover { color: var(--kal-primary-700); }

.kl-admission {
  display: grid;
  grid-template-columns: 98px 1fr;
  gap: 8px;
  align-items: stretch;
  padding: 4px;
  min-height: 42px;
  background:
    linear-gradient(180deg, rgba(253, 243, 241, 0.72), rgba(255, 255, 255, 0.92));
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  transition: border-color var(--kal-duration-2), box-shadow var(--kal-duration-2);
}
.kl-admission:focus-within {
  border-color: var(--kal-primary-300);
  box-shadow: 0 0 0 3px rgba(134, 26, 18, 0.08);
}
.kl-admission-year {
  width: 100%;
  height: 34px;
  padding: 0 10px;
  background: transparent;
  border: 0;
  outline: 0;
  color: var(--kal-text-strong);
  font-family: var(--kal-font-serif);
  font-size: 14px;
  letter-spacing: 1px;
  cursor: pointer;
}
.kl-admission-teacher {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 10px;
  font-family: var(--kal-font-serif);
  font-size: 13px;
  color: var(--kal-text-strong);
  letter-spacing: 1px;
  white-space: nowrap;
}
.kl-admission-teacher :deep(.kal-icon) { color: var(--kal-primary-700); }
.kl-degree {
  position: relative;
  padding-left: 6px;
  border-left: 1px solid var(--kal-border);
}
.kl-degree-trigger {
  width: 100%;
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  white-space: nowrap;
  border: 0;
  border-radius: calc(var(--kal-radius-sm) - 2px);
  background: var(--ruc-red);
  color: #fff;
  box-shadow: 0 6px 18px rgba(134, 26, 18, 0.14);
  font-size: 12px;
  letter-spacing: 1.2px;
  cursor: pointer;
  transition: background var(--kal-duration-2), box-shadow var(--kal-duration-2);
}
.kl-degree-trigger > span {
  flex: 1;
  text-align: left;
}
.kl-degree-trigger:hover,
.kl-degree-trigger.is-open {
  background: var(--ruc-red-dark);
  box-shadow: 0 8px 22px rgba(134, 26, 18, 0.20);
}
.kl-degree-trigger :deep(.kal-icon) {
  transition: transform var(--kal-duration-2);
}
.kl-degree-trigger.is-open :deep(.kal-icon) {
  transform: rotate(180deg);
}
.kl-degree-drawer {
  position: absolute;
  z-index: 20;
  top: calc(100% + 8px);
  right: 0;
  min-width: 130px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-sm);
  box-shadow: 0 18px 48px rgba(48, 24, 18, 0.14);
}
.kl-degree-drawer::before {
  content: '';
  position: absolute;
  top: -5px;
  right: 22px;
  width: 8px;
  height: 8px;
  background: #fff;
  border-left: 1px solid var(--kal-border);
  border-top: 1px solid var(--kal-border);
  transform: rotate(45deg);
}
.kl-degree-option {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 12px;
  white-space: nowrap;
  border: 0;
  border-radius: calc(var(--kal-radius-sm) - 2px);
  background: transparent;
  color: var(--kal-text-muted);
  font-size: 13px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: background var(--kal-duration-2), color var(--kal-duration-2);
}
.kl-degree-option:hover {
  background: var(--kal-primary-50);
  color: var(--ruc-red-dark);
}
.kl-degree-option.is-active {
  background: var(--kal-primary-100);
  color: var(--ruc-red);
  font-weight: 600;
}

.kl-emailcode { display: flex; gap: 10px; align-items: stretch; }
.kl-emailcode .kal-input { flex: 1; letter-spacing: 6px; font-family: var(--kal-font-serif); font-size: 16px; }
.kl-emailcode-btn {
  white-space: nowrap;
  padding: 0 16px;
  font-size: 12px;
  letter-spacing: 1px;
  flex-shrink: 0;
}
.kl-hint { margin: 6px 2px 0; font-size: 11px; color: var(--kal-text-subtle); letter-spacing: 0.5px; }

.kl-error {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--kal-primary-50);
  color: var(--kal-primary-700);
  padding: 10px 14px;
  border-radius: var(--kal-radius-sm);
  font-size: var(--kal-text-sm);
  border-left: 2px solid var(--kal-primary-600);
}
.kl-submit {
  background: var(--kal-ink);
  border-color: var(--kal-ink);
  color: #fff;
  margin-top: 8px;
  letter-spacing: 2px;
  font-weight: var(--kal-fw-medium);
}
.kl-submit:hover { background: var(--kal-ink-soft); border-color: var(--kal-ink-soft); }

.kl-divider {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 28px 0 14px;
  color: var(--kal-text-subtle);
  font-size: 11px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}
.kl-divider::before, .kl-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--kal-divider);
}
.kl-note {
  margin-top: 28px;
  text-align: center;
  font-size: 11px;
  color: var(--kal-text-subtle);
  letter-spacing: 0.5px;
}
.kl-note a { color: var(--kal-text); border-bottom: 1px solid currentColor; padding-bottom: 1px; }
.kl-note a:hover { color: var(--kal-primary-700); }
.kl-meta { letter-spacing: 1px; }

.kl-spin { display: inline-flex; animation: kl-rot 1s linear infinite; }
@keyframes kl-rot { to { transform: rotate(360deg); } }

@media (min-width: 1024px) {
  .kl-aside { display: block; flex: 1.1; }
  .kl-main { flex: 0.9; }
}
@media (max-width: 640px) {
  .kl-card { padding: 32px 24px; }
}
</style>
