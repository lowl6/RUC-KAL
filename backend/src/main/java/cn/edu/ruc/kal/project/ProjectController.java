package cn.edu.ruc.kal.project;

import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.BizException;
import cn.edu.ruc.kal.common.PageResult;
import cn.edu.ruc.kal.project.ProjectDtos.*;
import cn.edu.ruc.kal.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository repo;

    @GetMapping("/public/projects")
    public ApiResponse<PageResult<View>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String competition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = repo.search(keyword, competition, Project.Status.recruiting,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 50)));
        return ApiResponse.ok(PageResult.of(p, ProjectController::toView));
    }

    @GetMapping("/public/projects/{id}")
    public ApiResponse<View> get(@PathVariable("id") String id) {
        Project p = repo.findById(id).orElseThrow(() -> new BizException(404, "项目不存在"));
        if (p.getStatus() == Project.Status.deleted) throw new BizException(404, "项目不存在");
        p.setViewCount(safeInc(p.getViewCount()));
        repo.save(p);
        return ApiResponse.ok(toView(p));
    }

    @PostMapping("/projects")
    public ApiResponse<View> create(@RequestBody @Valid CreateReq req) {
        String uid = CurrentUser.requireUserId();
        Project p = Project.builder()
                .projectId("p_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .projectName(req.projectName())
                .oneLiner(req.oneLiner())
                .projectType(parseType(req.projectType()))
                .competitionShort(req.competitionShort())
                .competitionTarget(req.competitionTarget())
                .competitionDeadline(req.competitionDeadline())
                .teamDeadline(req.teamDeadline())
                .currentMembers(req.currentMembers())
                .neededCount(req.neededCount())
                .weeklyHours(req.weeklyHours())
                .detail(req.detail())
                .tagsCsv(req.tags() == null ? null : String.join(",", req.tags()))
                .creatorId(uid)
                .viewCount(0)
                .applyCount(0)
                .status(Project.Status.recruiting)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        if (req.roles() != null) {
            req.roles().forEach(r -> {
                ProjectRole pr = ProjectRole.builder()
                        .project(p)
                        .roleName(r.roleName())
                        .count(r.count() == null ? 1 : r.count())
                        .skills(r.skills())
                        .build();
                p.getRoles().add(pr);
            });
        }
        repo.save(p);
        return ApiResponse.ok(toView(p));
    }

    @GetMapping("/projects/mine")
    public ApiResponse<List<View>> mine() {
        String uid = CurrentUser.requireUserId();
        return ApiResponse.ok(repo.findByCreatorIdOrderByCreatedAtDesc(uid).stream()
                .map(ProjectController::toView).toList());
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") String id) {
        String uid = CurrentUser.requireUserId();
        Project p = repo.findById(id).orElseThrow(() -> new BizException(404, "项目不存在"));
        if (!p.getCreatorId().equals(uid)) throw new BizException(403, "无权操作");
        p.setStatus(Project.Status.deleted);
        p.setUpdatedAt(LocalDateTime.now());
        repo.save(p);
        return ApiResponse.ok();
    }

    private static int safeInc(Integer i) { return i == null ? 1 : i + 1; }

    private static Project.Type parseType(String s) {
        try { return Project.Type.valueOf(s); }
        catch (Exception e) { return Project.Type.innovation; }
    }

    public static View toView(Project p) {
        List<String> tags = p.getTagsCsv() == null || p.getTagsCsv().isBlank()
                ? List.of()
                : Arrays.stream(p.getTagsCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        List<RoleDto> roles = p.getRoles() == null ? List.of() :
                p.getRoles().stream().map(r -> new RoleDto(r.getRoleName(), r.getCount(), r.getSkills())).toList();
        return new View(
                p.getProjectId(), p.getProjectName(), p.getOneLiner(),
                p.getProjectType() == null ? null : p.getProjectType().name(),
                p.getCompetitionShort(), p.getCompetitionTarget(),
                p.getCompetitionDeadline(), p.getTeamDeadline(),
                p.getCurrentMembers(), p.getNeededCount(), p.getWeeklyHours(),
                p.getDetail(), tags, p.getCreatorId(),
                p.getViewCount(), p.getApplyCount(),
                p.getStatus() == null ? null : p.getStatus().name(),
                roles);
    }
}
