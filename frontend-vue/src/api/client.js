import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1'

const api = axios.create({
  baseURL,
  timeout: 15000,
})

const TOKEN_KEY = 'kal.token'
const USER_KEY  = 'kal.user'

export const tokenStore = {
  get token () { return localStorage.getItem(TOKEN_KEY) },
  set token (v) { v ? localStorage.setItem(TOKEN_KEY, v) : localStorage.removeItem(TOKEN_KEY) },
  get user () {
    const raw = localStorage.getItem(USER_KEY)
    try { return raw ? JSON.parse(raw) : null } catch { return null }
  },
  set user (v) {
    v ? localStorage.setItem(USER_KEY, JSON.stringify(v))
      : localStorage.removeItem(USER_KEY)
  },
  clear () { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY) }
}

api.interceptors.request.use((cfg) => {
  const t = tokenStore.token
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

api.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      const err = new Error(body.message || '请求失败')
      err.code = body.code
      throw err
    }
    return body
  },
  (error) => {
    if (error.response?.status === 401) {
      tokenStore.clear()
      if (!location.pathname.startsWith('/login') && !location.pathname.startsWith('/admin/login')) {
        const next = location.pathname.startsWith('/admin') ? '/admin/login' : '/login'
        location.href = next
      }
    }
    const msg = error.response?.data?.message || error.message || '网络异常'
    const err = new Error(msg)
    err.code = error.response?.data?.code || error.response?.status || 0
    return Promise.reject(err)
  }
)

export default api
