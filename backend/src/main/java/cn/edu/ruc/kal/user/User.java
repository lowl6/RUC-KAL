package cn.edu.ruc.kal.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_user", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_role",  columnList = "role")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(length = 32)
    private String userId;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 40)
    private String name;

    @Column(length = 40)
    private String displayName;

    @Column(length = 40)
    private String deptName;

    @Column(length = 16)
    private String grade;

    @Column(length = 200)
    private String avatarUrl;

    @Column(length = 40)
    private String wechatId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private Status status;

    /** 仅在 admin / super_admin 时使用，逗号分隔的权限码 */
    @Column(length = 500)
    private String permsCsv;

    @Column(length = 200)
    private String banReason;

    /** 是否接收邮件通知（新私信等） */
    private Boolean notifyEmail;

    private LocalDateTime banExpireAt;
    private LocalDateTime lastLoginAt;
    @Column(length = 64)
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Role { student, teacher, staff, admin, super_admin }
    public enum Status { active, disabled, banned, pending_first_login }
}
