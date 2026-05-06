package cn.edu.ruc.kal.forum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PK> {
    boolean existsByPostIdAndUserId(String postId, String userId);
    long deleteByPostIdAndUserId(String postId, String userId);
    List<PostLike> findByUserIdAndPostIdIn(String userId, List<String> postIds);
}
