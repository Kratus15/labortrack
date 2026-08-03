package com.labortrack.labortrack_backend.security.user;

import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** This service loads a user from the database when they try to log in.
 * It then converts the User entity into our custom LaborTrackUserDetails
 * object so Spring Security can use the user's information for authentication
 * and authorization.
 */
@Service
public class LaborTrackUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    public LaborTrackUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public LaborTrackUserDetails loadUserByUsername(String email) {
        String normalizedEmail = normalizeEmail(email);

        // load the user from db throw if not found
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid email or password") // using generic alert because it's safer
                );

        return LaborTrackUserDetails.from(user);
    }

    // HELPER METHOD
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new  UsernameNotFoundException("Invalid email or password"); // using generic alert because it's safer
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
