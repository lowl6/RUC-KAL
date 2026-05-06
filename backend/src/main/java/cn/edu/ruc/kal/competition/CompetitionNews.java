package cn.edu.ruc.kal.competition;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_competition_news", indexes = {
        @Index(name = "idx_news_competition", columnList = "competitionId"),
        @Index(name = "idx_news_publish",     columnList = "publishAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionNews {

    @Id
    @Column(length = 32)
    private String newsId;

    /** 可空：综合资讯不绑定具体比赛 */
    @Column(length = 32)
    private String competitionId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 60)
    private String source;

    @Column(length = 300)
    private String summary;

    @Column(length = 4000)
    private String content;

    @Column(length = 300)
    private String link;

    @Column(length = 200)
    private String coverUrl;

    private LocalDateTime publishAt;

    @Column(length = 16)
    private String status; // published / hidden

    private Integer sortOrder;
}
