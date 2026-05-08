package cn.edu.ruc.kal.workspace;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支持工单：项目成员向工作人员（staff）发起的求助会话。
 * 一条工单对应一个项目 × 一个会话主题，下挂多条 TicketMessage。
 */
@Entity
@Table(name = "kal_support_ticket", indexes = {
        @Index(name = "idx_tk_ws",       columnList = "workspaceId"),
        @Index(name = "idx_tk_assignee", columnList = "assigneeStaffId"),
        @Index(name = "idx_tk_status",   columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @Column(length = 32)
    private String ticketId;

    @Column(length = 32, nullable = false)
    private String workspaceId;

    /** 发起人（必为该项目的成员之一） */
    @Column(length = 32, nullable = false)
    private String openerId;

    /** 处理工单的工作人员；可空 = 待认领 */
    @Column(length = 32)
    private String assigneeStaffId;

    @Column(length = 80, nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status;

    /** 最近一条消息时间，用于列表排序 */
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;

    /** 给两端显示"是否未读"的简单计数（前端没读时由前端维护，此处只持久化已读时间戳） */
    private LocalDateTime ownerReadAt;
    private LocalDateTime staffReadAt;

    /** 类别：导师推荐 / 经费 / 物资场地 / 其他 */
    public enum Category { advisor, funding, material, other }

    /** 工单状态 */
    public enum Status { open, in_progress, resolved, closed }
}
