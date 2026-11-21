package com.sankhya.user.service;

import com.sankhya.common.enums.UserRole;
import com.sankhya.common.exception.DuplicateResourceException;
import com.sankhya.common.exception.ResourceNotFoundException;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User createUser(String fullName, String email, String passwordHash, UserRole role) {
        if (existsByEmail(email)) {
            throw new DuplicateResourceException("An account with email " + email + " already exists.");
        }

        User user = new User(fullName, email, passwordHash, role != null ? role : UserRole.ROLE_USER);
        return userRepository.save(user);
    }
}
