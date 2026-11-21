package com.sankhya.repository;

import com.sankhya.common.enums.UserRole;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist and find user by email with timestamps")
    void shouldPersistAndFindUser() {
        User user = new User("Rajesh Kumar", "rajesh@supermarket.com", "$2a$10$hashedpassword", UserRole.ROLE_USER);
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<User> found = userRepository.findByEmail("rajesh@supermarket.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Rajesh Kumar");
    }

    @Test
    @DisplayName("Should enforce unique constraint on email")
    void shouldEnforceUniqueEmailConstraint() {
        User user1 = new User("Rajesh Kumar", "duplicate@supermarket.com", "hash1", UserRole.ROLE_USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("Suresh Patel", "duplicate@supermarket.com", "hash2", UserRole.ROLE_USER);

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    @DisplayName("Should verify existsByEmail works correctly")
    void shouldVerifyExistsByEmail() {
        User user = new User("Anita Sharma", "anita@store.com", "hash", UserRole.ROLE_USER);
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("anita@store.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@store.com")).isFalse();
    }
}
