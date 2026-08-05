package com.labortrack.labortrack_backend.dashboard.controller;

import com.labortrack.labortrack_backend.dashboard.dto.response.EmployeeDashboardResponse;
import com.labortrack.labortrack_backend.dashboard.service.DashboardService;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is used to expose the logged-in employee's
 * personal dashboard, including their information, clock-in
 * status, open session, recent sessions and worked minutes.
 */
@RestController
@RequestMapping("/api/employee/me")
public class EmployeeDashboardController {

    private final DashboardService dashboardService;
    public EmployeeDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * This method-endpoint returns dashboard information for the
     * given authenticated employee (pov: myself). The employeeId
     * comes directly from the authenticated JWT principal, so an
     * employee cannot request another employee's dashboard.
     */
    @GetMapping("/dashboard")
    public EmployeeDashboardResponse getDashboard(
            @AuthenticationPrincipal LaborTrackUserDetails principal
    ) {
        return dashboardService.getEmployeeDashboard(
                principal.getEmployeeId()
        );
    }
}
