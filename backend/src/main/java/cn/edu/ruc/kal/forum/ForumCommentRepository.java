package cn.edu.ruc.kal.forum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
    List<ForumComment> findByPostIdAndStatusOrderByCreatedAtAsc(String postId, String status);
    long countByPostIdAndStatus(String postId, String status);
}
