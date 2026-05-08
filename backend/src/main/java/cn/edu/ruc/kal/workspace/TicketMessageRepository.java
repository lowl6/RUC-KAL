package cn.edu.ruc.kal.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(String ticketId);
    long countByTicketId(String ticketId);
}
