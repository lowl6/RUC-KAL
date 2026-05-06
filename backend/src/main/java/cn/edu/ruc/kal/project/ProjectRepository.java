package cn.edu.ruc.kal.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, String> {

    @Query("""
        select p from Project p
        where (:keyword is null or :keyword = ''
            or lower(p.projectName) like lower(concat('%', :keyword, '%'))
            or lower(p.oneLiner)    like lower(concat('%', :keyword, '%')))
          and (:competition is null or :competition = '' or p.competitionShort = :competition)
          and (:status is null or p.status = :status)
        order by p.createdAt desc
        """)
    Page<Project> search(@Param("keyword") String keyword,
                         @Param("competition") String competition,
                         @Param("status") Project.Status status,
                         Pageable pageable);

    List<Project> findByCreatorIdOrderByCreatedAtDesc(String creatorId);

    long countByStatus(Project.Status status);
}
