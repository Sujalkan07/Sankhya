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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
public class WorkspaceMembershipRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Test
    @DisplayName("Should prevent duplicate membership for same user in same workspace")
    void shouldPreventDuplicateMembership() {
        User user = userRepository.save(new User("Rajesh Kumar", "rajesh@store.com", "hash", UserRole.ROLE_USER));
        Workspace ws = workspaceRepository.save(new Workspace("Store #1", "Desc", user));

        WorkspaceMembership m1 = new WorkspaceMembership(ws, user, WorkspaceRole.OWNER);
        membershipRepository.saveAndFlush(m1);

        WorkspaceMembership m2 = new WorkspaceMembership(ws, user, WorkspaceRole.MEMBER);

        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipRepository.saveAndFlush(m2);
        });
    }

    @Test
    @DisplayName("Should verify existsByWorkspaceIdAndUserId")
    void shouldCheckMembershipExistence() {
        User user = userRepository.save(new User("Anita Sharma", "anita@store.com", "hash", UserRole.ROLE_USER));
        Workspace ws = workspaceRepository.save(new Workspace("Store #2", "Desc", user));

        assertThat(membershipRepository.existsByWorkspaceIdAndUserId(ws.getId(), user.getId())).isFalse();

        membershipRepository.save(new WorkspaceMembership(ws, user, WorkspaceRole.OWNER));

        assertThat(membershipRepository.existsByWorkspaceIdAndUserId(ws.getId(), user.getId())).isTrue();
    }
}
