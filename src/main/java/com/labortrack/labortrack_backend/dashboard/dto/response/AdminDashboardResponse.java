package com.labortrack.labortrack_backend.dashboard.dto.response;

import java.util.List;

/**
 * This DTO response holds the information
 * shown on the admin dashboard.
 */
public record AdminDashboardResponse(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long currentlyClockedInEmployees,
        long todayWorkedMinutes,
        List<WorkSessionSummaryResponse> recentWorkSessions
) {
}
