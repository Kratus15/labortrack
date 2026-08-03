package com.labortrack.labortrack_backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required, cannot be blank or null.")
        String currentPassword,

        @NotBlank(message = "New password is required, cannot be blank or null.")
        @Size(
                min = 12,
                max = 64,
                message = "New password must be between 12 and 64 characters."
        )
        String newPassword
) {
}
