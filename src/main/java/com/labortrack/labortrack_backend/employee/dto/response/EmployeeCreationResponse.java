package com.labortrack.labortrack_backend.employee.dto.response;

import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * This DTO response is return only when an employee account is created.
 * The temporary password is returned to the authenticated admin once,
 * so it can be provided securely to the employee. Only its hash will be
 * stored in the database.
 */
public record EmployeeCreationResponse(
        Long employeeId,
        Long companyId,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String phone,
        BigDecimal hourlyRate,
        String profileImageUrl,
        EmployeeStatus status,
        LocalDate hireDate,
        OffsetDateTime createdAt,
        String temporaryPassword
) {
}
