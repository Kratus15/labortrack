package com.labortrack.labortrack_backend.auth.controller;

import com.labortrack.labortrack_backend.auth.dto.request.ChangePasswordRequest;
import com.labortrack.labortrack_backend.auth.dto.request.LoginRequest;
import com.labortrack.labortrack_backend.auth.dto.response.ChangePasswordResponse;
import com.labortrack.labortrack_backend.auth.dto.response.LoginResponse;
import com.labortrack.labortrack_backend.auth.service.AuthService;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller allows users to submit their email and password,
 * and receive a JWT when authentication succeeds.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    /**
     * Handles authentication http requests like login.
     */


    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * authenticates a user and return a signed JWT token with object of basic user information.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * This endpoint changes the password of the currently authenticated user.
     * The user identity comes from the authenticated JWT principal, not from
     * a param supplied by the client.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @AuthenticationPrincipal LaborTrackUserDetails authenticatedUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        ChangePasswordResponse response = authService.changePassword(
                authenticatedUser.getUserId(),
                request
        );

        return ResponseEntity.ok(response);
    }

}
