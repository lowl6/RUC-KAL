import api from './client'

export const adminApi = {
  stats: () => api.get('/admin/stats'),

  listUsers: (params) => api.get('/admin/users', { params }),
  createUser: (payload) => api.post('/admin/users', payload),
  updateUserStatus: (id, status, reason) =>
    api.patch(`/admin/users/${id}/status`, { status, reason }),
  updateUserRole: (id, role, perms) =>
    api.patch(`/admin/users/${id}/role`, { role, perms }),
  resetUserPassword: (id) => api.post(`/admin/users/${id}/reset-password`),

  listProjects: (params) => api.get('/admin/projects', { params }),
  updateProjectStatus: (id, status, reason) =>
    api.patch(`/admin/projects/${id}/status`, { status, reason }),

  listPosts: (params) => api.get('/admin/forum/posts', { params }),
  updatePostStatus: (id, status, reason) =>
    api.patch(`/admin/forum/posts/${id}/status`, { status, reason }),
  pinPost: (id, pinned) =>
    api.patch(`/admin/forum/posts/${id}/pin`, null, { params: { pinned } }),
  listComments: (id) => api.get(`/admin/forum/posts/${id}/comments`),
  removeComment: (id) => api.delete(`/admin/forum/comments/${id}`),

  listCompetitions: () => api.get('/admin/competitions'),
  getCompetition: (id) => api.get(`/admin/competitions/${id}`),
  upsertCompetition: (payload) => api.post('/admin/competitions', payload),
  deleteCompetition: (id) => api.delete(`/admin/competitions/${id}`),

  listNews: (params) => api.get('/admin/news', { params }),
  upsertNews: (payload) => api.post('/admin/news', payload),
  deleteNews: (id) => api.delete(`/admin/news/${id}`),
  importNewsLinks: (payload) => api.post('/admin/news/import-links', payload),

  auditLogs: (params) => api.get('/admin/audit-logs', { params }),
}
