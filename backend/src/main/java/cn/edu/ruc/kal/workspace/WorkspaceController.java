package cn.edu.ruc.kal.workspace;

import cn.edu.ruc.kal.common.ApiResponse;
import cn.edu.ruc.kal.common.PageResult;
import cn.edu.ruc.kal.security.CurrentUser;
import cn.edu.ruc.kal.workspace.WorkspaceDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService svc;

    /* ============== Workspace 我端 ============== */

    @GetMapping("/workspaces/mine")
    public ApiResponse<List<WorkspaceListItem>> mine() {
        return ApiResponse.ok(svc.mine(CurrentUser.requireUserId()));
    }

    @PostMapping("/workspaces")
    public ApiResponse<WorkspaceListItem> create(@RequestBody @Valid CreateWorkspaceReq req) {
        return ApiResponse.ok(svc.create(CurrentUser.requireUserId(), req));
    }

    @GetMapping("/workspaces/{id}")
    public ApiResponse<WorkspaceDetail> detail(@PathVariable("id") String id) {
        return ApiResponse.ok(svc.detail(CurrentUser.requireUserId(), id));
    }

    @PatchMapping("/workspaces/{id}")
    public ApiResponse<WorkspaceListItem> update(@PathVariable("id") String id,
                                                 @RequestBody UpdateWorkspaceReq req) {
        return ApiResponse.ok(svc.update(CurrentUser.requireUserId(), id, req));
    }

    @PatchMapping("/workspaces/{id}/progress")
    public ApiResponse<WorkspaceListItem> progress(@PathVariable("id") String id,
                                                   @RequestBody Map<String, Object> body) {
        Integer progress = body.get("progress") == null ? null : ((Number) body.get("progress")).intValue();
        String phase = (String) body.get("phase");
        return ApiResponse.ok(svc.patchProgress(CurrentUser.requireUserId(), id, progress, phase));
    }

    /* ============== Members ============== */

    @PostMapping("/workspaces/{id}/members")
    public ApiResponse<List<MemberView>> addMember(@PathVariable("id") String id,
                                                   @RequestBody @Valid AddMemberReq req) {
        return ApiResponse.ok(svc.addMember(CurrentUser.requireUserId(), id, req));
    }

    @DeleteMapping("/workspaces/{id}/members/{uid}")
    public ApiResponse<List<MemberView>> removeMember(@PathVariable("id") String id,
                                                      @PathVariable("uid") String uid) {
        return ApiResponse.ok(svc.removeMember(CurrentUser.requireUserId(), id, uid));
    }

    /* ============== Milestones ============== */

    @PostMapping("/workspaces/{id}/milestones")
    public ApiResponse<MilestoneView> createMilestone(@PathVariable("id") String id,
                                                      @RequestBody @Valid UpsertMilestoneReq req) {
        return ApiResponse.ok(svc.upsertMilestone(CurrentUser.requireUserId(), id, null, req));
    }

    @PatchMapping("/workspaces/{id}/milestones/{mid}")
    public ApiResponse<MilestoneView> updateMilestone(@PathVariable("id") String id,
                                                      @PathVariable("mid") Long mid,
                                                      @RequestBody @Valid UpsertMilestoneReq req) {
        return ApiResponse.ok(svc.upsertMilestone(CurrentUser.requireUserId(), id, mid, req));
    }

    @DeleteMapping("/workspaces/{id}/milestones/{mid}")
    public ApiResponse<Void> deleteMilestone(@PathVariable("id") String id,
                                             @PathVariable("mid") Long mid) {
        svc.deleteMilestone(CurrentUser.requireUserId(), id, mid);
        return ApiResponse.ok();
    }

    /* ============== Tickets ============== */

    @PostMapping("/workspaces/{id}/tickets")
    public ApiResponse<TicketSummary> openTicket(@PathVariable("id") String id,
                                                 @RequestBody @Valid CreateTicketReq req) {
        return ApiResponse.ok(svc.createTicket(CurrentUser.requireUserId(), id, req));
    }

    @GetMapping("/tickets/{tid}")
    public ApiResponse<TicketDetail> ticketDetail(@PathVariable("tid") String tid) {
        return ApiResponse.ok(svc.ticketDetail(CurrentUser.requireUserId(), tid));
    }

    @PostMapping("/tickets/{tid}/messages")
    public ApiResponse<TicketMessageView> reply(@PathVariable("tid") String tid,
                                                @RequestBody @Valid TicketMessageReq req) {
        return ApiResponse.ok(svc.replyTicket(CurrentUser.requireUserId(), tid, req));
    }

    @PatchMapping("/tickets/{tid}/status")
    public ApiResponse<TicketSummary> ticketStatus(@PathVariable("tid") String tid,
                                                   @RequestBody @Valid TicketStatusReq req) {
        return ApiResponse.ok(svc.changeTicketStatus(CurrentUser.requireUserId(), tid, req.status()));
    }

    @GetMapping("/workspaces/staff")
    public ApiResponse<List<StaffView>> activeStaff() {
        return ApiResponse.ok(svc.listActiveStaff());
    }

    @PatchMapping("/workspaces/{id}/staff")
    public ApiResponse<WorkspaceListItem> assignStaff(@PathVariable("id") String id,
                                                      @RequestBody AssignStaffReq req) {
        return ApiResponse.ok(svc.assignStaff(CurrentUser.requireUserId(), id, req));
    }

    /* ============== Staff 工作台（仅 staff 账号可访问；管理员仍走 /api/v1/admin/**） ============== */

    @GetMapping("/staff/tickets")
    public ApiResponse<PageResult<TicketSummary>> staffTickets(
            @RequestParam(required = false, defaultValue = "all") String scope,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        ensureStaff();
        var p = svc.staffTickets(CurrentUser.requireUserId(), scope, status,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 50)));
        return ApiResponse.ok(PageResult.of(p));
    }

    @GetMapping("/staff/stats")
    public ApiResponse<Map<String, Long>> staffStats() {
        ensureStaff();
        return ApiResponse.ok(svc.staffStats(CurrentUser.requireUserId()));
    }

    private static void ensureStaff() {
        var p = CurrentUser.require();
        if (!"staff".equals(p.getRole())) {
            throw new cn.edu.ruc.kal.common.BizException(403, "无工作人员权限");
        }
    }
}
