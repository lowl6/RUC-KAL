function daysLeft(dateLike) {
  if (!dateLike) return 0
  const end = new Date(dateLike)
  if (Number.isNaN(end.getTime())) return 0
  const diff = end.getTime() - Date.now()
  return Math.max(0, Math.ceil(diff / 86400000))
}

function splitCsv(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  return String(value).split(',').map(s => s.trim()).filter(Boolean)
}

export function normalizeUser(u = {}) {
  return {
    user_id: u.userId || u.user_id,
    email: u.email,
    name: u.name || u.displayName || u.display_name || '同学',
    display_name: u.displayName || u.display_name || u.name || '同学',
    dept_name: u.deptName || u.dept_name || '',
    grade: u.grade || '',
    avatar_url: u.avatarUrl || u.avatar_url || '',
    role: u.role,
    status: u.status,
    perms: u.perms || [],
    wechat_id: u.wechatId || u.wechat_id || '',
  }
}

export function normalizeProject(p = {}) {
  const deadline = p.teamDeadline || p.team_deadline || p.competitionDeadline || p.competition_deadline
  const left = p.days_left ?? daysLeft(deadline)
  const creatorName = p.creator?.name || p.creatorName || p.creatorId || '项目负责人'
  return {
    project_id: p.projectId || p.project_id,
    project_name: p.projectName || p.project_name,
    one_liner: p.oneLiner || p.one_liner || '',
    project_type: p.projectType || p.project_type || '',
    competition_short: p.competitionShort || p.competition_short || p.competitionTarget || p.competition_target || '未指定比赛',
    competition_target: p.competitionTarget || p.competition_target || '',
    competition_deadline: p.competitionDeadline || p.competition_deadline,
    team_deadline: p.teamDeadline || p.team_deadline,
    current_members: p.currentMembers ?? p.current_members ?? 1,
    needed_count: p.neededCount ?? p.needed_count ?? 0,
    weekly_hours: p.weeklyHours ?? p.weekly_hours ?? 0,
    detail: p.detail || '',
    tags: splitCsv(p.tags || p.tagsCsv || p.tags_csv),
    creator_id: p.creatorId || p.creator_id,
    creator: p.creator || {
      initial: creatorName?.[0] || '项',
      name: creatorName,
      dept: p.creatorDept || '人大',
      grade: p.creatorGrade || '',
    },
    view_count: p.viewCount ?? p.view_count ?? 0,
    apply_count: p.applyCount ?? p.apply_count ?? 0,
    status: p.status || 'recruiting',
    days_left: left,
    badge: left <= 7 ? 'urgent' : '',
    roles: (p.roles || []).map(r => ({
      role_name: r.roleName || r.role_name,
      count: r.count ?? 1,
      skills: r.skills || '',
    })),
    applications: p.applications || [],
    related_ids: p.related_ids || [],
  }
}

export function normalizePersonalCard(c = {}) {
  const name = c.displayName || c.display_name || '同学'
  return {
    card_id: c.cardId || c.card_id,
    user_id: c.userId || c.user_id,
    initial: name?.[0] || '我',
    display_name: name,
    dept_name: c.deptName || c.dept_name || '中国人民大学',
    grade: c.grade || '',
    target_role: c.targetRole || c.target_role || '',
    weekly_hours: c.weeklyHours ?? c.weekly_hours ?? 0,
    vacation_available: c.vacationAvailable ?? c.vacation_available ?? false,
    skills: splitCsv(c.skills || c.skillsCsv || c.skills_csv),
    self_intro: c.selfIntro || c.self_intro || '',
    interested_competitions: splitCsv(c.interestedCompetitions || c.interestedCompetitionsCsv || c.interested_competitions),
    visibility: c.visibility || 'public',
    status: c.status || 'active',
    is_new: c.is_new || false,
  }
}

export function normalizeCompetition(c = {}) {
  const left = c.days_left ?? daysLeft(c.registerEnd || c.register_end)
  return {
    competition_id: c.competitionId || c.competition_id,
    name: c.name,
    short_name: c.shortName || c.short_name || c.name,
    initial: c.initial || c.name?.[0] || '赛',
    level: c.level || 'school',
    organizer: c.organizer || '',
    register_start: c.registerStart || c.register_start,
    register_end: c.registerEnd || c.register_end,
    project_count: c.projectCount ?? c.project_count ?? 0,
    status: c.status || 'upcoming',
    description: c.description || '',
    poster_url: c.posterUrl || c.poster_url || '',
    prize: c.prize || '',
    schedule_note: c.scheduleNote || c.schedule_note || '',
    contact_email: c.contactEmail || c.contact_email || '',
    contact_phone: c.contactPhone || c.contact_phone || '',
    official_links: Array.isArray(c.officialLinks) ? c.officialLinks
                  : Array.isArray(c.official_links) ? c.official_links : [],
    qr_codes: Array.isArray(c.qrCodes) ? c.qrCodes
            : Array.isArray(c.qr_codes) ? c.qr_codes : [],
    days_left: left,
  }
}

export function normalizeNews(n = {}) {
  const t = (n.publishAt || n.publish_at || '').replace('T', ' ').slice(0, 16)
  return {
    news_id: n.newsId || n.news_id,
    competition_id: n.competitionId || n.competition_id || '',
    title: n.title,
    source: n.source || '',
    summary: n.summary || '',
    content: n.content || '',
    link: n.link || '',
    cover_url: n.coverUrl || n.cover_url || '',
    publish_at: t,
    status: n.status || 'published',
    sort_order: n.sortOrder ?? n.sort_order ?? 0,
  }
}

export function normalizeComment(c = {}) {
  const t = (c.createdAt || c.created_at || '').replace('T', ' ').slice(0, 16)
  const name = c.authorName || c.author_name || c.authorId || c.author_id || '同学'
  return {
    id: c.id,
    post_id: c.postId || c.post_id,
    author_id: c.authorId || c.author_id,
    author_name: name,
    author_initial: name?.[0] || '同',
    content: c.content || '',
    like_count: c.likeCount ?? c.like_count ?? 0,
    created_at: t,
  }
}

export function normalizeForumPost(p = {}) {
  const badges = []
  if (p.pinned) badges.push('pinned')
  if (p.essence) badges.push('essence')
  if ((p.likeCount ?? p.like_count ?? 0) > 30) badges.push('hot')
  return {
    post_id: p.postId || p.post_id,
    title: p.title,
    content: p.content || '',
    excerpt: p.excerpt || p.content || '',
    topic: p.topic || '',
    author_id: p.authorId || p.author_id,
    author: p.author || {
      initial: (p.authorName || p.authorId || '同')?.[0],
      name: p.authorName || p.authorId || '同学',
      dept: p.authorDept || '人大',
    },
    pinned: !!p.pinned,
    essence: !!p.essence,
    badges,
    view_count: p.viewCount ?? p.view_count ?? 0,
    reply_count: p.replyCount ?? p.reply_count ?? 0,
    like_count: p.likeCount ?? p.like_count ?? 0,
    status: p.status || 'published',
    last_reply_at: (p.lastReplyAt || p.last_reply_at || p.createdAt || '').replace('T', ' ').slice(0, 16),
  }
}

export function normalizeConversation(c = {}, meId = '') {
  const otherId = c.userAId === meId ? c.userBId : c.userAId
  const name = c.counterpart?.name || otherId || '同学'
  return {
    conversation_id: c.conversationId || c.conversation_id,
    counterpart_id: otherId,
    counterpart: c.counterpart || { initial: name?.[0] || '同', name },
    context_label: c.contextLabel || c.context_label || '站内私信',
    last_message: c.lastMessage || c.last_message || '开始新的对话',
    last_time: (c.lastMessageAt || c.last_message_at || '').replace('T', ' ').slice(11, 16) || '',
    is_expired: c.expired ?? c.is_expired ?? false,
    unread_count: c.unread ?? c.unread_count ?? 0,
  }
}

export function normalizeMessage(m = {}, meId = '') {
  return {
    id: m.id || `m_${Date.now()}`,
    from: (m.senderId || m.sender_id) === meId ? 'me' : 'them',
    text: m.content || '',
    time: (m.createdAt || m.created_at || '').replace('T', ' ').slice(11, 16) || '',
  }
}
