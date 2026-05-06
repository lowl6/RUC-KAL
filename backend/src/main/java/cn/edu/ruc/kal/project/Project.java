package cn.edu.ruc.kal.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kal_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @Column(length = 32)
    private String projectId;

    @Column(nullable = false, length = 80)
    private String projectName;

    @Column(length = 160)
    private String oneLiner;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private Type projectType;

    @Column(length = 30)
    private String competitionShort;

    @Column(length = 60)
    private String competitionTarget;

    private LocalDate competitionDeadline;
    private LocalDate teamDeadline;

    private Integer currentMembers;
    private Integer neededCount;
    private Integer weeklyHours;

    @Column(length = 4000)
    private String detail;

    /** 逗号分隔的标签 */
    @Column(length = 400)
    private String tagsCsv;

    @Column(length = 32)
    private String creatorId;

    private Integer viewCount;
    private Integer applyCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ProjectRole> roles = new ArrayList<>();

    public enum Type { innovation, creation, entrepreneurship }
    public enum Status { recruiting, completed, closed, hidden, deleted }
}
