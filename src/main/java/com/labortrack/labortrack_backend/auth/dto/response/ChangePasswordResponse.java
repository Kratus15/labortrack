package com.labortrack.labortrack_backend.auth.dto.response;

public record ChangePasswordResponse(
        String message,
        boolean mustChangePassword
) {
}
