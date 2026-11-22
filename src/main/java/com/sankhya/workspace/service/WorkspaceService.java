package com.sankhya.workspace.service;

import com.sankhya.common.enums.WorkspaceRole;
import com.sankhya.common.exception.ResourceNotFoundException;
import com.sankhya.common.exception.UnauthorizedAccessException;
import com.sankhya.security.AuthenticationSuccessHandler;
import com.sankhya.user.entity.User;
import com.sankhya.workspace.dto.WorkspaceCreateRequest;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.entity.WorkspaceMembership;
import com.sankhya.workspace.repository.WorkspaceMembershipRepository;
import com.sankhya.workspace.repository.WorkspaceRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMembershipRepository membershipRepository) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public Workspace createWorkspace(WorkspaceCreateRequest request, User owner) {
        Workspace workspace = new Workspace(
                request.getName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : "",
                owner
        );
        if (request.getStoreFormat() != null && !request.getStoreFormat().isBlank()) {
            workspace.setStoreFormat(request.getStoreFormat());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            workspace.setCity(request.getCity().trim());
        }
        if (request.getState() != null && !request.getState().isBlank()) {
            workspace.setState(request.getState().trim());
        }
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            workspace.setCountry(request.getCountry().trim());
        }
        workspace.setOnboardingStatus("DATASET_ONBOARDING");
        Workspace saved = workspaceRepository.save(workspace);

        WorkspaceMembership membership = new WorkspaceMembership(saved, owner, WorkspaceRole.OWNER);
        membershipRepository.save(membership);

        return saved;
    }

    @Transactional
    public Workspace updateOnboardingStatus(Long workspaceId, Long userId, String status) {
        Workspace workspace = getWorkspaceByIdAndValidateAccess(workspaceId, userId);
        workspace.setOnboardingStatus(status);
        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace findById(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspaceByIdAndValidateAccess(Long workspaceId, Long userId) {
        Workspace workspace = findById(workspaceId);

        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(userId);
        if (!isOwner && !membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedAccessException("You do not have permission to access workspace #" + workspaceId);
        }

        return workspace;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getUserWorkspaces(Long userId, Long activeWorkspaceId) {
        List<Workspace> accessible = workspaceRepository.findAllAccessibleByUserId(userId);
        return accessible.stream()
                .map(w -> WorkspaceResponse.fromEntity(w, userId, activeWorkspaceId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Workspace getActiveWorkspace(HttpSession session, Long userId) {
        Long activeWorkspaceId = (Long) session.getAttribute(AuthenticationSuccessHandler.ACTIVE_WORKSPACE_ID_SESSION_KEY);

        if (activeWorkspaceId != null) {
            try {
                return getWorkspaceByIdAndValidateAccess(activeWorkspaceId, userId);
            } catch (ResourceNotFoundException | UnauthorizedAccessException ex) {
                session.removeAttribute(AuthenticationSuccessHandler.ACTIVE_WORKSPACE_ID_SESSION_KEY);
            }
        }

        List<Workspace> accessible = workspaceRepository.findAllAccessibleByUserId(userId);
        if (!accessible.isEmpty()) {
            Workspace defaultActive = accessible.get(0);
            session.setAttribute(AuthenticationSuccessHandler.ACTIVE_WORKSPACE_ID_SESSION_KEY, defaultActive.getId());
            return defaultActive;
        }

        return null;
    }

    @Transactional(readOnly = true)
    public void setActiveWorkspace(Long workspaceId, HttpSession session, Long userId) {
        // Validate access first
        Workspace workspace = getWorkspaceByIdAndValidateAccess(workspaceId, userId);
        session.setAttribute(AuthenticationSuccessHandler.ACTIVE_WORKSPACE_ID_SESSION_KEY, workspace.getId());
    }
}
