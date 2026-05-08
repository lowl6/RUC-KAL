import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { tokenStore } from '@/api/client'

export const useAuthStore = defineStore('auth', () => {
  const me = ref(tokenStore.user)
  const token = ref(tokenStore.token)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() =>
    me.value && (me.value.role === 'admin' || me.value.role === 'super_admin')
  )
  const isSuperAdmin = computed(() => me.value?.role === 'super_admin')
  const isStaff = computed(() => me.value?.role === 'staff')
  /** 工作人员视角：staff / admin / super_admin 都能进工作台 */
  const isStaffLevel = computed(() =>
    me.value && (me.value.role === 'staff' || me.value.role === 'admin' || me.value.role === 'super_admin')
  )

  function _setToken (data) {
    token.value = data.token
    me.value = data.user
    tokenStore.token = data.token
    tokenStore.user = data.user
  }

  async function login (payload) {
    const data = await authApi.login(payload)
    _setToken(data)
    return data
  }

  async function register (payload) {
    const data = await authApi.register(payload)
    _setToken(data)
    return data
  }

  async function resetPassword (payload) {
    const data = await authApi.resetPassword(payload)
    _setToken(data)
    return data
  }

  async function adminLogin (payload) {
    const data = await authApi.adminLogin(payload)
    _setToken(data)
    return data
  }

  async function refreshMe () {
    if (!token.value) return null
    try {
      const u = await authApi.me()
      me.value = u
      tokenStore.user = u
      return u
    } catch (e) {
      logout()
      return null
    }
  }

  async function setNotifyEmail (enabled) {
    await authApi.notifyEmail(enabled)
    if (me.value) {
      me.value = { ...me.value, notifyEmail: enabled }
      tokenStore.user = me.value
    }
  }

  function logout () {
    token.value = null
    me.value = null
    tokenStore.clear()
  }

  return {
    me, token, isLoggedIn, isAdmin, isSuperAdmin, isStaff, isStaffLevel,
    login, register, resetPassword, adminLogin, refreshMe, logout, setNotifyEmail,
  }
})
