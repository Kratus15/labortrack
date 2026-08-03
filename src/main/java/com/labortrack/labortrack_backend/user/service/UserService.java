package com.labortrack.labortrack_backend.user.service;

import com.labortrack.labortrack_backend.common.exception.DuplicateResourceException;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class UserService {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%";
    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARACTERS;
    private static final int TEMPORARY_PASSWORD_LENGTH = 16;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Ensure email is available and it is not already register.
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
     * This method simply generates a secure temp password and hashes
     * it for storage. The raw password must be shown to the admin only
     * once. The encoded password should be stored in database.
     */
    public GeneratedTemporaryPassword generateTemporaryPassword() {
        char[] passwordCharacters = new char[TEMPORARY_PASSWORD_LENGTH];

        // first make sure you have a character from each gropu
        passwordCharacters[0] = randomCharacter(UPPERCASE);
        passwordCharacters[1] = randomCharacter(LOWERCASE);
        passwordCharacters[2] = randomCharacter(DIGITS);
        passwordCharacters[3] = randomCharacter(SPECIAL_CHARACTERS);

        // fill the remaining positions with random characters
        for (int index = 4;
             index < TEMPORARY_PASSWORD_LENGTH;
             index++) {

            passwordCharacters[index] = randomCharacter(ALL_CHARACTERS);
        }

        // shuffle password
        shuffle(passwordCharacters);

        String rawPassword = new String(passwordCharacters);
        String passwordHash = passwordEncoder.encode(rawPassword);

        // return object with raw and encoded password
        return new GeneratedTemporaryPassword(
                rawPassword,
                passwordHash
        );
    }

    // HELPER METHODS
    private char randomCharacter(String allowedCharacters) {
        int index = secureRandom.nextInt(allowedCharacters.length());
        return allowedCharacters.charAt(index);
    }
    private void shuffle(char[] characters) {
        for (int index = characters.length-1;
        index > 0;
        index --) {

            int randomIndex = secureRandom.nextInt(index + 1);

            char temporaryCharacter = characters[index];
            characters[index] = characters[randomIndex];
            characters[randomIndex] = temporaryCharacter;
        }
    }

}
