package cn.edu.ruc.kal.personalcard;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_personal_card", indexes = {
        @Index(name = "idx_pc_user", columnList = "userId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalCard {

    @Id
    @Column(length = 32)
    private String cardId;

    @Column(nullable = false, length = 32)
    private String userId;

    @Column(length = 40)
    private String displayName;

    @Column(length = 40)
    private String targetRole;

    private Integer weeklyHours;
    private Boolean vacationAvailable;

    /** 逗号分隔技能 */
    @Column(length = 400)
    private String skillsCsv;

    @Column(length = 1500)
    private String selfIntro;

    /** 逗号分隔关心的比赛 */
    @Column(length = 200)
    private String interestedCompetitionsCsv;

    @Column(length = 16)
    private String visibility; // public / private
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status { active, hidden, deleted }
}
