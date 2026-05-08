package cn.edu.ruc.kal.workspace;

import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import cn.edu.ruc.kal.workspace.WorkspaceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository wsRepo;
    private final WorkspaceMemberRepository memberRepo;
    private final WorkspaceMilestoneRepository milestoneRepo;
    private final SupportTicketRepository ticketRepo;
    private final TicketMessageRepository msgRepo;
    private final UserRepository userRepo;

    /* ====================== Workspace ====================== */

    @Transactional
    public WorkspaceListItem create(String userId, CreateWorkspaceReq req) {
        User assignedStaff = resolveStaff(req.assignedStaffId(), false);
        Workspace w = Workspace.builder()
                .workspaceId("ws_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .title(req.title().trim())
                .summary(safe(req.summary()))
                .competitionShort(safe(req.competitionShort()))
                .competitionTarget(safe(req.competitionTarget()))
                .ownerId(userId)
                .assignedStaffId(assignedStaff == null ? null : assignedStaff.getUserId())
                .phase(parsePhase(req.phase()))
                .progress(clampProgress(req.progress()))
                .status(Workspace.Status.active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        wsRepo.save(w);
        memberRepo.save(WorkspaceMember.builder()
                .workspaceId(w.getWorkspaceId())
                .userId(userId)
                .role(WorkspaceMember.Role.owner)
                .roleNote("项目负责人")
                .joinedAt(LocalDateTime.now())
                .build());
        return toListItem(w);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceListItem> mine(String userId) {
        List<WorkspaceMember> mems = memberRepo.findByUserId(userId);
        if (mems.isEmpty()) return List.of();
        Set<String> ids = mems.stream().map(WorkspaceMember::getWorkspaceId).collect(Collectors.toSet());
        return wsRepo.findAllById(ids).stream()
                .sorted(Comparator.comparing(Workspace::getUpdatedAt).reversed())
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public WorkspaceDetail detail(String userId, String workspaceId) {
        Workspace w = wsRepo.findById(workspaceId).orElseThrow(() -> new BizException(404, "项目不存在"));
        WorkspaceMember mine = memberRepo.findByWorkspaceIdAndUserId(workspaceId, userId).orElse(null);
        if (mine == null) throw new BizException(403, "你不是该项目成员");
        return assembleDetail(w, mine);
    }

    @Transactional
    public WorkspaceListItem update(String userId, String workspaceId, UpdateWorkspaceReq req) {
        Workspace w = mustOwner(userId, workspaceId);
        User assignedStaff = req.assignedStaffId() == null ? null : resolveStaff(req.assignedStaffId(), true);
        if (req.title() != null && !req.title().isBlank()) w.setTitle(req.title().trim());
        if (req.summary() != null) w.setSummary(req.summary());
        if (req.competitionShort() != null) w.setCompetitionShort(req.competitionShort());
        if (req.competitionTarget() != null) w.setCompetitionTarget(req.competitionTarget());
        if (req.assignedStaffId() != null) w.setAssignedStaffId(assignedStaff == null ? null : assignedStaff.getUserId());
        if (req.phase() != null) w.setPhase(parsePhase(req.phase()));
        if (req.progress() != null) w.setProgress(clampProgress(req.progress()));
        if (req.status() != null) {
            try { w.setStatus(Workspace.Status.valueOf(req.status())); }
            catch (Exception ignore) {}
        }
        w.setUpdatedAt(LocalDateTime.now());
        wsRepo.save(w);
        return toListItem(w);
    }

    @Transactional
    public WorkspaceListItem assignStaff(String userId, String workspaceId, AssignStaffReq req) {
        Workspace w = mustOwner(userId, workspaceId);
        User staff = resolveStaff(req.staffId(), true);
        w.setAssignedStaffId(staff == null ? null : staff.getUserId());
        w.setUpdatedAt(LocalDateTime.now());
        wsRepo.save(w);
        return toListItem(w);
    }

    @Transactional(readOnly = true)
    public List<StaffView> listActiveStaff() {
        return userRepo.findByRoleAndStatusOrderByDisplayNameAsc(User.Role.staff, User.Status.active)
                .stream()
                .map(u -> new StaffView(
                        u.getUserId(),
                        u.getDisplayName(),
                        u.getEmail(),
                        safe(u.getDeptName()),
                        safe(u.getGrade())))
                .toList();
    }

    /** 项目成员（不止 owner）也可以推动进度，但不能改成员关系。 */
    @Transactional
    public WorkspaceListItem patchProgress(String userId, String workspaceId, Integer progress, String phase) {
        Workspace w = mustMember(userId, workspaceId);
        if (progress != null) w.setProgress(clampProgress(progress));
        if (phase != null && !phase.isBlank()) w.setPhase(parsePhase(phase));
        w.setUpdatedAt(LocalDateTime.now());
        wsRepo.save(w);
        return toListItem(w);
    }

    /* ====================== Members ====================== */

    @Transactional
    public List<MemberView> addMember(String userId, String workspaceId, AddMemberReq req) {
        mustOwner(userId, workspaceId);
        String email = req.email().toLowerCase().trim();
        User u = userRepo.findByEmail(email).orElseThrow(() ->
                new BizException(404, "未找到邮箱为 " + email + " 的同学；请先让 ta 在平台注册账号。"));
        if (memberRepo.existsByWorkspaceIdAndUserId(workspaceId, u.getUserId())) {
            throw new BizException("该同学已在项目中");
        }
        memberRepo.save(WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(u.getUserId())
                .role(WorkspaceMember.Role.member)
                .roleNote(safe(req.roleNote()))
                .joinedAt(LocalDateTime.now())
                .build());
        bump(workspaceId);
        return listMembers(workspaceId);
    }

    @Transactional
    public List<MemberView> removeMember(String userId, String workspaceId, String memberUserId) {
        Workspace w = mustOwner(userId, workspaceId);
        if (Objects.equals(memberUserId, w.getOwnerId())) {
            throw new BizException("无法移除项目负责人");
        }
        memberRepo.deleteByWorkspaceIdAndUserId(workspaceId, memberUserId);
        bump(workspaceId);
        return listMembers(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<MemberView> listMembers(String workspaceId) {
        List<WorkspaceMember> ms = memberRepo.findByWorkspaceIdOrderByJoinedAtAsc(workspaceId);
        Map<String, User> users = loadUsers(ms.stream().map(WorkspaceMember::getUserId).toList());
        return ms.stream().map(m -> {
            User u = users.get(m.getUserId());
            return new MemberView(
                    m.getUserId(),
                    u == null ? "已移除用户" : u.getDisplayName(),
                    u == null ? "" : u.getEmail(),
                    u == null ? "" : safe(u.getDeptName()),
                    u == null ? "" : safe(u.getGrade()),
                    m.getRole().name(),
                    safe(m.getRoleNote()),
                    m.getJoinedAt()
            );
        }).toList();
    }

    /* ====================== Milestones ====================== */

    @Transactional
    public MilestoneView upsertMilestone(String userId, String workspaceId, Long id, UpsertMilestoneReq req) {
        mustMember(userId, workspaceId);
        WorkspaceMilestone m;
        if (id == null) {
            m = WorkspaceMilestone.builder()
                    .workspaceId(workspaceId)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            m = milestoneRepo.findById(id).orElseThrow(() -> new BizException(404, "里程碑不存在"));
            if (!Objects.equals(m.getWorkspaceId(), workspaceId)) throw new BizException(403, "里程碑不属于该项目");
        }
        m.setTitle(req.title().trim());
        m.setNote(safe(req.note()));
        m.setDueDate(req.dueDate());
        WorkspaceMilestone.Status st = parseMilestoneStatus(req.status());
        if (st == WorkspaceMilestone.Status.done && m.getStatus() != WorkspaceMilestone.Status.done) {
            m.setCompletedAt(LocalDateTime.now());
        } else if (st != WorkspaceMilestone.Status.done) {
            m.setCompletedAt(null);
        }
        m.setStatus(st);
        m.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        milestoneRepo.save(m);
        bump(workspaceId);
        return toMilestoneView(m);
    }

    @Transactional
    public void deleteMilestone(String userId, String workspaceId, Long id) {
        mustMember(userId, workspaceId);
        WorkspaceMilestone m = milestoneRepo.findById(id).orElseThrow(() -> new BizException(404, "里程碑不存在"));
        if (!Objects.equals(m.getWorkspaceId(), workspaceId)) throw new BizException(403, "里程碑不属于该项目");
        milestoneRepo.delete(m);
        bump(workspaceId);
    }

    /* ====================== Tickets ====================== */

    @Transactional
    public TicketSummary createTicket(String userId, String workspaceId, CreateTicketReq req) {
        Workspace w = mustMember(userId, workspaceId);
        User opener = userRepo.findById(userId).orElseThrow(() -> new BizException(401, "未登录"));

        SupportTicket t = SupportTicket.builder()
                .ticketId("tk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .workspaceId(workspaceId)
                .openerId(userId)
                .assigneeStaffId(w.getAssignedStaffId())
                .subject(req.subject().trim())
                .category(parseCategory(req.category()))
                .status(w.getAssignedStaffId() == null ? SupportTicket.Status.open : SupportTicket.Status.in_progress)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .ownerReadAt(LocalDateTime.now())
                .build();
        ticketRepo.save(t);

        msgRepo.save(TicketMessage.builder()
                .ticketId(t.getTicketId())
                .authorId(userId)
                .authorRole(opener.getRole().name())
                .content(req.firstMessage().trim())
                .createdAt(LocalDateTime.now())
                .build());

        return toTicketSummary(t, false);
    }

    @Transactional
    public TicketDetail ticketDetail(String userId, String ticketId) {
        SupportTicket t = ticketRepo.findById(ticketId).orElseThrow(() -> new BizException(404, "工单不存在"));
        boolean isStaff = isStaff(userId);
        if (!isStaff) {
            mustMember(userId, t.getWorkspaceId()); // 项目成员才能看
            t.setOwnerReadAt(LocalDateTime.now());
        } else {
            t.setStaffReadAt(LocalDateTime.now());
            // 自动认领：第一个查看的 staff 即为处理人（除非已分配）
            if (t.getAssigneeStaffId() == null) {
                t.setAssigneeStaffId(userId);
                if (t.getStatus() == SupportTicket.Status.open) {
                    t.setStatus(SupportTicket.Status.in_progress);
                }
            }
        }
        ticketRepo.save(t);

        Workspace w = wsRepo.findById(t.getWorkspaceId()).orElse(null);
        List<TicketMessage> msgs = msgRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
        Set<String> aids = new HashSet<>();
        msgs.forEach(m -> aids.add(m.getAuthorId()));
        Map<String, User> users = loadUsers(aids);
        List<TicketMessageView> views = msgs.stream().map(m -> {
            User u = users.get(m.getAuthorId());
            return new TicketMessageView(
                    m.getId(),
                    m.getAuthorId(),
                    u == null ? "未知用户" : u.getDisplayName(),
                    m.getAuthorRole(),
                    m.getContent(),
                    m.getCreatedAt()
            );
        }).toList();
        return new TicketDetail(toTicketSummary(t, false),
                w == null ? "" : w.getTitle(),
                views);
    }

    @Transactional
    public TicketMessageView replyTicket(String userId, String ticketId, TicketMessageReq req) {
        SupportTicket t = ticketRepo.findById(ticketId).orElseThrow(() -> new BizException(404, "工单不存在"));
        User author = userRepo.findById(userId).orElseThrow(() -> new BizException(401, "未登录"));
        boolean staff = isStaffRole(author.getRole());
        if (!staff) {
            mustMember(userId, t.getWorkspaceId());
        } else {
            // staff 回复时如未认领则自动认领
            if (t.getAssigneeStaffId() == null) t.setAssigneeStaffId(userId);
            if (t.getStatus() == SupportTicket.Status.open) {
                t.setStatus(SupportTicket.Status.in_progress);
            }
        }
        if (t.getStatus() == SupportTicket.Status.closed) {
            throw new BizException("该工单已关闭");
        }

        TicketMessage m = TicketMessage.builder()
                .ticketId(ticketId)
                .authorId(userId)
                .authorRole(author.getRole().name())
                .content(req.content().trim())
                .createdAt(LocalDateTime.now())
                .build();
        msgRepo.save(m);
        t.setLastActivityAt(LocalDateTime.now());
        if (staff) t.setStaffReadAt(LocalDateTime.now()); else t.setOwnerReadAt(LocalDateTime.now());
        ticketRepo.save(t);
        return new TicketMessageView(m.getId(), userId, author.getDisplayName(), author.getRole().name(),
                m.getContent(), m.getCreatedAt());
    }

    @Transactional
    public TicketSummary changeTicketStatus(String userId, String ticketId, String status) {
        SupportTicket t = ticketRepo.findById(ticketId).orElseThrow(() -> new BizException(404, "工单不存在"));
        boolean staff = isStaff(userId);
        if (!staff) {
            // 普通成员只能 close（关闭）"自己提的"工单或处于 resolved 的工单
            if (!Objects.equals(t.getOpenerId(), userId)) throw new BizException(403, "无权操作");
            if (!"closed".equals(status)) throw new BizException("仅工作人员可改为该状态");
        }
        try { t.setStatus(SupportTicket.Status.valueOf(status)); }
        catch (Exception e) { throw new BizException("非法状态"); }
        t.setLastActivityAt(LocalDateTime.now());
        ticketRepo.save(t);
        return toTicketSummary(t, false);
    }

    /** Staff 视角的工单列表 */
    @Transactional(readOnly = true)
    public Page<TicketSummary> staffTickets(String staffUserId, String scope, String status, Pageable pageable) {
        SupportTicket.Status st = null;
        if (status != null && !status.isBlank()) {
            try { st = SupportTicket.Status.valueOf(status); } catch (Exception ignore) {}
        }
        String assignee = null;
        if ("mine".equalsIgnoreCase(scope)) assignee = staffUserId;
        return ticketRepo.searchForStaff(assignee, st, pageable).map(t -> toTicketSummary(t, true));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> staffStats(String staffUserId) {
        Map<String, Long> m = new HashMap<>();
        m.put("open",        ticketRepo.countByStatus(SupportTicket.Status.open));
        m.put("in_progress", ticketRepo.countByStatus(SupportTicket.Status.in_progress));
        m.put("resolved",    ticketRepo.countByStatus(SupportTicket.Status.resolved));
        m.put("closed",      ticketRepo.countByStatus(SupportTicket.Status.closed));
        return m;
    }

    /* ====================== Helpers ====================== */

    private WorkspaceListItem toListItem(Workspace w) {
        long memberCount = memberRepo.findByWorkspaceIdOrderByJoinedAtAsc(w.getWorkspaceId()).size();
        long total = milestoneRepo.countByWorkspaceId(w.getWorkspaceId());
        long done = milestoneRepo.countByWorkspaceIdAndStatus(w.getWorkspaceId(), WorkspaceMilestone.Status.done);
        long open = ticketRepo.countByWorkspaceIdAndStatusIn(w.getWorkspaceId(),
                List.of(SupportTicket.Status.open, SupportTicket.Status.in_progress));
        User owner = userRepo.findById(w.getOwnerId()).orElse(null);
        User staff = w.getAssignedStaffId() == null ? null : userRepo.findById(w.getAssignedStaffId()).orElse(null);
        return new WorkspaceListItem(
                w.getWorkspaceId(), w.getTitle(), w.getSummary(),
                w.getCompetitionShort(), w.getCompetitionTarget(),
                w.getPhase().name(), w.getProgress(), w.getStatus().name(),
                w.getOwnerId(), owner == null ? "" : owner.getDisplayName(),
                w.getAssignedStaffId(), staff == null ? "" : staff.getDisplayName(),
                (int) memberCount, (int) total, (int) done, (int) open,
                w.getUpdatedAt()
        );
    }

    private WorkspaceDetail assembleDetail(Workspace w, WorkspaceMember mine) {
        List<MemberView> members = listMembers(w.getWorkspaceId());
        List<MilestoneView> milestones = milestoneRepo.findByWorkspaceIdOrderBySortOrderAscIdAsc(w.getWorkspaceId())
                .stream().map(this::toMilestoneView).toList();
        List<SupportTicket> tickets = ticketRepo.findByWorkspaceIdOrderByLastActivityAtDesc(w.getWorkspaceId());
        List<TicketSummary> ticketSummaries = tickets.stream().map(t -> toTicketSummary(t, false)).toList();

        User owner = userRepo.findById(w.getOwnerId()).orElse(null);
        User staff = w.getAssignedStaffId() == null ? null : userRepo.findById(w.getAssignedStaffId()).orElse(null);

        return new WorkspaceDetail(
                w.getWorkspaceId(), w.getTitle(), w.getSummary(),
                w.getCompetitionShort(), w.getCompetitionTarget(),
                w.getPhase().name(), w.getProgress(), w.getStatus().name(),
                w.getOwnerId(), owner == null ? "" : owner.getDisplayName(),
                w.getAssignedStaffId(), staff == null ? "" : staff.getDisplayName(),
                w.getCreatedAt(), w.getUpdatedAt(),
                members, milestones, ticketSummaries,
                mine.getRole().name()
        );
    }

    private TicketSummary toTicketSummary(SupportTicket t, boolean includeWsTitle) {
        Workspace w = includeWsTitle ? wsRepo.findById(t.getWorkspaceId()).orElse(null) : null;
        User opener = userRepo.findById(t.getOpenerId()).orElse(null);
        User staff = t.getAssigneeStaffId() == null ? null : userRepo.findById(t.getAssigneeStaffId()).orElse(null);
        long count = msgRepo.countByTicketId(t.getTicketId());
        return new TicketSummary(
                t.getTicketId(), t.getWorkspaceId(),
                w == null ? null : w.getTitle(),
                t.getSubject(), t.getCategory().name(), t.getStatus().name(),
                t.getOpenerId(), opener == null ? "" : opener.getDisplayName(),
                t.getAssigneeStaffId(), staff == null ? "" : staff.getDisplayName(),
                t.getLastActivityAt(), t.getCreatedAt(),
                (int) count
        );
    }

    private MilestoneView toMilestoneView(WorkspaceMilestone m) {
        return new MilestoneView(m.getId(), m.getTitle(), m.getNote(),
                m.getDueDate(), m.getStatus().name(),
                m.getSortOrder(), m.getCompletedAt());
    }

    private Workspace mustOwner(String userId, String workspaceId) {
        Workspace w = wsRepo.findById(workspaceId).orElseThrow(() -> new BizException(404, "项目不存在"));
        if (!Objects.equals(w.getOwnerId(), userId)) {
            throw new BizException(403, "仅项目负责人可执行该操作");
        }
        return w;
    }

    private Workspace mustMember(String userId, String workspaceId) {
        Workspace w = wsRepo.findById(workspaceId).orElseThrow(() -> new BizException(404, "项目不存在"));
        if (!memberRepo.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new BizException(403, "你不是该项目成员");
        }
        return w;
    }

    private boolean isStaff(String userId) {
        return userRepo.findById(userId).map(u -> isStaffRole(u.getRole())).orElse(false);
    }

    public static boolean isStaffRole(User.Role role) {
        return role == User.Role.staff;
    }

    private User resolveStaff(String staffId, boolean allowBlank) {
        if (staffId == null || staffId.isBlank()) {
            if (allowBlank) return null;
            return null;
        }
        User staff = userRepo.findById(staffId).orElseThrow(() -> new BizException(404, "工作人员不存在"));
        if (staff.getRole() != User.Role.staff || staff.getStatus() != User.Status.active) {
            throw new BizException("只能选择有效的工作人员账号");
        }
        return staff;
    }

    private void bump(String workspaceId) {
        wsRepo.findById(workspaceId).ifPresent(w -> {
            w.setUpdatedAt(LocalDateTime.now());
            wsRepo.save(w);
        });
    }

    private Map<String, User> loadUsers(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return userRepo.findAllById(ids).stream().collect(Collectors.toMap(User::getUserId, u -> u));
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static int clampProgress(Integer v) {
        if (v == null) return 0;
        return Math.max(0, Math.min(100, v));
    }

    private static Workspace.Phase parsePhase(String s) {
        if (s == null || s.isBlank()) return Workspace.Phase.idea;
        try { return Workspace.Phase.valueOf(s); } catch (Exception e) { return Workspace.Phase.idea; }
    }

    private static WorkspaceMilestone.Status parseMilestoneStatus(String s) {
        if (s == null || s.isBlank()) return WorkspaceMilestone.Status.pending;
        try { return WorkspaceMilestone.Status.valueOf(s); } catch (Exception e) { return WorkspaceMilestone.Status.pending; }
    }

    private static SupportTicket.Category parseCategory(String s) {
        if (s == null || s.isBlank()) return SupportTicket.Category.other;
        try { return SupportTicket.Category.valueOf(s); } catch (Exception e) { return SupportTicket.Category.other; }
    }
}
