package cn.edu.ruc.kal.admin;

import cn.edu.ruc.kal.auth.AuthService;
import cn.edu.ruc.kal.auth.AuthDtos.UserView;
import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.common.PageResult;
import cn.edu.ruc.kal.common.JsonText;
import cn.edu.ruc.kal.competition.Competition;
import cn.edu.ruc.kal.competition.CompetitionDtos;
import cn.edu.ruc.kal.competition.CompetitionDtos.NewsView;
import cn.edu.ruc.kal.competition.CompetitionDtos.View;
import cn.edu.ruc.kal.competition.CompetitionNews;
import cn.edu.ruc.kal.competition.CompetitionNewsRepository;
import cn.edu.ruc.kal.competition.CompetitionRepository;
import cn.edu.ruc.kal.forum.ForumComment;
import cn.edu.ruc.kal.forum.ForumCommentRepository;
import cn.edu.ruc.kal.forum.ForumPost;
import cn.edu.ruc.kal.forum.ForumPostRepository;
import cn.edu.ruc.kal.news.WechatLinkImporter;
import cn.edu.ruc.kal.project.Project;
import cn.edu.ruc.kal.project.ProjectController;
import cn.edu.ruc.kal.project.ProjectDtos;
import cn.edu.ruc.kal.project.ProjectRepository;
import cn.edu.ruc.kal.security.AuthPrincipal;
import cn.edu.ruc.kal.security.CurrentUser;
import cn.edu.ruc.kal.user.User;
import cn.edu.ruc.kal.user.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminController {

    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;
    private final ForumPostRepository forumRepo;
    private final ForumCommentRepository forumCommentRepo;
    private final CompetitionRepository competitionRepo;
    private final CompetitionNewsRepository newsRepo;
    private final AuditLogRepository auditRepo;
    private final PasswordEncoder encoder;
    private final WechatLinkImporter wechatImporter;

    /* ============== 仪表盘 ============== */

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalUsers",          userRepo.count());
        m.put("activeUsers",         userRepo.countByStatus(User.Status.active));
        m.put("totalProjects",       projectRepo.count());
        m.put("recruitingProjects",  projectRepo.countByStatus(Project.Status.recruiting));
        m.put("totalForumPosts",     forumRepo.count());
        m.put("publishedForumPosts", forumRepo.countByStatus(ForumPost.Status.published));
        m.put("totalCompetitions",   competitionRepo.count());
        m.put("activeCompetitions",  competitionRepo.countByStatus(Competition.Status.active));
        return ApiResponse.ok(m);
    }

    /* ============== 用户管理 ============== */

    @GetMapping("/users")
    public ApiResponse<PageResult<UserView>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireSuperAdmin();
        var p = userRepo.search(keyword, parseRole(role), parseStatus(status),
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return ApiResponse.ok(PageResult.of(p, AuthService::toView));
    }

    /**
     * 管理员手动创建账号（包括「校外邀请」路径）。
     * 与自助注册不同：此处允许任意合法邮箱（非 @ruc.edu.cn 也可），用于嘉宾导师 / 校外评审 / 合作伙伴等。
     */
    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateUserReq req) {
        requireSuperAdmin();
        String email = req.getEmail().toLowerCase().trim();
        if (userRepo.existsByEmail(email)) throw new BizException("该邮箱已存在");
        String initialPwd = (req.getInitialPassword() == null || req.getInitialPassword().isBlank())
                ? generateInitialPassword()
                : req.getInitialPassword();
        User u = User.builder()
                .userId("u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .email(email)
                .passwordHash(encoder.encode(initialPwd))
                .name(req.getName())
                .displayName(req.getName())
                .deptName(req.getDeptName())
                .grade(req.getGrade())
                .role(parseRole(req.getRole()) == null ? User.Role.student : parseRole(req.getRole()))
                .status(User.Status.pending_first_login)
                .notifyEmail(false)
                .permsCsv(req.getPerms() == null ? null : String.join(",", req.getPerms()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepo.save(u);
        boolean external = !email.endsWith("@ruc.edu.cn");
        audit("user_create", "user", u.getUserId(),
                (external ? "（校外邀请）" : "") + "管理员创建账号 " + email);
        Map<String, Object> out = new HashMap<>();
        out.put("user", AuthService.toView(u));
        out.put("initialPassword", initialPwd);
        out.put("external", external);
        return ApiResponse.ok(out);
    }

    private static String generateInitialPassword() {
        // 形如 Kal-A7q9-Pn3K（每次随机，便于一次性发给被邀请人）
        char[] pool = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789".toCharArray();
        var rng = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder("Kal-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            sb.append(pool[rng.nextInt(pool.length)]);
        }
        return sb.toString();
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable("id") String id, @RequestBody UpdateStatusReq req) {
        requireSuperAdmin();
        User u = userRepo.findById(id).orElseThrow(() -> new BizException(404, "用户不存在"));
        u.setStatus(parseStatus(req.getStatus()));
        u.setBanReason(req.getReason());
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);
        audit("user_status", "user", id, "状态改为 " + req.getStatus() + (req.getReason() == null ? "" : "：" + req.getReason()));
        return ApiResponse.ok();
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable("id") String id, @RequestBody UpdateRoleReq req) {
        requireSuperAdmin();
        User u = userRepo.findById(id).orElseThrow(() -> new BizException(404, "用户不存在"));
        u.setRole(parseRole(req.getRole()));
        u.setPermsCsv(req.getPerms() == null ? null : String.join(",", req.getPerms()));
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);
        audit("user_role", "user", id, "角色 -> " + req.getRole());
        return ApiResponse.ok();
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable("id") String id) {
        requireSuperAdmin();
        User u = userRepo.findById(id).orElseThrow(() -> new BizException(404, "用户不存在"));
        String tmp = "kal-" + UUID.randomUUID().toString().substring(0, 6);
        u.setPasswordHash(encoder.encode(tmp));
        userRepo.save(u);
        audit("user_reset_pwd", "user", id, "重置密码");
        return ApiResponse.ok(Map.of("temporaryPassword", tmp));
    }

    /* ============== 项目内容治理 ============== */

    @GetMapping("/projects")
    public ApiResponse<PageResult<ProjectDtos.View>> listProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Project.Status st = null;
        try { if (status != null && !status.isBlank()) st = Project.Status.valueOf(status); } catch (Exception ignore) {}
        var p = projectRepo.search(keyword, competition, st,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return ApiResponse.ok(PageResult.of(p, ProjectController::toView));
    }

    @PatchMapping("/projects/{id}/status")
    public ApiResponse<Void> projectStatus(@PathVariable("id") String id, @RequestBody UpdateStatusReq req) {
        Project p = projectRepo.findById(id).orElseThrow(() -> new BizException(404, "项目不存在"));
        try { p.setStatus(Project.Status.valueOf(req.getStatus())); }
        catch (Exception e) { throw new BizException("非法状态"); }
        p.setUpdatedAt(LocalDateTime.now());
        projectRepo.save(p);
        audit("project_status", "project", id, req.getStatus() + (req.getReason() == null ? "" : "：" + req.getReason()));
        return ApiResponse.ok();
    }

    /* ============== 论坛内容治理 ============== */

    @GetMapping("/forum/posts")
    public ApiResponse<PageResult<ForumPost>> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        ForumPost.Status st = null;
        try { if (status != null && !status.isBlank()) st = ForumPost.Status.valueOf(status); } catch (Exception ignore) {}
        var p = forumRepo.search(keyword, topic, st,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return ApiResponse.ok(PageResult.of(p));
    }

    @PatchMapping("/forum/posts/{id}/status")
    public ApiResponse<Void> postStatus(@PathVariable("id") String id, @RequestBody UpdateStatusReq req) {
        ForumPost p = forumRepo.findById(id).orElseThrow(() -> new BizException(404, "帖子不存在"));
        try { p.setStatus(ForumPost.Status.valueOf(req.getStatus())); }
        catch (Exception e) { throw new BizException("非法状态"); }
        forumRepo.save(p);
        audit("forum_status", "forum_post", id, req.getStatus() + (req.getReason() == null ? "" : "：" + req.getReason()));
        return ApiResponse.ok();
    }

    @PatchMapping("/forum/posts/{id}/pin")
    public ApiResponse<Void> pinPost(@PathVariable("id") String id, @RequestParam("pinned") boolean pinned) {
        ForumPost p = forumRepo.findById(id).orElseThrow(() -> new BizException(404, "帖子不存在"));
        p.setPinned(pinned); forumRepo.save(p);
        audit("forum_pin", "forum_post", id, pinned ? "置顶" : "取消置顶");
        return ApiResponse.ok();
    }

    /* ============== 比赛管理 ============== */

    @GetMapping("/competitions")
    public ApiResponse<List<View>> listCompetitions() {
        return ApiResponse.ok(competitionRepo.findAll().stream().map(CompetitionDtos::toView).toList());
    }

    @GetMapping("/competitions/{id}")
    public ApiResponse<View> getCompetition(@PathVariable("id") String id) {
        var c = competitionRepo.findById(id).orElseThrow(() -> new BizException(404, "比赛不存在"));
        return ApiResponse.ok(CompetitionDtos.toView(c));
    }

    @PostMapping("/competitions")
    public ApiResponse<View> upsertCompetition(@RequestBody CompetitionUpsertReq req) {
        Competition c;
        if (req.getCompetitionId() == null || req.getCompetitionId().isBlank()) {
            c = new Competition();
            c.setCompetitionId("cp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            c.setProjectCount(0);
        } else {
            c = competitionRepo.findById(req.getCompetitionId()).orElseGet(() -> {
                Competition n = new Competition();
                n.setCompetitionId(req.getCompetitionId());
                n.setProjectCount(0);
                return n;
            });
        }
        c.setName(req.getName());
        c.setShortName(req.getShortName());
        c.setInitial(req.getInitial());
        c.setOrganizer(req.getOrganizer());
        c.setRegisterStart(req.getRegisterStart() == null ? LocalDate.now() : req.getRegisterStart());
        c.setRegisterEnd(req.getRegisterEnd() == null ? LocalDate.now().plusMonths(1) : req.getRegisterEnd());
        c.setDescription(req.getDescription());
        c.setPosterUrl(req.getPosterUrl());
        c.setPrize(req.getPrize());
        c.setScheduleNote(req.getScheduleNote());
        c.setContactEmail(req.getContactEmail());
        c.setContactPhone(req.getContactPhone());
        c.setOfficialLinksJson(JsonText.stringify(req.getOfficialLinks()));
        c.setQrCodesJson(JsonText.stringify(req.getQrCodes()));
        try { c.setLevel(Competition.Level.valueOf(req.getLevel())); } catch (Exception e) { c.setLevel(Competition.Level.school); }
        // 状态不再依赖 req.status：根据「报名起止日期」自动派生，并写入数据库以保证统计口径一致
        String autoStatus = CompetitionDtos.computeStatus(c.getRegisterStart(), c.getRegisterEnd(), c.getStatus());
        try { c.setStatus(Competition.Status.valueOf(autoStatus)); } catch (Exception e) { c.setStatus(Competition.Status.upcoming); }
        competitionRepo.save(c);
        audit("competition_upsert", "competition", c.getCompetitionId(), c.getName());
        return ApiResponse.ok(CompetitionDtos.toView(c));
    }

    @DeleteMapping("/competitions/{id}")
    public ApiResponse<Void> deleteCompetition(@PathVariable("id") String id) {
        newsRepo.findByCompetitionIdOrderByPublishAtDesc(id).forEach(newsRepo::delete);
        competitionRepo.deleteById(id);
        audit("competition_delete", "competition", id, "删除比赛");
        return ApiResponse.ok();
    }

    /* ============== 资讯（News） ============== */

    @GetMapping("/news")
    public ApiResponse<PageResult<NewsView>> listNews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = newsRepo.search(keyword, status,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return ApiResponse.ok(PageResult.of(p, CompetitionDtos::toView));
    }

    @PostMapping("/news")
    public ApiResponse<NewsView> upsertNews(@RequestBody NewsUpsertReq req) {
        CompetitionNews n;
        if (req.getNewsId() == null || req.getNewsId().isBlank()) {
            n = new CompetitionNews();
            n.setNewsId("nw_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        } else {
            n = newsRepo.findById(req.getNewsId()).orElseGet(() -> {
                CompetitionNews x = new CompetitionNews();
                x.setNewsId(req.getNewsId());
                return x;
            });
        }
        n.setTitle(req.getTitle());
        n.setSource(req.getSource());
        n.setSummary(req.getSummary());
        n.setContent(req.getContent());
        n.setLink(req.getLink());
        n.setCoverUrl(req.getCoverUrl());
        n.setCompetitionId(req.getCompetitionId());
        n.setStatus(req.getStatus() == null || req.getStatus().isBlank() ? "published" : req.getStatus());
        n.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        n.setPublishAt(req.getPublishAt() == null ? LocalDateTime.now() : req.getPublishAt());
        newsRepo.save(n);
        audit("news_upsert", "news", n.getNewsId(), n.getTitle());
        return ApiResponse.ok(CompetitionDtos.toView(n));
    }

    @DeleteMapping("/news/{id}")
    public ApiResponse<Void> deleteNews(@PathVariable("id") String id) {
        newsRepo.deleteById(id);
        audit("news_delete", "news", id, "删除资讯");
        return ApiResponse.ok();
    }

    /**
     * 批量「公众号链接 → 资讯」导入。
     * <p>
     * 对每个 URL，后端 fetch 文章页解析 og:title / nickname / og:image / 发布时间。
     * 如果库里已有相同 link 的资讯，则更新标题/封面（保留管理员手动编辑过的 summary/content）；
     * 否则插入新条目。
     */
    @PostMapping("/news/import-links")
    public ApiResponse<Map<String, Object>> importNewsLinks(@RequestBody NewsImportReq req) {
        if (req == null || req.getUrls() == null || req.getUrls().isEmpty()) {
            throw new BizException(400, "请至少粘贴一条公众号链接");
        }
        int created = 0, updated = 0;
        List<Map<String, String>> failures = new java.util.ArrayList<>();
        List<NewsView> imported = new java.util.ArrayList<>();

        for (String raw : req.getUrls()) {
            if (raw == null) continue;
            String url = raw.trim();
            if (url.isEmpty()) continue;
            // 容错：如果一行里粘进了多余前缀（如 "标题：xxx https://..."）
            int idx = url.indexOf("https://mp.weixin.qq.com/");
            if (idx > 0) url = url.substring(idx);
            // 截断 query 之外的尾巴（# 锚点）
            int hash = url.indexOf('#');
            if (hash >= 0) url = url.substring(0, hash);

            WechatLinkImporter.Result r = wechatImporter.fetch(url);
            if (!r.ok()) {
                failures.add(Map.of("url", url, "reason", r.error() == null ? "未知错误" : r.error()));
                continue;
            }

            // 以 link 为 idempotency key
            final String key = url;
            CompetitionNews existing = newsRepo.findAll().stream()
                    .filter(n -> key.equals(n.getLink()))
                    .findFirst().orElse(null);

            CompetitionNews n = existing == null ? new CompetitionNews() : existing;
            boolean isNew = existing == null;
            if (isNew) {
                n.setNewsId("nw_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
                n.setStatus("published");
                n.setSortOrder(0);
                n.setSummary(r.summary());
                n.setContent("本资讯转载自公众号原文，点击「查看原文」前往微信阅读全文。");
            } else {
                // 更新模式：仅在原内容仍是占位时覆盖 summary/content
                if (n.getSummary() == null || n.getSummary().isBlank()) n.setSummary(r.summary());
            }

            n.setTitle(r.title());
            n.setSource(r.source() == null || r.source().isBlank() ? "微信公众号" : r.source());
            n.setLink(key);
            if (r.coverUrl() != null && !r.coverUrl().isBlank()) n.setCoverUrl(r.coverUrl());
            if (r.publishAt() != null) n.setPublishAt(r.publishAt());
            else if (n.getPublishAt() == null) n.setPublishAt(LocalDateTime.now());

            if (req.getCompetitionId() != null && !req.getCompetitionId().isBlank()) {
                n.setCompetitionId(req.getCompetitionId());
            }
            newsRepo.save(n);
            imported.add(CompetitionDtos.toView(n));
            if (isNew) created++; else updated++;
            audit(isNew ? "news_import_create" : "news_import_update",
                    "news", n.getNewsId(), n.getTitle());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("created", created);
        body.put("updated", updated);
        body.put("failed",  failures);
        body.put("items",   imported);
        return ApiResponse.ok(body);
    }

    /* ============== 评论治理 ============== */

    @GetMapping("/forum/posts/{id}/comments")
    public ApiResponse<List<ForumComment>> listComments(@PathVariable("id") String id) {
        return ApiResponse.ok(forumCommentRepo.findByPostIdAndStatusOrderByCreatedAtAsc(id, "published"));
    }

    @DeleteMapping("/forum/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable("id") Long id) {
        var c = forumCommentRepo.findById(id).orElseThrow(() -> new BizException(404, "评论不存在"));
        c.setStatus("deleted");
        forumCommentRepo.save(c);
        audit("forum_comment_delete", "forum_comment", String.valueOf(id), "删除评论");
        return ApiResponse.ok();
    }

    /* ============== 审计日志 ============== */

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditLog>> auditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        var p = auditRepo.search(actor, action, PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return ApiResponse.ok(PageResult.of(p));
    }

    /* ============== 工具 ============== */

    private void audit(String action, String type, String id, String detail) {
        AuthPrincipal me = CurrentUser.getOrNull();
        String actorId = me == null ? "system" : me.getUserId();
        String actorName = me == null ? "system"
                : userRepo.findById(actorId).map(u -> u.getDisplayName()).orElse(actorId);
        auditRepo.save(AuditLog.builder()
                .actorId(actorId).actorName(actorName)
                .action(action).targetType(type).targetId(id).detail(detail)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private static void requireSuperAdmin() {
        if (!"super_admin".equals(CurrentUser.require().getRole())) {
            throw new BizException(403, "仅超级管理员可进行账号管理");
        }
    }

    private static User.Role parseRole(String s) {
        if (s == null || s.isBlank()) return null;
        try { return User.Role.valueOf(s); } catch (Exception e) { return null; }
    }

    private static User.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return User.Status.valueOf(s); } catch (Exception e) { return null; }
    }

    @Data public static class CreateUserReq {
        @NotBlank @Email private String email;
        @NotBlank private String name;
        private String deptName;
        private String grade;
        private String role;
        private List<String> perms;
        private String initialPassword;
    }

    @Data public static class UpdateStatusReq {
        private String status;
        private String reason;
    }

    @Data public static class UpdateRoleReq {
        private String role;
        private List<String> perms;
    }

    @Data public static class CompetitionUpsertReq {
        private String competitionId;
        private String name;
        private String shortName;
        private String initial;
        private String level;
        private String status;
        private String organizer;
        private LocalDate registerStart;
        private LocalDate registerEnd;
        private String description;
        private String posterUrl;
        private String prize;
        private String scheduleNote;
        private String contactEmail;
        private String contactPhone;
        private List<Map<String, String>> officialLinks;
        private List<Map<String, String>> qrCodes;
    }

    @Data public static class NewsUpsertReq {
        private String newsId;
        private String competitionId;
        private String title;
        private String source;
        private String summary;
        private String content;
        private String link;
        private String coverUrl;
        private String status;
        private Integer sortOrder;
        private LocalDateTime publishAt;
    }

    @Data public static class NewsImportReq {
        /** 公众号文章 URL 列表，每行一个，前后可带空白与中文标点。 */
        private List<String> urls;
        /** 可选：把这一批资讯一并挂到某个比赛下。 */
        private String competitionId;
    }
}
