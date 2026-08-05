package com.labortrack.labortrack_backend.dashboard.controller;

import com.labortrack.labortrack_backend.dashboard.dto.response.AdminDashboardResponse;
import com.labortrack.labortrack_backend.dashboard.service.DashboardService;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is used to expose the admin dashboard endpoint,
 * showing company totals like all employees, active employees,
 * inactive employees, etc
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    public  AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * This method-endpoint returns dashboard information for the given
     * authenticated company (admin-user). The companyId comes from the
     * authenticated JWT instead of a request param or body preventing,
     * access to another company's data.
     */
    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard(
            @AuthenticationPrincipal LaborTrackUserDetails principal
    ) {
       return dashboardService.getAdminDashboard(
               principal.getCompanyId()
       );
    }
}
