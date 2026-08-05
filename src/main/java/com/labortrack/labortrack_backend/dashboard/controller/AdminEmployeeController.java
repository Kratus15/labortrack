package com.labortrack.labortrack_backend.dashboard.controller;

import com.labortrack.labortrack_backend.dashboard.dto.response.AdminEmployeeDetailResponse;
import com.labortrack.labortrack_backend.dashboard.dto.response.AdminEmployeeListItemResponse;
import com.labortrack.labortrack_backend.dashboard.service.DashboardService;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * This controller is used to expose admin employee endpoints
 * such as listing employees, filtering them by status,
 * and also viewing one employee's details.
 */
@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {

    private final DashboardService dashboardService;
    public AdminEmployeeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * This method-endpoint returns employees belonging to the authenticated
     * given company (admin user). It has optional status param, that can be
     * used to filter employees by ACTIVE or INACTIVE status.
     */
    @GetMapping
    public List<AdminEmployeeListItemResponse> getEmployees(
            @AuthenticationPrincipal LaborTrackUserDetails principal,
            @RequestParam(required = false) EmployeeStatus status ) {
        return dashboardService.getAdminEmployees(
                principal.getCompanyId(),
                status
        );
    }

    /**
     * This method-endpoint returns informaiton for one employee belonging
     * to the authenticated given company. The service checks both employeeId
     * and companyId to enforce company isolation. Both company and employee
     * IDs must match to return.
     */
    @GetMapping("/{employeeId}")
    public AdminEmployeeDetailResponse getEmployee(
            @AuthenticationPrincipal LaborTrackUserDetails principal,
            @PathVariable Long employeeId) {
        return dashboardService.getAdminEmployeeDetail(
                principal.getCompanyId(),
                employeeId
        );
    }

}
