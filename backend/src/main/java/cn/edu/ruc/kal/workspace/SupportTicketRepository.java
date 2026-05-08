package cn.edu.ruc.kal.workspace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {

    List<SupportTicket> findByWorkspaceIdOrderByLastActivityAtDesc(String workspaceId);

    long countByWorkspaceIdAndStatusIn(String workspaceId, List<SupportTicket.Status> statuses);

    @Query("""
        select t from SupportTicket t
        where (:assignee is null or :assignee = '' or t.assigneeStaffId = :assignee)
          and (:status is null or t.status = :status)
        order by case when t.status in (cn.edu.ruc.kal.workspace.SupportTicket$Status.open,
                                        cn.edu.ruc.kal.workspace.SupportTicket$Status.in_progress)
                       then 0 else 1 end,
                 t.lastActivityAt desc
        """)
    Page<SupportTicket> searchForStaff(@Param("assignee") String assignee,
                                       @Param("status") SupportTicket.Status status,
                                       Pageable pageable);

    long countByStatus(SupportTicket.Status status);
}
