package com.labortrack.labortrack_backend.auth.dto.response;

import com.labortrack.labortrack_backend.user.enums.UserRole;

// we never return raw password, hash password, neither login security keys like JWT secret.
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        Long companyId,
        Long employeeId,  // employee can be nullable, because an admin (company) may not have an employee profile
        String email,
        UserRole role,
        boolean mustChangePassword
) {
}
