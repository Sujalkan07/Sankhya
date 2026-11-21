package com.sankhya.workspace.repository;

import com.sankhya.workspace.entity.WorkspaceMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, Long> {

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    List<WorkspaceMembership> findByUserId(Long userId);
}
