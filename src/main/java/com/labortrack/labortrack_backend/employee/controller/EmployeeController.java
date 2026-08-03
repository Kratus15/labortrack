package com.labortrack.labortrack_backend.employee.controller;

import com.labortrack.labortrack_backend.employee.dto.request.EmployeeCreationRequest;
import com.labortrack.labortrack_backend.employee.dto.response.EmployeeCreationResponse;
import com.labortrack.labortrack_backend.employee.service.EmployeeCreationResult;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.service.EmployeeService;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class EmployeeController {

    /**
     * Handles employee-related http requests under /api/companies.
     */

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Perform createEmployee action using companyId and EmployeeCreation request dto.
     * Request would be redelegate to service layer. Employee would be created, IFF
     * admin user (company) perform that operation, and company exists, the given email is unique.
     * This operation would create (employee profile + employee user), and will be given initial
     * temp password that would have to be changed at first login.
     */
    @PostMapping("/{companyId}/employees")
    public ResponseEntity<EmployeeCreationResponse> createEmployee(
            @PathVariable Long companyId,
           @AuthenticationPrincipal LaborTrackUserDetails authenticatedUser,
           @Valid @RequestBody EmployeeCreationRequest request) {

        // company provided in the url can be created, don't trust it
        // confirm it matches authenticated admin's company.
        // this confirm url is valid
        if (!authenticatedUser.getCompanyId().equals(companyId)) {
            throw new AccessDeniedException(
                    "You cannot create employees for another company"
            );
        }

        // instead of using the url company pass, use the authenticate user
        EmployeeCreationResult result = employeeService.createEmployee(
                authenticatedUser.getCompanyId(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.hourlyRate(),
                request.hireDate(),
                request.profileImageUrl()
        );

        Employee employee = result.employee();

        // generate the dto response
        EmployeeCreationResponse  response = new EmployeeCreationResponse(
                employee.getId(),
                employee.getCompany().getId(),
                employee.getUser().getId(),
                employee.getUser().getEmail(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPhone(),
                employee.getHourlyRate(),
                employee.getProfileImageUrl(),
                employee.getStatus(),
                employee.getHireDate(),
                employee.getCreatedAt(),
                result.temporaryPassword()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
