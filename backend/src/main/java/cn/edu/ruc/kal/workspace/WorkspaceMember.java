package cn.edu.ruc.kal.workspace;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目成员关系（多对多 user × workspace）。
 * 一个用户在同一项目里只会出现一次（联合唯一约束）。
 */
@Entity
@Table(name = "kal_workspace_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_ws_user", columnNames = {"workspaceId", "userId"}),
        indexes = {
                @Index(name = "idx_wsm_user", columnList = "userId"),
                @Index(name = "idx_wsm_ws",   columnList = "workspaceId")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String workspaceId;

    @Column(length = 32, nullable = false)
    private String userId;

    /** owner: 创建者 / 项目负责人；member: 普通成员 */
    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private Role role;

    /** 在项目中的角色描述（"前端"/"商业策划"），纯文本，可空 */
    @Column(length = 40)
    private String roleNote;

    private LocalDateTime joinedAt;

    public enum Role { owner, member }
}
