package com.sankhya.auth.service;

import com.sankhya.auth.dto.RegisterRequest;
import com.sankhya.common.enums.UserRole;
import com.sankhya.common.enums.WorkspaceRole;
import com.sankhya.common.exception.DuplicateResourceException;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.entity.WorkspaceMembership;
import com.sankhya.workspace.repository.WorkspaceMembershipRepository;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       WorkspaceRepository workspaceRepository,
                       WorkspaceMembershipRepository membershipRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email " + normalizedEmail + " already exists.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getFullName().trim(), normalizedEmail, encodedPassword, UserRole.ROLE_USER);
        User savedUser = userRepository.save(user);

        // Provision an initial default supermarket workspace for the new user
        String defaultWorkspaceName = request.getFullName().trim() + " Supermarket";
        Workspace initialWorkspace = new Workspace(
                defaultWorkspaceName,
                "Primary supermarket workspace for " + request.getFullName().trim() + ".",
                savedUser
        );
        Workspace savedWorkspace = workspaceRepository.save(initialWorkspace);

        // Add user as OWNER membership
        WorkspaceMembership membership = new WorkspaceMembership(savedWorkspace, savedUser, WorkspaceRole.OWNER);
        membershipRepository.save(membership);

        return savedUser;
    }
}
