package com.labortrack.labortrack_backend.auth.controller;

import com.labortrack.labortrack_backend.auth.dto.request.ChangePasswordRequest;
import com.labortrack.labortrack_backend.auth.dto.request.LoginRequest;
import com.labortrack.labortrack_backend.company.dto.request.CompanyRegistrationRequest;
import com.labortrack.labortrack_backend.employee.dto.request.EmployeeCreationRequest;
import com.labortrack.labortrack_backend.security.jwt.JwtProperties;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * This method-test verifies that company-level tenant isolation. An ADMIN beloging
     * to company A cannot create an employee inside company B, even though they are
     * both authenticated and have ADMIN_ROLE. Authentication and role authorization
     * succeed, but company ownership authorization must reject the request with
     * 403 forbidden.
     */
    @Test
    void adminCannotCreateEmployeeForAnotherCompany() throws Exception {
        String adminAEmail = "company-isolation-admin-a-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String adminAPassword = "CompanyIsolationAdminA123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest companyARegistrationRequest =
                new CompanyRegistrationRequest(
                        "Company Isolation A",
                        adminAEmail,
                        adminAPassword
                );

        // send that request through API and expect created status back and save results
        MvcResult companyARegistrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        companyARegistrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode companyARegistrationJson = objectMapper.readTree(
                companyARegistrationResult
                        .getResponse()
                        .getContentAsString()
        );

        // get companyId from results (company A)
        Long companyAId = companyARegistrationJson
                .get("companyId")
                .asLong();

        String adminBEmail = "company-isolation-admin-b-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String adminBPassword = "CompanyIsolationAdminB123$";

        // create (company B + adminUser B registration request)
        CompanyRegistrationRequest companyBRegistrationRequest =
                new CompanyRegistrationRequest(
                        "Company Isolation B",
                        adminBEmail,
                        adminBPassword
                );

        // send that request through API and expect created status back and save results
        MvcResult companyBRegistrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        companyBRegistrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode companyBRegistrationJson = objectMapper.readTree(
                companyBRegistrationResult
                        .getResponse()
                        .getContentAsString()
        );

        // get companyId from results (company B)
        Long companyBId = companyBRegistrationJson
                .get("companyId")
                .asLong();

        // confirm the test created two different companies
        if (companyAId.equals(companyBId)) {
            throw new IllegalStateException(
                    "Company A and Company B must have different IDs."
            );
        }

        // create an admin-login request (company A)
        LoginRequest adminALoginRequest =
                new LoginRequest(
                        adminAEmail,
                        adminAPassword
                );

        // send that request through API and expect OK status back and save results
        MvcResult adminALoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        adminALoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId")
                        .value(companyAId))
                .andExpect(jsonPath("$.role")
                        .value("ADMIN"))
                .andReturn();

        JsonNode adminALoginJson = objectMapper.readTree(
                adminALoginResult
                        .getResponse()
                        .getContentAsString()
        );

        // get admin-access-token from results (company A)
        String adminAAccessToken = adminALoginJson
                .get("accessToken")
                .asString();

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest companyBEmployeeRequest =
                new EmployeeCreationRequest(
                        "Other",
                        "Company",
                        "973-555-0120",
                        new BigDecimal("27.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        "company-b-employee-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        /*
            send that request through API using company A's access token but
            using company B's companyId. Tenant isolation must reject this.
            Expect forbidden status back because companyId pass as param and
            JWT must be the same authenticated user therefore, in this
            scenario the request must fail.
         */
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyBId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        companyBEmployeeRequest
                                ))
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status")
                        .value(403))
                .andExpect(jsonPath("$.error")
                        .value("Forbidden"))
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/"
                                + companyBId
                                + "/employees"
                ))
                .andExpect(jsonPath("$.employeeId")
                        .doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword")
                        .doesNotExist());
    }

    /**
     * This method-test verifies that an authenticated employee cannot access another
     * employee's work-session history, even when both employees are authenticated and
     * belong to the same company. We change the temp password of employee 1, so we can
     * perform normal endpoint like accessing work-session history but we receivded
     * forbidden back because we can't do that.
     */
    @Test
    void employeeCannotAccessAnotherEmployeesWorkSessionHistory() throws Exception {
        String adminEmail = "history-isolation-admin-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String adminPassword = "HistoryIsolationAdmin123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "History Isolation Test Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult registrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // get companyId from results
        Long companyId = registrationJson
                .get("companyId")
                .asLong();

        // create an admin-login request
        LoginRequest adminLoginRequest =
                new LoginRequest(
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect OK status back and save results
        MvcResult adminLoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        adminLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminLoginJson = objectMapper.readTree(
                adminLoginResult.getResponse().getContentAsString()
        );

        // get access-token from results
        String adminAccessToken = adminLoginJson
                .get("accessToken")
                .asString();

        String employeeAEmail = "history-employee-a-"
                        + UUID.randomUUID()
                        + "@labortrack.test";

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeARequest =
                new EmployeeCreationRequest(
                        "Employee",
                        "Alpha",
                        "973-555-0118",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        employeeAEmail
                );

        // send that request through API using JWT and expect created status back and save results
        MvcResult employeeACreationResult = mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeARequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode employeeACreationJson = objectMapper.readTree(
                employeeACreationResult
                        .getResponse()
                        .getContentAsString()
        );

        // get employeeId and temporaryPassword from results
        Long employeeAId = employeeACreationJson
                .get("employeeId")
                .asLong();
        String employeeATemporaryPassword =
                employeeACreationJson
                        .get("temporaryPassword")
                        .asString();

        String employeeBEmail = "history-employee-b-"
                        + UUID.randomUUID()
                        + "@labortrack.test";

        // create an (employee profile 2 + employee user 2 creation request)
        EmployeeCreationRequest employeeBRequest =
                new EmployeeCreationRequest(
                        "Employee",
                        "Beta",
                        "973-555-0119",
                        new BigDecimal("26.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        employeeBEmail
                );

        // send that request through API using adminJWT and expect created status back and save results
        MvcResult employeeBCreationResult = mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeBRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode employeeBCreationJson = objectMapper.readTree(
                employeeBCreationResult
                        .getResponse()
                        .getContentAsString()
        );

        // get employeeId (employee 2)
        Long employeeBId = employeeBCreationJson
                .get("employeeId")
                .asLong();

        // create an employee-login request (employee 1)
        LoginRequest employeeALoginRequest =
                new LoginRequest(
                        employeeAEmail,
                        employeeATemporaryPassword
                );

        // send that request through API and expect OK status back and save results
        MvcResult employeeALoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeALoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId")
                        .value(employeeAId))
                .andExpect(jsonPath("$.role")
                        .value("EMPLOYEE"))
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(true))
                .andReturn();

        JsonNode employeeALoginJson = objectMapper.readTree(
                employeeALoginResult
                        .getResponse()
                        .getContentAsString()
        );

        // get employee-access-token from results (employee 1)
        String employeeAAccessToken = employeeALoginJson
                .get("accessToken")
                .asString();

        // create a password-change request (employee 1)
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        employeeATemporaryPassword,
                        "EmployeeAlphaPassword123$"
                );

        // send that request through API using employee1-access-token and expect Ok status back and save results
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        changePasswordRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(false));

        /*
         * employee 1 is authenticated and has the correct EMPLOYEE role,
         * but cannot read employee 2's history. Send a request to read
         * employee 2's work-session history using employee 1's access token
         * and expect forbidden status back
         */
        mockMvc.perform(
                        get(
                                "/api/employees/{employeeId}/work-sessions",
                                employeeBId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAAccessToken
                                )
                )
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
                        "/api/employees/"
                                + employeeBId
                                + "/work-sessions"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    /**
     * This method-test verifies that an authenticated EMPLOYEE role cannot
     * access the employee-creation endpoint nor created an employee because
     * it requires the ADMIN role. The employee first changes the temporary
     * password so the request reaches role authorization instead of being
     * blocked by the password-change required filter.
     */
    @Test
    void employeeJwtReturnsForbiddenWhenCreatingAnotherEmployee() throws Exception {
        String adminEmail = "employee-role-admin-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String adminPassword = "EmployeeRoleAdmin123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Employee Role Test Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult registrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // get companyId from results
        Long companyId = registrationJson
                .get("companyId")
                .asLong();

        // create an admin-login request
        LoginRequest adminLoginRequest =
                new LoginRequest(
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect OK status back and save return
        MvcResult adminLoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        adminLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminLoginJson = objectMapper.readTree(
                adminLoginResult.getResponse().getContentAsString()
        );

        // get access-token from results
        String adminAccessToken = adminLoginJson
                .get("accessToken")
                .asString();

        String firstEmployeeEmail = "employee-role-first-"
                        + UUID.randomUUID()
                        + "@labortrack.test";

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest firstEmployeeRequest =
                new EmployeeCreationRequest(
                        "First",
                        "Employee",
                        "973-555-0116",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        firstEmployeeEmail
                );

        // send that request through API using JWT and expect created status back and saved results
        MvcResult employeeCreationResult = mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        firstEmployeeRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode employeeCreationJson = objectMapper.readTree(
                employeeCreationResult.getResponse().getContentAsString()
        );

        // get temporary-password from results
        String temporaryPassword = employeeCreationJson
                .get("temporaryPassword")
                .asString();

        // create an employee-login request
        LoginRequest employeeLoginRequest =
                new LoginRequest(
                        firstEmployeeEmail,
                        temporaryPassword
                );

        // send that request through API and expect OK status back and saved results
        MvcResult employeeLoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();

        JsonNode employeeLoginJson = objectMapper.readTree(
                employeeLoginResult.getResponse().getContentAsString()
        );

        // get employee-access-token from results
        String employeeAccessToken = employeeLoginJson
                .get("accessToken")
                .asString();

        // create a new password
        String newEmployeePassword = "EmployeeRolePassword123$";

        // create a password-change request
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        temporaryPassword,
                        newEmployeePassword
                );

        // send that request through API with JWT and expect Ok status back
        // change the temporary password so role authorization is tested.
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        changePasswordRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(false));

        // create an (employee profile 2 + employee user 2 creation request again)
        EmployeeCreationRequest secondEmployeeRequest =
                new EmployeeCreationRequest(
                        "Second",
                        "Employee",
                        "973-555-0117",
                        new BigDecimal("26.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        "employee-role-second-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        // send that request through API using employee-access-token and expect isForbidden status back
        // EMPLOYEE is authenticated but does not have ROLE_ADMIN.
        // ONLY ROLE_ADMIN can create employee
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        secondEmployeeRequest
                                ))
                )
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
                        "/api/companies/"
                                + companyId
                                + "/employees"
                ))
                .andExpect(jsonPath("$.employeeId")
                        .doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword")
                        .doesNotExist());
    }

    /**
     * This method-test verifies that an Authorization header containing
     * the Bearer prefix without an actual JWT cannot authenticate a
     * protected request.
     */
    @Test
    void protectedEndpointReturnsUnauthorizedWhenBearerTokenIsEmpty() throws Exception {
        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Empty",
                        "Token",
                        "973-555-0115",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        "empty-token-employee-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        /*
        Send that request through API with "Bearer " but not JWT and
        expect unauthorized because couldn't authenticate because
        bearer was present but not JWT that follows it.
         */
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                999999L
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer "
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeRequest
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value(
                        "Unauthorized"
                ))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/999999/employees"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    /**
     * This method-test verifies that a correctly signed JWT cannot
     * authenticate after its expiration time has passed. The token
     * has a valid structure and signature, but only its expiration
     * time claim is already in the past. We create a JWT from
     * JwtProperties and modified the expiration time and send
     * that request and expect unauthorized status back because
     * expire token cannot authenticate.
     */
    @Test
    void protectedEndpointReturnsUnauthorizedWhenJwtIsExpired() throws Exception {
        String adminEmail = "expired-token-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminPassword = "ExpiredTokenPassword123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Expired Token Test Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult registrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // get CompanyId from results
        Long companyId = registrationJson
                .get("companyId")
                .asLong();

        /*
         * Build a token using the application's real signing secret.
         * Its issued-at time and expiration time are both in the past.
         */
        Instant now = Instant.now();

        SecretKey signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        jwtProperties.secret()
                )
        );

        String expiredAccessToken = Jwts.builder()
                .subject(adminEmail)
                .issuedAt(Date.from(
                        now.minusSeconds(7200)
                ))
                .expiration(Date.from(
                        now.minusSeconds(3600)
                ))
                .signWith(signingKey)
                .compact();

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Expired",
                        "Token",
                        "973-555-0114",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        "expired-token-employee-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        // send that request through API with an expired JWT and expect
        // unauthorized status back
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + expiredAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeRequest
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value(
                        "Unauthorized"
                ))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/"
                                + companyId
                                + "/employees"
                ))
                .andExpect(jsonPath("$.employeeId")
                        .doesNotExist());
    }

    /**
     * This method-test verifies the complete first-login password-change flow.
     * An employee receives a temporary password and can log in, but normal endpoints
     * like clock-in, clock-out, etc. will remain blocked while mustChangePassword field
     * is true. After changing the password, the same JWT can access the employee
     * clock-in endpoint and expect success.
     */
    @Test
    void employeeMustChangeTemporaryPasswordBeforeAccessingClockIn() throws Exception {

        String adminEmail = "password-flow-admin-" + UUID.randomUUID() + "@labortrack.test";
        String adminPassword = "AdminPassword123$";

        // create a request (company + adminuser registration)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Password Flow Test Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect is created status back and saved return
        MvcResult registrationResult = mockMvc.perform(
                post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registrationRequest
                        ))
        )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // get companyId from that result
        Long companyId = registrationJson.get("companyId").asLong();

        // create a ADMIN-login request
        LoginRequest adminLoginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // send that request through API and expect OK status back and saved return
        MvcResult adminLoginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                adminLoginRequest
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(false))
                .andReturn();

        JsonNode adminLoginJson = objectMapper.readTree(
                adminLoginResult.getResponse().getContentAsString()
        );

        // get access token (JWT) from the result
        String adminAccessToken =
                adminLoginJson.get("accessToken").asString();

        String employeeEmail =
                "password-flow-employee-"
                        + UUID.randomUUID()
                        + "@labortrack.test";

        // create a (employee profile + employee user request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Password",
                        "Employee",
                        "973-555-0105",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 7, 31),
                        null,
                        employeeEmail
                );

        // send that request through API and expect created status back and saved return
        MvcResult employeeCreationResult = mockMvc.perform(
                post("/api/companies/{companyId}/employees", companyId)
                        .header(
                                AUTHORIZATION,
                                "Bearer " + adminAccessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeRequest
                        ))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andReturn();

        JsonNode employeeCreationJson = objectMapper.readTree(
                employeeCreationResult.getResponse().getContentAsString()
        );

        // get the employeeId and temporaryPassword from that return
        Long employeeId = employeeCreationJson
                .get("employeeId").asLong();
        String temporaryPassword =
                employeeCreationJson
                        .get("temporaryPassword").asString();

        // create an employee-login request (using temporary password)
        LoginRequest employeeLoginRequest = new LoginRequest(
                employeeEmail,
                temporaryPassword
        );

        // send that request through API and expect OK status back and saved return
        MvcResult employeeLoginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeLoginRequest
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role")
                        .value("EMPLOYEE"))
                .andExpect(jsonPath("$.employeeId")
                        .value(employeeId))
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(true))
                .andReturn();

        JsonNode employeeLoginJson = objectMapper.readTree(
                employeeLoginResult.getResponse().getContentAsString()
        );

        // get the access token from the result
        String employeeAccessToken = employeeLoginJson
                .get("accessToken").asString();

        // perform clock-in to the employee created using JWT and expect forbidden status back
        // because employee hasn't changed its initial temporary password yet
        mockMvc.perform(
                        post("/api/employees/{employeeId}/clock-in", employeeId)
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAccessToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(
                        "Password change required."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/employees/"
                                + employeeId
                                + "/clock-in"
                ));

        String newPassword = "EmployeeNewPassword123$";

        // create a change password requests
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        temporaryPassword,
                        newPassword
                );

        // send that request through API and expect OK status back
        mockMvc.perform(
                post("/api/auth/change-password")
                        .header(
                                AUTHORIZATION,
                                "Bearer " + employeeAccessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                changePasswordRequest
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Password changed successfully."
                ))
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(false));

        // perform clock-in again to the created, authenticated employee and
        // expect created status back and successful message because
        // employee is authenticated and has changed its initial temporary password
        // before performing any action.
        mockMvc.perform(
                post("/api/employees/{employeeId}/clock-in", employeeId)
                        .header(
                                AUTHORIZATION,
                                "Bearer " + employeeAccessToken
                        )
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId")
                        .value(employeeId))
                .andExpect(jsonPath("$.status")
                        .value("OPEN"));
    }

    /**
     * This method-test verifies that changing a password fails when the pass in current
     * password does not match the stored password hash therefore password change should
     * not happen and expect unauthorized status.
     */
    @Test
    void changePasswordReturnsUnauthorizedWhenCurrentPasswordIsIncorrect() throws Exception {

        String adminEmail = "wrong-current-password-" + UUID.randomUUID() + "@labortrack.test";
        String correctPassword = "CorrectPassword123$";

        // create a (company + adminUser creation request)
        CompanyRegistrationRequest registrationRequest = new CompanyRegistrationRequest(
                "Wrong Current Password Company",
                adminEmail,
                correctPassword
        );

        // send that request through API and expect is created status back
        mockMvc.perform(
                post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registrationRequest
                        ))
        )
                .andExpect(status().isCreated());

        // create an admin-login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                correctPassword
        );

        // send that request through API, expect OK status back and saved return
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest
                        ))
        )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get the access-token from that result
        String accessToken = loginJson
                .get("accessToken").asString();

        // create a password change request but passing a wrong current password
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
                "WrongCurrentPassword123$",
                "DifferentNewPassword123$"
        );

        // send that request through API, expect unauthorized status back because
        // current password submitted does not match the original stored hash password
        mockMvc.perform(
                post("/api/auth/change-password")
                        .header(
                                AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                changePasswordRequest
                        ))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value(
                        "/api/auth/change-password"
                ));

        // confirm the original password wasn't change
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest
                        ))
        )
                .andExpect(status().isOk());
    }

    /**
     * This method-test verifies that a user cannot change their password to the
     * same password they currently use. That is invalid, and we must expect
     * a bad request stating that you must use a different password.
     */
    @Test
    void changePasswordReturnsBadRequestWhenNewPasswordMatchesCurrentPassword()
            throws Exception {

        String adminEmail = "same-password-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String currentPassword = "CurrentPassword123$";

        // create a (company + adminUser register request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Same Password Test Company",
                        adminEmail,
                        currentPassword
                );

        // send that request through API and expect created status back
        mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated());

        // create an admin-login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                currentPassword
        );

        // send that request through API and expect OK status back and saved result
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get the access-token from that result
        String accessToken =
                loginJson.get("accessToken").asString();

        // create a password-change request but also passing current password as new password (can't happen)
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        currentPassword,
                        currentPassword
                );

        // send that request through API and expect bad request status back
        // because you cannot update to the same password
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        changePasswordRequest
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "New password must be different from old password."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/auth/change-password"
                ));

        // Confirm the current password still works
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk());
    }

    /**
     * This method-test verifies that password-change validation rejects a new
     * password containing fewer than 12 characters before reaching the service.
     * So you must expect a bad request and message stating that password must
     * be 12 or greater characters long.
     */
    @Test
    void changePasswordReturnsBadRequestWhenNewPasswordIsTooShort()
            throws Exception {

        String adminEmail = "short-new-password-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String currentPassword = "CurrentPassword123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Short Password Test Company",
                        adminEmail,
                        currentPassword
                );

        // send that request through API and expect created status back
        mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated());

        // create an admin-login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                currentPassword
        );

        // send that request through API and expect OK status back and saved result
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get the access-token from the result
        String accessToken =
                loginJson.get("accessToken").asString();

        // create a password-change request but with new
        // password being too short (<12)
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        currentPassword,
                        "Short123!"
                );

        // send that request through API and expect bad request status back
        // because password must be 12 or more characters long
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        changePasswordRequest
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value(
                        "/api/auth/change-password"
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.newPassword"
                ).value(
                        "New password must be between 12 and 64 characters."
                ));

        // Confirm validation failure did not change the password.
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk());
    }

    /**
     * This method-test verifies that password-change endpoint requires
     * authentication. A requests without a JWT must be rejected before
     * reaching the controller. Therefore, you must expect unauthorized
     * status back and message stating you need to authentication to
     * access this endpoint.
     */
    @Test
    void changePasswordReturnsUnauthorizedWhenJwtIsMissing() throws Exception {

        // create a change-password request
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        "CurrentPassword123$",
                        "DifferentPassword123$"
                );

        // send that request through API and expect unauthorized status back
        // because you didn't included authentication information like JWT
        mockMvc.perform(
                post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                changePasswordRequest
                        ))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/auth/change-password"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.mustChangePassword")
                        .doesNotExist());
    }

    /**
     * This method-test verifies that a successful password change replaces the old
     * password. The old password must stop authenticating, while the password works.
     * So expect unauthorized status back when using oldPassword after password-change
     * and expect OK status when using new password to authenticate.
     */
    @Test
    void changePasswordInvalidatesOldPasswordAndAllowsNewPassword() throws Exception {
        String adminEmail = "password-replacement-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String oldPassword = "OriginalPassword123$";
        String newPassword = "ReplacementPassword123$";

        // create a (company + adminUser registration request using oldPassword)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Password Replacement Company",
                        adminEmail,
                        oldPassword
                );

        // send that request through API and expect created status back
        mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated());

        // create admin-login request using oldPassword
        LoginRequest oldPasswordLoginRequest =
                new LoginRequest(
                        adminEmail,
                        oldPassword
                );

        // send that request through API and expect OK status back and saved results
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        oldPasswordLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get access-token from the result
        String accessToken =
                loginJson.get("accessToken").asString();

        // create a password-change request oldPassword-to-newPassword
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        oldPassword,
                        newPassword
                );

        // send that requests through API and expect OK status back
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        changePasswordRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Password changed successfully."
                ))
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(false));

        // Perform login using oldPassword. The old password must no longer authenticate.
        // expect an unauthorized status back.
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        oldPasswordLoginRequest
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.accessToken")
                        .doesNotExist());

        // create a login-request but with newPassword
        LoginRequest newPasswordLoginRequest =
                new LoginRequest(
                        adminEmail,
                        newPassword
                );

        // send that requests through API and expect OK status.
        // The new password must authenticate successfully.
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        newPasswordLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.email")
                        .value(adminEmail))
                .andExpect(jsonPath("$.role")
                        .value("ADMIN"))
                .andExpect(jsonPath("$.mustChangePassword")
                        .value(false));
    }

    /**
     * This method-test verifies that a disabled user cannot authenticated/
     * login even when the submitted email and password are correct. This
     * ensures that disabled accounts cannot access endpoints even if they
     * can authenticate. So we create a user, save it. Then load it again and
     * directly disabled it, then perform login back again, and we must expect
     * unauthorized status back.
     */
    @Test
    void loginReturnsUnauthorizedWhenUserIsDisabled() throws Exception {
        String adminEmail = "diabled-login-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminPassword = "DisabledUserPassword123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Disabled Login Test Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect created status back
        mockMvc.perform(
                post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registrationRequest
                        ))
        )
                .andExpect(status().isCreated());

        // load the user from db and disable it directly for this security test
        User savedAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow();
        savedAdmin.setEnabled(false);
        userRepository.saveAndFlush(savedAdmin); // save changes

        // create a login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // send that request through API and expect unauthorized status back
        // because a user with field disabled=true cannot log in even thought
        // credentials are correct just because account is disabled.
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest
                        ))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/auth/login"
                ))
                .andExpect(jsonPath("$.accessToken")
                        .doesNotExist());
    }

    /**
     * This method-test verifies that disabling a user invalidates practical
     * access from an already-issued JWT. The JWT may still have a valid
     * signature and expiration but the authentication filter must reject
     * because the account is disabled.
     */
    @Test
    void existingJwtReturnsUnauthorizedAfterUserIsDisabled() throws Exception {
        String adminEmail =
                "disabled-existing-jwt-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String adminPassword = "ExistingJwtPassword123$";

        // create (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Disabled Existing JWT Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult registrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // get companyId from results
        Long companyId = registrationJson
                .get("companyId")
                .asLong();

        // create a login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // send that request through API and expect ok status back and saved results
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get access-token from results
        String accessToken = loginJson
                .get("accessToken")
                .asString();

        // disable the account after its JWT has already been issued.
        User savedAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow();

        savedAdmin.setEnabled(false);
        userRepository.saveAndFlush(savedAdmin);

        // create (employee profile + employee user request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Disabled",
                        "JWT",
                        "973-555-0110",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        "disabled-jwt-employee-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        // send that request through API and expect unauthorized status back
        // because user account is disabling, so endpoints access can't be granted
        // the previously issued JWT must no longer grant access.
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeRequest
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/"
                                + companyId
                                + "/employees"
                ))
                .andExpect(jsonPath("$.employeeId")
                        .doesNotExist());
    }

    /**
     * This method-test verifies that a malformed JWT cannot authenticate a
     * protected request. The API must return a 401-unauthorized status back
     * rather than exposing a token-parsing exception or a server error.
     */
    @Test
    void protectedEndpointReturnsUnauthorizedWhenJwtIsMalformed() throws Exception {

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest = new EmployeeCreationRequest(
                "Malformed",
                "Token",
                "973-555-0111",
                new BigDecimal("25.00"),
                LocalDate.of(2026, 8, 1),
                null,
                "malformed-token-employee-" + UUID.randomUUID() + "@labortrack.test"
        );

        // send that request through API and expect unauthorized status back
        mockMvc.perform(
                post("/api/companies/{companyId}/employees", 999999L)
                        .header(
                                AUTHORIZATION,
                                "Bearer not-valid-jwt-malformed"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeRequest
                        ))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/999999/employees"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    /**
     * This method-test verifies that a structurally valid JWT with only an
     * altered signature cannot authenticate a protected request because
     * signature is invalid. Right here we create a company, login and
     * get JWT. The took the signature part of the JWT and slightly alter
     * it, the compose the JWT back again with only the signature slightly
     * altered and send a request with that JWT and expect authorization
     * status back because JWT is structurally correct but signature is
     * invalid.
     */
    @Test
    void protectedEndpointReturnsUnauthorizedWhenJwtSignatureIsInvalid()  throws Exception {
        String adminEmail =
                "invalid-signature-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String adminPassword = "InvalidSignaturePassword123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Invalid Signature Test Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expects created status back and saved results
        MvcResult registrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // get companyId from results
        Long companyId = registrationJson
                .get("companyId")
                .asLong();

        // create an admin-login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // send that request through API and expect OK status back and saved results
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get access-token from results
        String validAccessToken = loginJson
                .get("accessToken")
                .asString();

        // JWT structure:
        // header.payload.signature
        String[] tokenParts = validAccessToken.split("\\.");

        String signature = tokenParts[2];

        // Change the first signature character while keeping the JWT structure valid.
        char replacementCharacter =
                signature.charAt(0) == 'A'
                        ? 'B'
                        : 'A';

        String alteredSignature =
                replacementCharacter + signature.substring(1);

        String invalidSignatureToken =
                tokenParts[0]
                        + "."
                        + tokenParts[1]
                        + "."
                        + alteredSignature;

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Invalid",
                        "Signature",
                        "973-555-0112",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        "invalid-signature-employee-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        // send that request through the API passing JWT, everything correct
        // but the signature is slightly alter so expect unauthorized because
        // signature is invalid
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + invalidSignatureToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeRequest
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/"
                                + companyId
                                + "/employees"
                ))
                .andExpect(jsonPath("$.employeeId")
                        .doesNotExist());
    }

    /**
     * This method-test verifies that a valid JWT cannot authenticate a request when
     * the authentication header is missing the required "Bearer " prefix. The JWT
     * may be valid but without the prefix expect an unauthorized status back and
     * unabled to authenticated.
     */
    @Test
    void protectedEndpointReturnsUnauthorizedWhenBearerPrefixIsMissing() throws Exception {

        String adminEmail = "missing-bearer-prefix-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminPassword = "MissingBearerPassword123$";

        // create (company + adminUser registratio request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Missing Bearer Prefix Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult registrationResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        registrationRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // get companyId from the results
        Long companyId = registrationJson
                .get("companyId")
                .asLong();

        // create an admin-login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // send that request through API and expect OK status back and saved results
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get access-token from results
        String validAccessToken = loginJson
                .get("accessToken")
                .asString();

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Missing",
                        "Bearer",
                        "973-555-0113",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 1),
                        null,
                        "missing-bearer-employee-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        // sent that request through API with JWT but not "Bearer " prefix
        // in it and expect unauthorized status back and enabled to authenticate
        // because missing "Bearer " despite valid JWT.
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        validAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeRequest
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/"
                                + companyId
                                + "/employees"
                ))
                .andExpect(jsonPath("$.employeeId")
                        .doesNotExist());
    }

    /**
     * This method verifies the complete admin-login flow. The test first
     * registers a company + adminUser so the password is stored using the
     * password encoder. Then it perform a login request using the admin
     * user information, sending original credentials, then verifies that
     * a signed JWT and the correct authenticated user gets returned.
     */
    @Test
    void loginReturnsJwtWhenAdminCredentialsAreValid() throws Exception {
        String adminEmail = "login-admin-" + UUID.randomUUID() + "@labortrack.test";
        String adminPassword = "StringPassword123$";

        CompanyRegistrationRequest request = new CompanyRegistrationRequest(
                "Authentication Test Company",
                adminEmail,
                adminPassword
        );

        // register a company and admin user first
        mockMvc.perform(
                post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isCreated());

        // load the saved user, so id can be confirmed
        User savedAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow();

        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // now perform login, you must expect a jwt token back.
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.userId")
                        .value(savedAdmin.getId()))
                .andExpect(jsonPath("$.companyId")
                        .value(savedAdmin.getCompany().getId()))
                .andExpect(jsonPath("$.employeeId").isEmpty())
                .andExpect(jsonPath("$.email").value(adminEmail))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    /**
     * This method verifies that login is rejected when submitting an incorrect
     * password, because it does not match the stored password hash. The API must
     * return a 401-unauthorized custom error response back and should not generate
     * or expose a signed JWT at all.
     */
    @Test
    void loginReturnsUnauthorizedWhenPasswordIsIncorrect() throws Exception {

        String adminEmail = "wrong-password-admin-" + UUID.randomUUID() + "@labortrack.test";
        String correctPassword = "StrongPassword123$";

        // create a (register company + adminUser request)
        CompanyRegistrationRequest request = new CompanyRegistrationRequest(
                "Wrong Password Test Company",
                adminEmail,
                correctPassword
        );

        // send that request and expect created back
        mockMvc.perform(
                post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        ))
                        ).andExpect(status().isCreated());

        // then create a login requests, but using an incorrect password
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                "IncorrectPassword123$"
        );

        // send that requests and expect unauthorized-401 back because password don't match
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    /**
     * This method verifies the complete JWT request flow. The test register a
     * company + adminUser, send that request through API and from the response
     * extract the companyId. Then create a login request using credentials and
     * send it through the API, from the response extract the access-token
     * (JWT). Then create an employee creation requests and send it through the
     * API using the access token as authentication request and companyId and
     * expect created status back.
     */
    @Test
    void adminJwtAllowsCreatingEmployeeForOwnCompany() throws Exception {
        String adminEmail = "jwt-admin-" + UUID.randomUUID() + "@labortrack.test";
        String adminPassword = "StringPassword123$";

        // create a (register company + adminUser request)
        CompanyRegistrationRequest request = new CompanyRegistrationRequest(
                "JWT Integration Test Company",
                adminEmail,
                adminPassword
        );

        // send that request through API and expect created back. Store return
        MvcResult registrationResult = mockMvc.perform(
                post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        ))
        ).andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        // store registrationResult return companyId
        Long companyId = registrationJson.get("companyId").asLong();

        // create a login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // send that request through API and expect OK. Store return
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest
                        ))
        ).andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // from loginResult return obtain the access token or JWT
        String accessToken = loginJson
                .get("accessToken").asString();

        String employeeEmail =
                "jwt-employee-"
                        + UUID.randomUUID()
                        + "@labortrack.test";

        // create an employee creation request
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "JWT",
                        "Employee",
                        "973-555-0100",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 7, 29),
                        null,
                        employeeEmail
                );

        // send that request through API using access token (JWT) as authentication and expect created status back.
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andExpect(jsonPath("$.email").value(employeeEmail))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    /**
     * This method verifies that a protected endpoint rejects a requests when
     * no JWT is included in the authorization header. The request must return
     * custom error response dto with 401-unauthorized status and message and
     * must not reach the controller.
     */
    @Test
    void protectedEndpointReturnsUnauthorizedWhenJwtIsMissing() throws Exception {
        // create an employee creation request
        EmployeeCreationRequest employeeRequest = new EmployeeCreationRequest(
                "Unauthorized",
                "Employee",
                "973-555-0100",
                new BigDecimal("25.00"),
                LocalDate.of(2026, 7, 29),
                null,
                "unauthorized-" + UUID.randomUUID() +  "@labortrack.test"
        );

        // send that requests through API and expect unauthorized status because JWT is missing
        mockMvc.perform(
                post(
                        "/api/companies/{companyId}/employees",
                        999999L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeRequest
                        ))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/999999/employees"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    /**
     * This method verifies that a valid ADMIN JWT cannot access an endpoint restricted
     * to the EMPLOYEE role like clock-in can only be performed by employee. Authentication
     * succeeds because the JWT is valid, but authorization fails because ADMIN does
     * not have ROLE_EMPLOYEE.
     */
    @Test
    void adminJwtReturnsForbiddenWhenAccessingEmployeeClockInEndpoint() throws Exception {
        String adminEmail = "wrong-role-admin-" + UUID.randomUUID() + "@labortrack.test";
        String adminPassword = "StringPassword123$";

        // create a (register company + adminUser request)
        CompanyRegistrationRequest request = new CompanyRegistrationRequest(
                "Wrong Role Test Company",
                adminEmail,
                adminPassword
        );

        // send that requests through the API and expect created status back.
        mockMvc.perform(
                post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        ))
        ).andExpect(status().isCreated());

        // create a login request
        LoginRequest loginRequest = new LoginRequest(
                adminEmail,
                adminPassword
        );

        // send that request through the API and expect OK status and save the response
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest
                        ))
        )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get the access token from the response (JWT)
        String accessToken = loginJson
                .get("accessToken").asString();

        // use the access token to send a requests and expects 403-forbidden status because
        // the endpoint is restricted only allow EMPLOYEE roles to access even if the user
        // is authenticated, and it is a ADMIN
        mockMvc.perform(
                post("/api/employees/{employeeId}/clock-in", 999999L)
                        .header(
                                AUTHORIZATION,
                                "Bearer " + accessToken
                        )
        )
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
                        "/api/employees/999999/clock-in"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }
}
