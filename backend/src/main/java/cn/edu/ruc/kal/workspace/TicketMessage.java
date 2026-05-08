package cn.edu.ruc.kal.workspace;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_ticket_message", indexes = {
        @Index(name = "idx_tkm_ticket", columnList = "ticketId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String ticketId;

    @Column(length = 32, nullable = false)
    private String authorId;

    /** 冗余存当时角色，便于前端渲染气泡（student/teacher/staff/admin/super_admin） */
    @Column(length = 16, nullable = false)
    private String authorRole;

    @Column(length = 1000, nullable = false)
    private String content;

    private LocalDateTime createdAt;
}
