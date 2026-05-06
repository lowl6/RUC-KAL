package cn.edu.ruc.kal.competition;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "kal_competition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Competition {

    @Id
    @Column(length = 32)
    private String competitionId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String shortName;

    @Column(length = 4)
    private String initial;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Level level;

    @Column(length = 100)
    private String organizer;

    private LocalDate registerStart;
    private LocalDate registerEnd;
    private Integer projectCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Status status;

    @Column(length = 1000)
    private String description;

    @Column(length = 200)
    private String posterUrl;

    @Column(length = 400)
    private String prize;

    @Column(length = 1000)
    private String scheduleNote;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 60)
    private String contactPhone;

    /** JSON 字符串：[{"label":"官网","url":"https://..."}, ...] */
    @Column(length = 2000)
    private String officialLinksJson;

    /** JSON 字符串：[{"label":"官方公众号","imageUrl":"https://..."}, ...] */
    @Column(length = 2000)
    private String qrCodesJson;

    public enum Level { national, provincial, school }
    public enum Status { upcoming, active, urgent, ended }
}
