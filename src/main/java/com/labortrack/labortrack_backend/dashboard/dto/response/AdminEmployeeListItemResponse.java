package com.labortrack.labortrack_backend.dashboard.dto.response;

import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * This DTO response holds the lightweight employee
 * data shown in the admin employee list. This
 * only included things that are needed for the
 * used, making it simpler and lighter to load.
 */
public record AdminEmployeeListItemResponse(
        Long employeeId,
        String firstName,
        String lastName,
        String email,
        String phone,
        BigDecimal hourlyRate,
        LocalDate hireDate,
        EmployeeStatus status,
        String profileImageUrl,
        boolean currentlyClockedIn
) {
}
