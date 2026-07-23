package com.labortrack.labortrack_backend.employee.dto.response;

import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EmployeeResponse(
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
        OffsetDateTime createdAt
        ) {
}
