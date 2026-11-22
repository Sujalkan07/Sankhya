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

        // Attach new users to the canonical single-store Naveen Store workspace
        Workspace canonicalStore = workspaceRepository.findAll().stream()
                .filter(w -> "STORE-001".equalsIgnoreCase(w.getStoreKey())
                        || "Naveen Store".equalsIgnoreCase(w.getName()))
                .findFirst()
                .orElse(null);

        if (canonicalStore == null) {
            Workspace initialWorkspace = new Workspace(
                    "Naveen Store",
                    "Primary single-store analytics workspace for Naveen Store (STORE-001 • Lucknow, UP).",
                    savedUser
            );
            initialWorkspace.setStoreKey("STORE-001");
            initialWorkspace.setOwnerName("Sujal Kansal");
            initialWorkspace.setStoreFormat("Supermarket");
            initialWorkspace.setCity("Lucknow");
            initialWorkspace.setState("Uttar Pradesh");
            initialWorkspace.setCountry("India");
            initialWorkspace.setOnboardingStatus("COMPLETED");
            Workspace savedWorkspace = workspaceRepository.save(initialWorkspace);
            membershipRepository.save(new WorkspaceMembership(savedWorkspace, savedUser, WorkspaceRole.OWNER));
        } else {
            membershipRepository.save(new WorkspaceMembership(canonicalStore, savedUser, WorkspaceRole.MEMBER));
        }

        return savedUser;
    }
}
