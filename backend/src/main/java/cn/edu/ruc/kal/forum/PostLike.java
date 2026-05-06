package cn.edu.ruc.kal.forum;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "kal_post_like")
@IdClass(PostLike.PK.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLike {

    @Id
    @Column(length = 32)
    private String postId;

    @Id
    @Column(length = 32)
    private String userId;

    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private String postId;
        private String userId;

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(postId, pk.postId) && Objects.equals(userId, pk.userId);
        }
        @Override public int hashCode() { return Objects.hash(postId, userId); }
    }
}
