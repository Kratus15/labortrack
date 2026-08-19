package com.labortrack.labortrack_backend.dashboard.controller;

import com.labortrack.labortrack_backend.common.dto.response.PageResponse;
import com.labortrack.labortrack_backend.dashboard.dto.response.AdminEmployeeDetailResponse;
import com.labortrack.labortrack_backend.dashboard.dto.response.AdminEmployeeListItemResponse;
import com.labortrack.labortrack_backend.dashboard.service.DashboardService;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
     * This method-endpoint returns a paginated list of employees
     * that belongs to the authenticated admin's company. An optional
     * status parameter can be used to filter employees by their
     * current employee status.
     */
    @GetMapping
    public PageResponse<AdminEmployeeListItemResponse> getEmployees(
            @AuthenticationPrincipal LaborTrackUserDetails principal,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = {"lastName", "firstName"},
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {
        return dashboardService.getAdminEmployees(
                principal.getCompanyId(),
                status,
                pageable
        );
    }

    /**
     * This method-endpoint returns information for one employee belonging
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
