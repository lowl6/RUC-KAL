package cn.edu.ruc.kal.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    List<Workspace> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);
}
