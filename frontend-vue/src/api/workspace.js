import api from './client'

/**
 * 「我的项目」工作空间 + 支持工单 API。
 *
 * 概念区分：
 *  - 项目卡（projects）= 招募广告
 *  - 项目（workspaces）= 真正在做的项目（成员共享）
 *  - 工单（tickets）   = 项目向工作人员的求助会话
 */
export const workspaceApi = {
  // ============ 项目（成员侧） ============
  mine: () => api.get('/workspaces/mine'),
  detail: (id) => api.get(`/workspaces/${id}`),
  create: (payload) => api.post('/workspaces', payload),
  update: (id, payload) => api.patch(`/workspaces/${id}`, payload),
  patchProgress: (id, payload) => api.patch(`/workspaces/${id}/progress`, payload),

  addMember: (id, payload) => api.post(`/workspaces/${id}/members`, payload),
  removeMember: (id, uid) => api.delete(`/workspaces/${id}/members/${uid}`),

  createMilestone: (id, payload) => api.post(`/workspaces/${id}/milestones`, payload),
  updateMilestone: (id, mid, payload) => api.patch(`/workspaces/${id}/milestones/${mid}`, payload),
  deleteMilestone: (id, mid) => api.delete(`/workspaces/${id}/milestones/${mid}`),

  // ============ 工单（成员发起、双方对话） ============
  openTicket: (workspaceId, payload) => api.post(`/workspaces/${workspaceId}/tickets`, payload),
  ticketDetail: (tid) => api.get(`/tickets/${tid}`),
  reply: (tid, content) => api.post(`/tickets/${tid}/messages`, { content }),
  setTicketStatus: (tid, status) => api.patch(`/tickets/${tid}/status`, { status }),

  // ============ 工作人员（staff / admin / super_admin） ============
  staffTickets: (params) => api.get('/staff/tickets', { params }),
  staffStats:   ()       => api.get('/staff/stats'),
}

/* ---------------- 视图常量（前后端共用） ---------------- */

export const PHASES = [
  { value: 'idea',       label: '构想期', desc: '问题与方向尚在打磨' },
  { value: 'planning',   label: '立项期', desc: '在写立项书 / 排期' },
  { value: 'executing',  label: '执行期', desc: '正在做主体工作' },
  { value: 'polishing',  label: '打磨期', desc: '答辩材料 / 报告冲刺' },
  { value: 'submitted',  label: '已提交', desc: '已提交到比赛 / 等结果' },
]

export const PHASE_LABEL = Object.fromEntries(PHASES.map(p => [p.value, p.label]))

export const STATUSES = {
  active:    { label: '进行中', dot: 'recruiting' },
  archived:  { label: '已归档', dot: 'closed' },
  completed: { label: '已结题', dot: 'completed' },
}

export const TICKET_CATEGORIES = [
  { value: 'advisor',  label: '导师推荐', desc: '希望对接合适的指导老师' },
  { value: 'funding',  label: '经费支持', desc: '差旅 / 报销 / 立项资助' },
  { value: 'material', label: '物资场地', desc: '设备借用、场地预约' },
  { value: 'other',    label: '其他', desc: '一切其他项目相关求助' },
]

export const TICKET_CATEGORY_LABEL = Object.fromEntries(TICKET_CATEGORIES.map(c => [c.value, c.label]))

export const TICKET_STATUSES = {
  open:        { label: '待响应',   dot: 'urgent'      },
  in_progress: { label: '处理中',   dot: 'recruiting'  },
  resolved:    { label: '已解决',   dot: 'completed'   },
  closed:      { label: '已关闭',   dot: 'closed'      },
}

export const MILESTONE_STATUSES = {
  pending: { label: '未开始', dot: 'closed' },
  doing:   { label: '进行中', dot: 'recruiting' },
  done:    { label: '已完成', dot: 'completed' },
}
