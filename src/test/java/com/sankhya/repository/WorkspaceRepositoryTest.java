package com.sankhya.repository;

import com.sankhya.common.enums.UserRole;
import com.sankhya.common.enums.WorkspaceRole;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.entity.WorkspaceMembership;
import com.sankhya.workspace.repository.WorkspaceMembershipRepository;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class WorkspaceRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Test
    @DisplayName("Should persist workspace with owner relationship and find accessible workspaces")
    void shouldPersistAndFindAccessibleWorkspaces() {
        User owner = userRepository.save(new User("Rajesh Kumar", "owner@store.com", "hash", UserRole.ROLE_USER));
        User collaborator = userRepository.save(new User("Vikram Singh", "vikram@store.com", "hash", UserRole.ROLE_USER));

        // Workspace 1: Owned by Rajesh
        Workspace ws1 = new Workspace("FreshMart Vijayawada", "Main store", owner);
        Workspace savedWs1 = workspaceRepository.save(ws1);
        membershipRepository.save(new WorkspaceMembership(savedWs1, owner, WorkspaceRole.OWNER));

        // Workspace 2: Owned by Vikram, Rajesh is a Member
        Workspace ws2 = new Workspace("Green Valley Guntur", "Branch store", collaborator);
        Workspace savedWs2 = workspaceRepository.save(ws2);
        membershipRepository.save(new WorkspaceMembership(savedWs2, collaborator, WorkspaceRole.OWNER));
        membershipRepository.save(new WorkspaceMembership(savedWs2, owner, WorkspaceRole.MEMBER));

        // Rajesh should have access to both workspaces
        List<Workspace> accessibleToRajesh = workspaceRepository.findAllAccessibleByUserId(owner.getId());
        assertThat(accessibleToRajesh).hasSize(2);

        // Vikram should only have access to ws2
        List<Workspace> accessibleToVikram = workspaceRepository.findAllAccessibleByUserId(collaborator.getId());
        assertThat(accessibleToVikram).hasSize(1);
        assertThat(accessibleToVikram.get(0).getName()).isEqualTo("Green Valley Guntur");
    }
}
