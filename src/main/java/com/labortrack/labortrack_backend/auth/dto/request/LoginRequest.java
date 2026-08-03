package com.labortrack.labortrack_backend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required, cannot be blank or null.")
        @Email(message = "Email must be valid")
        @Size(max=320, message = "Email must not exceed 320 characters.")
        String email,
        @NotBlank(message = "Password is required, cannot be blank or null.")
        String password
) {
}
