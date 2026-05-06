package cn.edu.ruc.kal.competition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompetitionNewsRepository extends JpaRepository<CompetitionNews, String> {

    List<CompetitionNews> findByCompetitionIdOrderByPublishAtDesc(String competitionId);

    @Query("""
        select n from CompetitionNews n
        where (:keyword is null or :keyword = ''
            or lower(n.title)   like lower(concat('%', :keyword, '%'))
            or lower(n.summary) like lower(concat('%', :keyword, '%')))
          and (:status is null or :status = '' or n.status = :status)
        order by n.publishAt desc, n.sortOrder asc
        """)
    Page<CompetitionNews> search(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 Pageable pageable);
}
