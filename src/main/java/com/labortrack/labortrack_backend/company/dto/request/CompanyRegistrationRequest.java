package com.labortrack.labortrack_backend.company.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRegistrationRequest(

        @NotBlank(message = "Company name is required, cannot be blank or null.")
        @Size(max = 150, message = "Company name cannot exceed 150 characters.")
        String companyName,

        @NotBlank(message = "Admin email is required, cannot be blank or null.")
        @Email(message = "Admin email must be valid.")
        @Size(max = 254, message = "Admin email cannot exceed 254 characters.")
        String adminEmail,

        @NotBlank(message = "Admin password is required, cannot be blank or null.")
        @Size(min = 8, max = 72, message = "Admin password is required and must be between 8 and 72 characters."
        )
        String adminPassword
) {
}