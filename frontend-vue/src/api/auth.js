import api from './client'

export const authApi = {
  login: (payload) => api.post('/auth/login', payload),
  register: (payload) => api.post('/auth/register', payload),
  resetPassword: (payload) => api.post('/auth/reset-password', payload),
  adminLogin: (payload) => api.post('/auth/admin-login', payload),
  emailCode: (payload) => api.post('/auth/email-code', payload),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout'),
  notifyEmail: (enabled) => api.patch('/auth/notify-email', { enabled }),
}

export const captchaApi = {
  fetch: () => api.get('/public/captcha'),
}
