package com.labortrack.labortrack_backend.dashboard.dto.response;

import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * This DTO response holds all information
 * shown on an employee's personal dashboard.
 * These are all fields an employee would be
 * able to see on his dashboard on the front
 * end.
 */
public record EmployeeDashboardResponse(
        Long employeeId,
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
        long todayWorkedMinutes,
        List<WorkSessionSummaryResponse> recentWorkSessions
) {
}
