package cn.edu.ruc.kal.forum;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kal_forum_post")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPost {

    @Id
    @Column(length = 32)
    private String postId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String content;

    @Column(length = 300)
    private String excerpt;

    @Column(length = 24)
    private String topic;

    @Column(length = 32)
    private String authorId;

    @Column(length = 60)
    private String authorName;

    private Boolean pinned;
    private Boolean essence;

    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime lastReplyAt;

    public enum Status { published, hidden, deleted }
}
