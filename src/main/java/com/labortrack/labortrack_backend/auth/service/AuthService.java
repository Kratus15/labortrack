package com.labortrack.labortrack_backend.auth.service;

import com.labortrack.labortrack_backend.auth.dto.request.ChangePasswordRequest;
import com.labortrack.labortrack_backend.auth.dto.request.LoginRequest;
import com.labortrack.labortrack_backend.auth.dto.response.ChangePasswordResponse;
import com.labortrack.labortrack_backend.auth.dto.response.LoginResponse;
import com.labortrack.labortrack_backend.common.exception.ResourceNotFoundException;
import com.labortrack.labortrack_backend.security.jwt.JwtService;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class handles LaborTrack login process, service and constraints.
 * The service simply sends username(email) and password to spring security
 * to authenticate. Then receives the authenticated user, when credentials are
 * valid generate a JWT token for the user, and return the token with object that
 * have claims of user information.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * This authenticated a login request and return a signed JWT token.
     * If the email, password or account status is invalid, spring security
     * throws an authentication exception and not a signed JWT.
     */
    public LoginResponse login(LoginRequest request) {
        // wrap email and password into an object and send it to authenticateManager use
        //authenticationProvider to verify email and password. If matched authenticate becomes
        //true otherwise throw exception.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email(),
                        request.password()
                )
        );
        // get principals
        LaborTrackUserDetails userDetails = (LaborTrackUserDetails) authentication.getPrincipal();

        // generate a signed token
        String accessToken = jwtService.generateToken(userDetails);

        // return a login response
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userDetails.getUserId(),
                userDetails.getCompanyId(),
                userDetails.getEmployeeId(),
                userDetails.getUsername(),
                userDetails.getRole(),
                userDetails.isMustChangePassword()
        );
    }

    /**
     * This method changes the authenticated user's password. First, the
     * current password must be correct and the new password must be
     * different so it can change for a new password. New password will
     * be stored in db hash.
     */
    @Transactional
    public ChangePasswordResponse changePassword(
            Long authenticatedUserId,
            ChangePasswordRequest request) {
        // find the user in db
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "User with id=" + authenticatedUserId + " not found."
                        )
                );

        // verify that provided password matches the stored hash one.
        // to change the password, user must verify the one they have first
        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Current password is incorrect."
            );
        }

        // prevent the user from selecting the same password again
        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "New password must be different from old password."
            );
        }

        // otherwise stored the new password on db hash.
        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        // update parameter
        user.setMustChangePassword(false);

        userRepository.save(user);

        return new ChangePasswordResponse(
                "Password changed successfully.",
                false
        );
    }
}
