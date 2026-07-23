package com.labortrack.labortrack_backend.employee.controller;

import com.labortrack.labortrack_backend.employee.dto.request.EmployeeCreationRequest;
import com.labortrack.labortrack_backend.employee.dto.response.EmployeeResponse;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<EmployeeResponse> createEmployee(
            @PathVariable Long companyId,
           @Valid @RequestBody EmployeeCreationRequest request) {

        // create employee through service layer
        Employee employee = employeeService.createEmployee(
                companyId,
                request.email(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.hourlyRate(),
                request.hireDate(),
                request.profileImageUrl()
        );

        // generate the dto response
        EmployeeResponse response = new EmployeeResponse(
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
                employee.getCreatedAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
