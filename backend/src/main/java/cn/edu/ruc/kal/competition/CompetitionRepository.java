package cn.edu.ruc.kal.competition;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionRepository extends JpaRepository<Competition, String> {
    long countByStatus(Competition.Status status);
}
