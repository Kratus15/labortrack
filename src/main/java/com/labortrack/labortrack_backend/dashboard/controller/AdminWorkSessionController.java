package com.labortrack.labortrack_backend.dashboard.controller;

import com.labortrack.labortrack_backend.dashboard.dto.response.WorkSessionSummaryResponse;
import com.labortrack.labortrack_backend.dashboard.service.DashboardService;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * This controller is used to expose endpoints that shows
 * which employees are currently clocked in.
 */
@RestController
@RequestMapping("/api/admin/work-sessions")
public class AdminWorkSessionController {

    private final DashboardService dashboardService;
    public AdminWorkSessionController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * This method-endpoint returns currently open work sessions
     * belonging only to the given authenticated company (admin user).
     * Instead of using param to check company, rather use the saved
     * authenticated user for the companyId, that would be more secure.
     */
    @GetMapping("/open")
    public List<WorkSessionSummaryResponse> getOpenWorkSessions(
            @AuthenticationPrincipal LaborTrackUserDetails principal
    ) {
        return dashboardService.getOpenWorkSessions(
                principal.getCompanyId()
        );
    }
}
