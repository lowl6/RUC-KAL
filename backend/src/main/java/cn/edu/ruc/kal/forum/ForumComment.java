package cn.edu.ruc.kal.forum;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_forum_comment", indexes = {
        @Index(name = "idx_fc_post", columnList = "postId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String postId;

    @Column(length = 32, nullable = false)
    private String authorId;

    @Column(length = 60)
    private String authorName;

    @Column(length = 1500, nullable = false)
    private String content;

    private Integer likeCount;

    @Column(length = 16)
    private String status; // published / hidden / deleted

    private LocalDateTime createdAt;
}
