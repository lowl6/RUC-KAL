import api from './client'

export const projectsApi = {
  list: (params) => api.get('/public/projects', { params }),
  detail: (id) => api.get(`/public/projects/${id}`),
  create: (payload) => api.post('/projects', payload),
  mine: () => api.get('/projects/mine'),
  remove: (id) => api.delete(`/projects/${id}`),
}

export const personalCardsApi = {
  list: (params) => api.get('/public/personal-cards', { params }),
  detail: (id) => api.get(`/public/personal-cards/${id}`),
  mine: () => api.get('/personal-cards/mine'),
  upsert: (payload) => api.post('/personal-cards', payload),
}

export const competitionsApi = {
  list: () => api.get('/public/competitions'),
  detail: (id) => api.get(`/public/competitions/${id}`),
  news: (id) => api.get(`/public/competitions/${id}/news`),
}

export const newsApi = {
  list: () => api.get('/public/news'),
  detail: (id) => api.get(`/public/news/${id}`),
}

export const forumApi = {
  list: (params) => api.get('/public/forum/posts', { params }),
  detail: (id) => api.get(`/public/forum/posts/${id}`),
  create: (payload) => api.post('/forum/posts', payload),
  like: (id) => api.post(`/forum/posts/${id}/like`),
  comment: (id, content) => api.post(`/forum/posts/${id}/comments`, { content }),
}

export const messagesApi = {
  conversations: () => api.get('/messages/conversations'),
  history: (id) => api.get(`/messages/conversations/${id}`),
  open: (otherUserId) => api.post(`/messages/conversations/${otherUserId}`),
  send: (id, content) => api.post(`/messages/conversations/${id}/send`, { content }),
  unreadCount: () => api.get('/messages/unread-count'),
  markRead: (id) => api.post(`/messages/conversations/${id}/read`),
}

export const reportsApi = {
  create: (payload) => api.post('/reports', payload),
}
