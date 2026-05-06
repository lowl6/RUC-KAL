package cn.edu.ruc.kal.forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumPostRepository extends JpaRepository<ForumPost, String> {

    @Query("""
        select p from ForumPost p
        where (:keyword is null or :keyword = ''
            or lower(p.title)   like lower(concat('%', :keyword, '%'))
            or lower(p.content) like lower(concat('%', :keyword, '%')))
          and (:topic is null or :topic = '' or p.topic = :topic)
          and (:status is null or p.status = :status)
        order by p.pinned desc, p.lastReplyAt desc
        """)
    Page<ForumPost> search(@Param("keyword") String keyword,
                           @Param("topic") String topic,
                           @Param("status") ForumPost.Status status,
                           Pageable pageable);

    long countByStatus(ForumPost.Status status);
}
