package com.labortrack.labortrack_backend.dashboard.dto.response;

import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * This DTO response holds the full read-only
 * details for one employee. Includes all the
 * necessary information that an employees
 * needs to show on the frontend.
 */
public record AdminEmployeeDetailResponse(
        Long employeeId,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        BigDecimal hourlyRate,
        LocalDate hireDate,
        EmployeeStatus status,
        String profileImageUrl,
        boolean currentlyClockedIn,
        WorkSessionSummaryResponse currentOpenSession,
        List<WorkSessionSummaryResponse> recentWorkSessions
) {
}
