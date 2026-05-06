import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { normalizeUser } from '@/api/normalize'
import { messagesApi } from '@/api/projects'

export const useUserStore = defineStore('user', () => {
  const auth = useAuthStore()
  const unreadMessages = ref(0)
  const unreadApplications = ref(0)
  const unreadReplies = ref(0)
  let timer = null

  const me = computed(() => auth.me ? normalizeUser(auth.me) : null)
  const isLoggedIn = computed(() => !!me.value)
  const initials = computed(() => me.value?.display_name?.[0] || '我')

  async function refreshUnread () {
    if (!auth.isLoggedIn) {
      unreadMessages.value = 0
      return
    }
    try {
      const r = await messagesApi.unreadCount()
      unreadMessages.value = Number(r?.unread || 0)
    } catch (e) { /* 忽略后端不可用 */ }
  }

  function startUnreadPolling (intervalMs = 30000) {
    stopUnreadPolling()
    refreshUnread()
    timer = setInterval(refreshUnread, intervalMs)
  }
  function stopUnreadPolling () {
    if (timer) { clearInterval(timer); timer = null }
  }

  function login(payload) { return auth.login(payload) }
  function logout() {
    auth.logout()
    unreadMessages.value = 0
    stopUnreadPolling()
  }

  return {
    me, unreadMessages, unreadApplications, unreadReplies,
    isLoggedIn, initials,
    login, logout, refreshUnread, startUnreadPolling, stopUnreadPolling,
  }
})
