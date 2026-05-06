<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'
import Captcha from '@/components/Captcha.vue'

const router = useRouter()
const route  = useRoute()
const auth = useAuthStore()

const form = ref({ email: '', password: '' })
const captcha = ref({ id: '', code: '' })
const captchaRef = ref(null)
const loading = ref(false)
const error = ref('')

const formValid = computed(() =>
  form.value.email && form.value.password.length >= 6 && !!captcha.value.code)

async function submit () {
  error.value = ''
  loading.value = true
  try {
    await auth.adminLogin({
      email: form.value.email.trim(),
      password: form.value.password,
      captchaId: captcha.value.id,
      captchaCode: captcha.value.code,
    })
    const redirect = route.query.redirect || '/admin/competitions'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message || '登录失败'
    captchaRef.value?.refresh()
    captcha.value.code = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="al">
    <div class="al-bg" aria-hidden="true"></div>
    <main class="al-main">
      <div class="al-card">
        <div class="al-brand">
          <span class="al-mark">KAL</span>
          <div>
            <div class="al-brand-title">知行创坊 · 管理后台</div>
            <div class="al-brand-sub">KNOWACT&nbsp;LAB · ADMIN&nbsp;CONSOLE</div>
          </div>
        </div>

        <h2 class="al-title">管理员登录</h2>
        <p class="al-desc">仅授权管理员可登录此入口。所有操作均会被审计。</p>

        <form class="al-form" @submit.prevent="submit">
          <div class="al-field">
            <label class="kal-label kal-label-required">管理员账号</label>
            <input class="kal-input" v-model="form.email" type="email" placeholder="admin@ruc.edu.cn" autocomplete="username" />
          </div>
          <div class="al-field">
            <label class="kal-label kal-label-required">密码</label>
            <input class="kal-input" v-model="form.password" type="password" autocomplete="current-password" />
          </div>

          <div class="al-field">
            <label class="kal-label kal-label-required">图形验证码</label>
            <Captcha ref="captchaRef" v-model="captcha" />
          </div>

          <Transition name="kal-page">
            <div v-if="error" class="al-error">
              <Icon name="alert" :size="14" /><span>{{ error }}</span>
            </div>
          </Transition>

          <button class="kal-btn kal-btn-lg kal-btn-block al-submit" :disabled="!formValid || loading">
            <span v-if="loading" class="al-spin"><Icon name="settings" :size="14" /></span>
            <span>{{ loading ? '验证中…' : '进入管理后台' }}</span>
            <Icon v-if="!loading" name="arrow-right" :size="14" :stroke="2" />
          </button>
        </form>

        <div class="al-hint">
          <Icon name="shield" :size="13" />
          <span>本系统启用 JWT + 角色鉴权 + 审计日志</span>
        </div>

        <div class="al-foot">
          <a href="/" class="al-back">← 返回前台</a>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.al { min-height: 100vh; position: relative; display: flex; align-items: center; justify-content: center; background: var(--kal-paper); padding: 32px 16px; }
.al-bg {
  position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(700px 500px at 12% 8%, rgba(134, 26, 18, 0.08), transparent 60%),
    radial-gradient(600px 400px at 88% 92%, rgba(163, 123, 61, 0.10), transparent 60%);
}
.al-main { position: relative; width: 100%; max-width: 460px; }
.al-card {
  background: var(--kal-surface);
  border: 1px solid var(--kal-border);
  border-radius: var(--kal-radius-md);
  padding: 44px 40px 32px;
  box-shadow: 0 24px 56px rgba(97, 11, 8, 0.06);
}
.al-brand { display: flex; align-items: center; gap: 14px; margin-bottom: 36px; padding-bottom: 24px; border-bottom: 1px dashed var(--kal-border); }
.al-mark {
  width: 44px; height: 44px;
  background: var(--kal-ink); color: #fff;
  border-radius: var(--kal-radius-sm);
  font-family: var(--kal-font-serif); font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center;
  letter-spacing: 2px; font-size: 14px;
}
.al-brand-title { font-family: var(--kal-font-serif); font-weight: 600; font-size: 16px; letter-spacing: 4px; color: var(--kal-text-strong); }
.al-brand-sub { font-size: 10px; letter-spacing: 3px; color: var(--kal-text-subtle); margin-top: 4px; }

.al-title { font-family: var(--kal-font-serif); font-size: 28px; letter-spacing: 4px; color: var(--kal-text-strong); margin-bottom: 8px; font-weight: 600; }
.al-desc { color: var(--kal-text-muted); font-size: 13px; line-height: 1.7; margin-bottom: 28px; }

.al-form { display: flex; flex-direction: column; gap: 16px; }
.al-field { display: flex; flex-direction: column; }
.al-error {
  display: flex; align-items: center; gap: 8px;
  background: var(--kal-primary-50); color: var(--kal-primary-700);
  padding: 10px 14px; border-radius: var(--kal-radius-sm);
  font-size: 13px; border-left: 2px solid var(--kal-primary-600);
}
.al-submit {
  background: var(--kal-ink); border-color: var(--kal-ink); color: #fff;
  margin-top: 8px; letter-spacing: 2px; font-weight: var(--kal-fw-medium);
}
.al-submit:hover { background: var(--kal-ink-soft); border-color: var(--kal-ink-soft); }
.al-hint { display: flex; gap: 6px; align-items: center; color: var(--kal-text-subtle); font-size: 11px; letter-spacing: 1px; margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--kal-divider); }
.al-foot { text-align: center; margin-top: 16px; }
.al-back { color: var(--kal-text-subtle); font-size: 12px; letter-spacing: 1px; }
.al-back:hover { color: var(--kal-primary-700); }
.al-spin { display: inline-flex; animation: al-rot 1s linear infinite; }
@keyframes al-rot { to { transform: rotate(360deg); } }
</style>
