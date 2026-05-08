package cn.edu.ruc.kal.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceMilestoneRepository extends JpaRepository<WorkspaceMilestone, Long> {
    List<WorkspaceMilestone> findByWorkspaceIdOrderBySortOrderAscIdAsc(String workspaceId);
    long countByWorkspaceIdAndStatus(String workspaceId, WorkspaceMilestone.Status status);
    long countByWorkspaceId(String workspaceId);
}
