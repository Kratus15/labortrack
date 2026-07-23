package com.labortrack.labortrack_backend.employee.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeCreationRequest(

        @NotBlank(message = "First name is required, cannot be blank or null.")
        @Size(max = 100, message = "First name cannot exceed 100 characters.")
        String firstName,

        @NotBlank(message = "Last name is required, cannot be blank or null.")
        @Size(max = 100, message = "Last name cannot exceed 100 characters.")
        String lastName,

        @Size(max = 30, message = "Phone number cannot exceed 30 characters.")
        String phone,

        @NotNull(message = "Hourly rate is required, cannot be blank or null.")
        @PositiveOrZero(message = "Hourly rate cannot be negative.")
        BigDecimal hourlyRate,

        @NotNull(message = "Hire date is required, cannot be null.")
        LocalDate hireDate,

        @Size(max = 500, message = "Profile image URL cannot exceed 500 characters.")
        String profileImageUrl,

        @NotBlank(message = "Employee email is required, cannot be blank or null.")
        @Email(message = "Employee email must be valid.")
        @Size(max = 254, message = "Employee email cannot exceed 254 characters.")
        String email

) {
}
