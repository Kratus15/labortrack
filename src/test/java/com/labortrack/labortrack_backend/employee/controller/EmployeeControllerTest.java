package com.labortrack.labortrack_backend.employee.controller;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.employee.dto.request.EmployeeCreationRequest;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    /**
     * This test checks when invalid or malformed employee information is sent
     * throw employee creation dto. It sends blank names, negative hourlyRate,
     * invalid email, and not hireDate. All of those are invalid DTO requests
     * information, therefore we expect an invalid 400 status code with validation
     * errors. This confirms that Invalid request DTO data gets rejected before
     * acutually hitting the service layer.
     */
    @Test
    void createEmployeeReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        Company testCompany = createCompanyTest();

        // wrong or malformed inputs
        EmployeeCreationRequest request = new EmployeeCreationRequest(
                "",
                "",
                null,
                new BigDecimal("-1.00"),
                null,
                null,
                "not-an-email"
        );

        mockMvc.perform(post("/api/companies/{companyId}/employees", testCompany.getId())
                // request passes through authentication and authorization before DTO validation. Produce expected 400 status code
                        .with(user(createAdminPrincipal(testCompany.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        ))
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.error").value("Bad Request"))
                        .andExpect(jsonPath("$.message")
                                .value("Request validation failed"))
                        .andExpect(jsonPath("$.path")
                                .value("/api/companies/" + testCompany.getId() + "/employees"))
                        .andExpect(jsonPath("$.validationErrors.firstName").exists())
                        .andExpect(jsonPath("$.validationErrors.lastName").exists())
                        .andExpect(jsonPath("$.validationErrors.email").exists())
                        .andExpect(jsonPath("$.validationErrors.hourlyRate").exists())
                        .andExpect(jsonPath("$.validationErrors.hireDate").exists());

    }

    /**
     * This test checks if employee creation requests works correctly through the API.
     * It creates a test company, sends valid employee information request, and checks
     * that status 201 is returned with the expected employee and user data.
     * Also confirms password information is not exposed in the response.
     */
    @Test
    void createEmployeeReturnsCreatedEmployeeAndUserAccount() throws Exception {
        Company testCompany = createCompanyTest();
        String employeeEmail = "controller-employee-" + UUID.randomUUID() + "@labortrack.test";

        // valid dto request
        EmployeeCreationRequest request = new EmployeeCreationRequest(
                "Yonelvyn",
                "Morel",
                "973-555-0100",
                new BigDecimal("27.50"),
                LocalDate.of(2026, 7, 21),
                "https://example.com/images/yonelvyn-morel.jpg",
                employeeEmail
        );

        mockMvc.perform(post("/api/companies/{companyId}/employees", testCompany.getId())
                        // request passes through authentication and authorization before DTO validation.
                        .with(user(createAdminPrincipal(testCompany.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.employeeId").isNumber())
                .andExpect(jsonPath("$.companyId")
                        .value(testCompany.getId()))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value(employeeEmail))
                .andExpect(jsonPath("$.firstName").value("Yonelvyn"))
                .andExpect(jsonPath("$.lastName").value("Morel"))
                .andExpect(jsonPath("$.phone").value("973-555-0100"))
                .andExpect(jsonPath("$.hourlyRate").value(27.50))
                .andExpect(jsonPath("$.profileImageUrl")
                        .value("https://example.com/images/yonelvyn-morel.jpg"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.hireDate").value("2026-07-21"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    /**
     * Verifies that an authenticated admin cannot create an employee for
     * another company by changing the company ID in the URL. The request
     * must return 403 Forbidden before reaching the employee creation service.
     */
    @Test
    void createEmployeeReturnsForbiddenWhenAdminUsesAnotherCompanyId()
            throws Exception {

        Company authenticatedAdminCompany = createCompanyTest();
        Long requestedCompanyId = 999999L;

        String employeeEmail = "wrong-company-" + UUID.randomUUID() + "@labortrack.test";

        EmployeeCreationRequest request = new EmployeeCreationRequest(
                "Yonelvyn",
                "Morel",
                "973-555-0100",
                new BigDecimal("27.50"),
                LocalDate.of(2026, 7, 21),
                "https://example.com/images/yonelvyn-morel.jpg",
                employeeEmail
        );

        mockMvc.perform(post(
                        "/api/companies/{companyId}/employees",
                        requestedCompanyId
                )
                        // request passes through authentication and authorization before DTO validation.
                        .with(user(createAdminPrincipal(
                                authenticatedAdminCompany.getId()
                        )))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/999999/employees"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    // HELPER METHOD
    private Company createCompanyTest() {
        Company company = new Company();
        company.setName("Employee Controller Test Company");
        company.setEmail(
                "employee-controller-company-"
                        + UUID.randomUUID()
                        + "@labortrack.test"
        );
        company.setTimezone("America/New_York");

        return companyRepository.save(company);
    }
    private LaborTrackUserDetails createAdminPrincipal(Long companyId) {
        return new LaborTrackUserDetails(
                1000L,
                companyId,
                null,
                "admin-" + companyId + "@labortrack.test",
                "{noop}unused",
                UserRole.ADMIN,
                true,
                false
        );
    }
}
