package cn.edu.ruc.kal.admin;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_audit_log", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actorId"),
        @Index(name = "idx_audit_target", columnList = "targetType,targetId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String actorId;

    @Column(length = 40)
    private String actorName;

    @Column(length = 60, nullable = false)
    private String action;

    @Column(length = 30)
    private String targetType;

    @Column(length = 32)
    private String targetId;

    @Column(length = 500)
    private String detail;

    @Column(length = 64)
    private String ip;

    private LocalDateTime createdAt;
}
