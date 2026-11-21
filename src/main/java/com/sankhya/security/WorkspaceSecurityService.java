package com.sankhya.security;

import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceMembershipRepository;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceSecurityService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public WorkspaceSecurityService(WorkspaceRepository workspaceRepository,
                                  WorkspaceMembershipRepository membershipRepository) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public boolean canAccessWorkspace(Long workspaceId, Long userId) {
        if (workspaceId == null || userId == null) {
            return false;
        }

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            return false;
        }

        if (workspace.getOwner() != null && workspace.getOwner().getId().equals(userId)) {
            return true;
        }

        return membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }
}
