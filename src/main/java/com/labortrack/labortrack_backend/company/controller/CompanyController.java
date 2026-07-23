package com.labortrack.labortrack_backend.company.controller;

import com.labortrack.labortrack_backend.company.dto.request.CompanyRegistrationRequest;
import com.labortrack.labortrack_backend.company.dto.response.CompanyRegistrationResponse;
import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.service.CompanyRegistrationResult;
import com.labortrack.labortrack_backend.company.service.CompanyService;
import com.labortrack.labortrack_backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    /**
     * Handles company-related http requests under /api/companies.
     */

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * This endpoint registerCompany(request) at /register route is design to received requests
     * from user as JSON data. Using DTO for more specific requests and Validation with
     * @Valid to verify data is valid and safe to use. Delegates registration to the service layer,
     * hashed the password, and create (company + adminUser) and got them as a build-in object.
     * Then using the object generate a custom response DTO which return back to the user.
     */
    @PostMapping("/register")
    public ResponseEntity<CompanyRegistrationResponse> registerCompany(
            @Valid @RequestBody CompanyRegistrationRequest request) {

        // register the company (company + adminUser) and got both objects back
        CompanyRegistrationResult result = companyService.registerCompanyWithAdminUser(
                request.companyName(),
                request.adminEmail(),
                request.adminPassword()
        );

        Company company = result.company();
        User adminUser = result.adminUser();

        // produce the response
        CompanyRegistrationResponse response = new CompanyRegistrationResponse(
                company.getId(),
                company.getName(),
                adminUser.getId(),
                adminUser.getEmail(),
                company.getTimezone(),
                company.isActive(),
                company.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
