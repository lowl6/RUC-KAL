package cn.edu.ruc.kal.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceDtos {

    /* ==================== Requests ==================== */

    public record CreateWorkspaceReq(
            @NotBlank @Size(max = 80)  String title,
            @Size(max = 200)           String summary,
            @Size(max = 60)            String competitionShort,
            @Size(max = 100)           String competitionTarget,
            String                     phase,
            Integer                    progress
    ) {}

    public record UpdateWorkspaceReq(
            @Size(max = 80)  String title,
            @Size(max = 200) String summary,
            @Size(max = 60)  String competitionShort,
            @Size(max = 100) String competitionTarget,
            String           phase,
            Integer          progress,
            String           status
    ) {}

    public record AddMemberReq(
            @NotBlank String email,
            @Size(max = 40) String roleNote
    ) {}

    public record UpsertMilestoneReq(
            @NotBlank @Size(max = 80) String title,
            @Size(max = 200)          String note,
            LocalDate                 dueDate,
            String                    status,
            Integer                   sortOrder
    ) {}

    public record CreateTicketReq(
            @NotBlank @Size(max = 80) String subject,
            @NotBlank                 String category,
            @NotBlank @Size(max = 1000) String firstMessage
    ) {}

    public record TicketMessageReq(
            @NotBlank @Size(max = 1000) String content
    ) {}

    public record AssignTicketReq(
            String staffId
    ) {}

    public record TicketStatusReq(
            @NotBlank String status
    ) {}

    /* ==================== Responses ==================== */

    public record MemberView(
            String userId,
            String displayName,
            String email,
            String deptName,
            String grade,
            String role,
            String roleNote,
            LocalDateTime joinedAt
    ) {}

    public record MilestoneView(
            Long id,
            String title,
            String note,
            LocalDate dueDate,
            String status,
            Integer sortOrder,
            LocalDateTime completedAt
    ) {}

    public record WorkspaceListItem(
            String workspaceId,
            String title,
            String summary,
            String competitionShort,
            String competitionTarget,
            String phase,
            Integer progress,
            String status,
            String ownerId,
            String ownerName,
            String assignedStaffId,
            String assignedStaffName,
            Integer memberCount,
            Integer milestoneTotal,
            Integer milestoneDone,
            Integer openTickets,
            LocalDateTime updatedAt
    ) {}

    public record WorkspaceDetail(
            String workspaceId,
            String title,
            String summary,
            String competitionShort,
            String competitionTarget,
            String phase,
            Integer progress,
            String status,
            String ownerId,
            String ownerName,
            String assignedStaffId,
            String assignedStaffName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<MemberView> members,
            List<MilestoneView> milestones,
            List<TicketSummary> tickets,
            String myRole
    ) {}

    public record TicketSummary(
            String ticketId,
            String workspaceId,
            String workspaceTitle,
            String subject,
            String category,
            String status,
            String openerId,
            String openerName,
            String assigneeStaffId,
            String assigneeStaffName,
            LocalDateTime lastActivityAt,
            LocalDateTime createdAt,
            Integer messageCount
    ) {}

    public record TicketMessageView(
            Long id,
            String authorId,
            String authorName,
            String authorRole,
            String content,
            LocalDateTime createdAt
    ) {}

    public record TicketDetail(
            TicketSummary ticket,
            String workspaceTitle,
            List<TicketMessageView> messages
    ) {}
}
