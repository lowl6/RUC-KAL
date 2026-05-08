package cn.edu.ruc.kal.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    List<WorkspaceMember> findByWorkspaceIdOrderByJoinedAtAsc(String workspaceId);
    List<WorkspaceMember> findByUserId(String userId);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(String workspaceId, String userId);
    boolean existsByWorkspaceIdAndUserId(String workspaceId, String userId);
    void deleteByWorkspaceIdAndUserId(String workspaceId, String userId);
}
