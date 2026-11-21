package com.sankhya.service;

import com.sankhya.common.enums.UserRole;
import com.sankhya.common.exception.UnauthorizedAccessException;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.dto.WorkspaceCreateRequest;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WorkspaceServiceTest {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should create workspace, persist active workspace in session, and list user workspaces")
    void shouldCreateAndManageWorkspaces() {
        User user = userRepository.save(new User("Rajesh Kumar", "rajesh.ws@store.com", "hash", UserRole.ROLE_USER));
        MockHttpSession session = new MockHttpSession();

        WorkspaceCreateRequest request = new WorkspaceCreateRequest(
                "FreshMart Vijayawada",
                "Flagship store with 14 POS lanes"
        );

        Workspace created = workspaceService.createWorkspace(request, user);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("FreshMart Vijayawada");
        assertThat(created.getOwner().getId()).isEqualTo(user.getId());

        // Select and persist in session
        workspaceService.setActiveWorkspace(created.getId(), session, user.getId());
        Workspace active = workspaceService.getActiveWorkspace(session, user.getId());
        assertThat(active).isNotNull();
        assertThat(active.getId()).isEqualTo(created.getId());

        // List workspaces
        List<WorkspaceResponse> responses = workspaceService.getUserWorkspaces(user.getId(), active.getId());
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isActive()).isTrue();
        assertThat(responses.get(0).isOwner()).isTrue();
    }

    @Test
    @DisplayName("Should prevent cross-user unauthorized workspace access")
    void shouldPreventUnauthorizedWorkspaceAccess() {
        User userA = userRepository.save(new User("User A", "user.a@store.com", "hash", UserRole.ROLE_USER));
        User userB = userRepository.save(new User("User B", "user.b@store.com", "hash", UserRole.ROLE_USER));

        Workspace wsA = workspaceService.createWorkspace(
                new WorkspaceCreateRequest("Store A", "Desc"),
                userA
        );

        // User B must NOT be able to access User A's workspace
        assertThrows(UnauthorizedAccessException.class, () -> {
            workspaceService.getWorkspaceByIdAndValidateAccess(wsA.getId(), userB.getId());
        });

        // User B cannot select User A's workspace into their session
        MockHttpSession sessionB = new MockHttpSession();
        assertThrows(UnauthorizedAccessException.class, () -> {
            workspaceService.setActiveWorkspace(wsA.getId(), sessionB, userB.getId());
        });
    }
}
