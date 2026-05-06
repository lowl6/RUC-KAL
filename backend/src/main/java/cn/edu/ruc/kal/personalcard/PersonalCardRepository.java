package cn.edu.ruc.kal.personalcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonalCardRepository extends JpaRepository<PersonalCard, String> {

    Optional<PersonalCard> findByUserId(String userId);

    @Query("""
        select c from PersonalCard c
        where (:keyword is null or :keyword = ''
            or lower(c.displayName) like lower(concat('%', :keyword, '%'))
            or lower(c.skillsCsv)   like lower(concat('%', :keyword, '%'))
            or lower(c.selfIntro)   like lower(concat('%', :keyword, '%')))
          and (:role is null or :role = '' or c.targetRole = :role)
          and (:status is null or c.status = :status)
        order by c.createdAt desc
        """)
    Page<PersonalCard> search(@Param("keyword") String keyword,
                              @Param("role") String role,
                              @Param("status") PersonalCard.Status status,
                              Pageable pageable);
}
