package cn.edu.ruc.kal.workspace;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kal_workspace_milestone", indexes = {
        @Index(name = "idx_ms_ws", columnList = "workspaceId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String workspaceId;

    @Column(length = 80, nullable = false)
    private String title;

    @Column(length = 200)
    private String note;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private Status status;

    @Column(nullable = false)
    private Integer sortOrder;

    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public enum Status { pending, doing, done }
}
