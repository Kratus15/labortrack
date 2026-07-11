package com.labortrack.labortrack_backend.user.service;

import com.labortrack.labortrack_backend.common.exception.DuplicateResourceException;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Ensure email is available
     */
    @Transactional(readOnly  = true)
    public void ensureEmailIsAvailable(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists: " + normalizedEmail);
        }
    }

    /**
     * Normalize email. Ensure email is space trimmed and lowercase.
     */
    public String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required, cannot be null or blank.");
        }
        return email.trim().toLowerCase();
    }

    /**
     * Meant to generate an initial temporary random passwords for
     * new employee users. Will be given to employees to login,
     * then they must swap it
     */
    public String generateTemporaryPasswordHashedPlaceholder() {
        return "TEMP_PASSWORD_HASH_GENERATOR_NOT_IMPLEMENTED_YET";
    }

}
