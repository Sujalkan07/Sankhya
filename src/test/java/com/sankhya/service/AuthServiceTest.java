package com.sankhya.service;

import com.sankhya.auth.dto.RegisterRequest;
import com.sankhya.auth.service.AuthService;
import com.sankhya.common.exception.DuplicateResourceException;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should successfully register user, hash password using BCrypt, and provision default workspace")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "Sujal Patel",
                "sujal@supermarket.com",
                "Secret@123",
                "Secret@123",
                true
        );

        User registered = authService.registerUser(request);

        assertThat(registered.getId()).isNotNull();
        assertThat(registered.getEmail()).isEqualTo("sujal@supermarket.com");
        assertThat(registered.getFullName()).isEqualTo("Sujal Patel");

        // Verify password is NOT stored in plaintext and matches BCrypt hash
        assertThat(registered.getPasswordHash()).isNotEqualTo("Secret@123");
        assertThat(passwordEncoder.matches("Secret@123", registered.getPasswordHash())).isTrue();

        // Verify user was attached to canonical single-store Naveen Store (STORE-001, Lucknow)
        List<Workspace> workspaces = workspaceRepository.findAllAccessibleByUserId(registered.getId());
        assertThat(workspaces).isNotEmpty();
        assertThat(workspaces.get(0).getName()).isEqualTo("Naveen Store");
        assertThat(workspaces.get(0).getStoreKey()).isEqualTo("STORE-001");
        assertThat(workspaces.get(0).getCity()).isEqualTo("Lucknow");
        assertThat(workspaces.get(0).getOnboardingStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should reject duplicate email registration")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request1 = new RegisterRequest(
                "User One",
                "duplicate@supermarket.com",
                "Password123",
                "Password123",
                true
        );
        authService.registerUser(request1);

        RegisterRequest request2 = new RegisterRequest(
                "User Two",
                "duplicate@supermarket.com",
                "Password456",
                "Password456",
                true
        );

        assertThrows(DuplicateResourceException.class, () -> {
            authService.registerUser(request2);
        });
    }
}
