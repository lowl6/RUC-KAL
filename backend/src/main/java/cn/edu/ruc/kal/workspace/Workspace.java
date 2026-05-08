package cn.edu.ruc.kal.workspace;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目工作空间：真正落地执行的项目（区别于 kal_project 招募广告卡）。
 * 一个项目一条记录，所有成员共享，由 owner 创建并维护。
 */
@Entity
@Table(name = "kal_workspace", indexes = {
        @Index(name = "idx_ws_owner",  columnList = "ownerId"),
        @Index(name = "idx_ws_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    @Id
    @Column(length = 32)
    private String workspaceId;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 200)
    private String summary;

    /** 关联比赛（短名），如 "中国国际大学生创新大赛"；可空表示"自由项目" */
    @Column(length = 60)
    private String competitionShort;

    /** 项目目标，如 "全国铜奖"，纯文本 */
    @Column(length = 100)
    private String competitionTarget;

    /** 创建者（项目负责人，唯一可加 / 踢成员） */
    @Column(length = 32, nullable = false)
    private String ownerId;

    /** 已分配的工作人员（staff role），可空；未分配时由系统首位响应工单的 staff 自动认领 */
    @Column(length = 32)
    private String assignedStaffId;

    /** 当前阶段 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Phase phase;

    /** 进度百分比 0-100 */
    @Column(nullable = false)
    private Integer progress;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Phase { idea, planning, executing, polishing, submitted }
    public enum Status { active, archived, completed }
}
