package com.labortrack.labortrack_backend.dashboard.controller;

import com.labortrack.labortrack_backend.auth.dto.request.ChangePasswordRequest;
import com.labortrack.labortrack_backend.auth.dto.request.LoginRequest;
import com.labortrack.labortrack_backend.company.dto.request.CompanyRegistrationRequest;
import com.labortrack.labortrack_backend.employee.dto.request.EmployeeCreationRequest;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This test class basically tests that dashboard endpoints
 * return correct data and enforce roles and company isolation
 * protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    EmployeeRepository employeeRepository;

    /**
     * This method-test verifies that the admin employee-list endpoint
     * can filter employees by ACTIVE/INACTIVE status while remaining
     * restricted to the authenticated company. Only will show company's
     * employees.
     */
    @Test
    void adminEmployeeListCanFilterByStatus() throws Exception {

        String adminEmail = "status-filter-admin-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminPassword = "StatusFilterAdmin123$";

        // create (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Status Filter Company",
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

        // get companyId from results
        Long companyId = objectMapper.readTree(
                        registrationResult.getResponse().getContentAsString()
                )
                .get("companyId")
                .asLong();

        // create admin-login request
        LoginRequest loginRequest =
                new LoginRequest(
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

        // get admin-access-token from results
        String adminAccessToken = objectMapper.readTree(
                        loginResult.getResponse().getContentAsString()
                )
                .get("accessToken")
                .asString();

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest activeEmployeeRequest =
                new EmployeeCreationRequest(
                        "Active",
                        "Employee",
                        "973-555-0207",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        "active-filter-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        // send that request through API using admin's JWT
        // and expect created status back and saved results
        MvcResult activeEmployeeResult = mockMvc.perform(
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
                                        activeEmployeeRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        // get employeeId from results
        Long activeEmployeeId = objectMapper.readTree(
                        activeEmployeeResult.getResponse().getContentAsString()
                )
                .get("employeeId")
                .asLong();

        // create another (employee profile + employee user creation request)
        EmployeeCreationRequest inactiveEmployeeRequest =
                new EmployeeCreationRequest(
                        "Inactive",
                        "Employee",
                        "973-555-0208",
                        new BigDecimal("26.00"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        "inactive-filter-"
                                + UUID.randomUUID()
                                + "@labortrack.test"
                );

        // send that request through API using admin's JWT
        // and expect created status back and saved results
        MvcResult inactiveEmployeeResult = mockMvc.perform(
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
                                        inactiveEmployeeRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        // get employeeId from results
        Long inactiveEmployeeId = objectMapper.readTree(
                        inactiveEmployeeResult.getResponse().getContentAsString()
                )
                .get("employeeId")
                .asLong();

        // change the second employee's status to INACTIVE.
        Employee inactiveEmployee = employeeRepository
                .findById(inactiveEmployeeId)
                .orElseThrow();
        inactiveEmployee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.saveAndFlush(inactiveEmployee);

        // then perform a request to retrieve company employees
        // using filters as ACTIVE so only retrieve active employees from the company
        // expect first employee to show and second should not show because it is INACTIVE status
        mockMvc.perform(
                        get("/api/admin/employees")
                                .param("status", "ACTIVE")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(activeEmployeeId))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        // do another similar request but filter now to INACTIVE
        // so show all INACTIVE employees from the company
        // second employee must show up
        mockMvc.perform(
                        get("/api/admin/employees")
                                .param("status", "INACTIVE")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(inactiveEmployeeId))
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));
    }

    /**
     * This method-test verifies that an admin can retrieve the currently
     * open work sessions belonging to their company.
     */
    @Test
    void adminCanSeeCurrentlyClockedInEmployees() throws Exception {

        String adminEmail = "open-session-admin-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminPassword = "OpenSessionAdmin123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Open Session Test Company",
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

        // send that request through API and expect OK status back and saved results
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

        // get admin-access-token from results
        String adminAccessToken = adminLoginJson
                .get("accessToken")
                .asString();

        String employeeEmail = "open-session-employee-"
                + UUID.randomUUID()
                + "@labortrack.test";

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Clocked",
                        "In",
                        "973-555-0206",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        employeeEmail
                );

        // send that request through API using admin's JWT
        // and expect created status back and saved results
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
                                        employeeRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode employeeCreationJson = objectMapper.readTree(
                employeeCreationResult.getResponse().getContentAsString()
        );

        // get employeeId and temporaryPassword from results
        Long employeeId = employeeCreationJson
                .get("employeeId")
                .asLong();
        String temporaryPassword = employeeCreationJson
                .get("temporaryPassword")
                .asString();

        // create employee-login request
        LoginRequest employeeLoginRequest =
                new LoginRequest(
                        employeeEmail,
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
                .andReturn();

        JsonNode employeeLoginJson = objectMapper.readTree(
                employeeLoginResult.getResponse().getContentAsString()
        );

        // get employee-access-token from results
        String employeeAccessToken = employeeLoginJson
                .get("accessToken")
                .asString();

        // create employee-password-change request
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        temporaryPassword,
                        "OpenSessionEmployee123$"
                );

        // send that request through API using employee's JWT adn expect OK status back
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
                .andExpect(status().isOk());

        // clock-in the employee and expect created status back
        mockMvc.perform(
                        post(
                                "/api/employees/{employeeId}/clock-in",
                                employeeId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAccessToken
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        // admin requests the company's currently open sessions and expect
        // the employee open session there
        mockMvc.perform(
                        get("/api/admin/work-sessions/open")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(employeeId))
                .andExpect(jsonPath("$[0].employeeName").value("Clocked In"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].clockInTime").isNotEmpty())
                .andExpect(jsonPath("$[0].clockOutTime").isEmpty())
                .andExpect(jsonPath("$[0].workedMinutes").isNumber());
    }

    /**
     * This method-test verifies that an authenticated admin can access
     * their company dashboard and receives the correct employee and
     * work-session totals.
     */
    @Test
    void adminCanAccessOwnDashboard() throws Exception {

        String adminEmail = "admin-dashboard-success-"
                + UUID.randomUUID()
                + "@labortrack.test";

        String adminPassword = "AdminDashboardSuccess123$";

        // create (company + adminUser request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Admin Dashboard Success Company",
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

        // create admin-login request
        LoginRequest loginRequest =
                new LoginRequest(
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect OK status back and save results
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

        // get admin-access-token from results
        String adminAccessToken = loginJson
                .get("accessToken")
                .asString();

        String employeeEmail = "admin-dashboard-employee-"
                + UUID.randomUUID()
                + "@labortrack.test";

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Admin",
                        "Dashboard",
                        "973-555-0205",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        employeeEmail
                );

        // send that request through API using admin's JWT and expect created status back
        mockMvc.perform(
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
                                        employeeRequest
                                ))
                )
                .andExpect(status().isCreated());

        // request the authenticated admin's company dashboard using admin's JWT
        mockMvc.perform(
                        get("/api/admin/dashboard")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.totalEmployees").value(1))
                .andExpect(jsonPath("$.activeEmployees").value(1))
                .andExpect(jsonPath("$.inactiveEmployees").value(0))
                .andExpect(jsonPath("$.currentlyClockedInEmployees").value(0))
                .andExpect(jsonPath("$.todayWorkedMinutes").value(0))
                .andExpect(jsonPath("$.recentWorkSessions").isArray())
                .andExpect(jsonPath("$.recentWorkSessions").isEmpty());
    }

    /**
     * This method-test verifies that an authenticated employee can
     * access their own dashboard information. The employeeId comes
     * from the authenticated JWT rather than from a request param
     * or body.
     */
    @Test
    void employeeCanAccessOwnDashboard() throws Exception {

        String adminEmail = "self-dashboard-admin-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminPassword = "SelfDashboardAdmin123$";

        // create (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Employee Self Dashboard Company",
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

        // send that request through API and expect OK status back and saved results
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

        // get accessToken from results
        String adminAccessToken = adminLoginJson
                .get("accessToken")
                .asString();

        String employeeEmail = "self-dashboard-employee-"
                + UUID.randomUUID()
                + "@labortrack.test";

        // create an (employee profile + employee user creation requests)
        EmployeeCreationRequest employeeRequest =
                new EmployeeCreationRequest(
                        "Self",
                        "Dashboard",
                        "973-555-0204",
                        new BigDecimal("26.50"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        employeeEmail
                );

        // send that request through API using company's JWT
        // and expect created status back and saved results
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
                                        employeeRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode employeeCreationJson = objectMapper.readTree(
                employeeCreationResult.getResponse().getContentAsString()
        );

        // get employeeId and temporaryPassword from results
        Long employeeId = employeeCreationJson
                .get("employeeId")
                .asLong();
        String temporaryPassword = employeeCreationJson
                .get("temporaryPassword")
                .asString();

        // create employee-login request
        LoginRequest employeeLoginRequest =
                new LoginRequest(
                        employeeEmail,
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
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andReturn();

        JsonNode employeeLoginJson = objectMapper.readTree(
                employeeLoginResult.getResponse().getContentAsString()
        );

        // get employee-access-token from results
        String employeeAccessToken = employeeLoginJson
                .get("accessToken")
                .asString();

        // create employee-password-change request
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        temporaryPassword,
                        "SelfDashboardEmployee123$"
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
                .andExpect(status().isOk());

        // request employee's dashboard using employee's JWT and expect OK status back
        mockMvc.perform(
                        get("/api/employee/me/dashboard")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.firstName").value("Self"))
                .andExpect(jsonPath("$.lastName").value("Dashboard"))
                .andExpect(jsonPath("$.email").value(employeeEmail))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentlyClockedIn").value(false))
                .andExpect(jsonPath("$.currentOpenSession").isEmpty())
                .andExpect(jsonPath("$.todayWorkedMinutes").value(0))
                .andExpect(jsonPath("$.recentWorkSessions").isArray())
                .andExpect(jsonPath("$.recentWorkSessions").isEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist());
    }

    /**
     * This method-test verifies an authenticated admin cannot retrieve
     * employee details for an employee belonging to another company.
     * The API would return 404 not found instead of revealing that
     * the other company's employee exists.
     */
    @Test
    void adminCannotReadAnotherCompanyEmployee() throws Exception {

        // ---------- Company A ----------
        String adminAEmail = "detail-admin-a-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminAPassword = "DetailAdminAPassword123$";

        // create a (company A + adminUser A registration request)
        CompanyRegistrationRequest companyARequest =
                new CompanyRegistrationRequest(
                        "Employee Detail Company A",
                        adminAEmail,
                        adminAPassword
                );

        // send that request through API and expect created status back
        mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        companyARequest
                                ))
                )
                .andExpect(status().isCreated());

        // create admin-login request (company A)
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
                .andReturn();

        JsonNode adminALoginJson = objectMapper.readTree(
                adminALoginResult.getResponse().getContentAsString()
        );

        // get access-token from results (company A)
        String adminAAccessToken = adminALoginJson
                .get("accessToken")
                .asString();

        // ---------- Company B ----------

        String adminBEmail = "detail-admin-b-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminBPassword = "DetailAdminBPassword123$";

        // create a (company B + adminUser B registration request)
        CompanyRegistrationRequest companyBRequest =
                new CompanyRegistrationRequest(
                        "Employee Detail Company B",
                        adminBEmail,
                        adminBPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult companyBResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        companyBRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode companyBJson = objectMapper.readTree(
                companyBResult.getResponse().getContentAsString()
        );

        // get companyId from results (company B)
        Long companyBId = companyBJson
                .get("companyId")
                .asLong();

        // create admin-login request (company B)
        LoginRequest adminBLoginRequest =
                new LoginRequest(
                        adminBEmail,
                        adminBPassword
                );

        // send that request through API and expect OK status back and saved results
        MvcResult adminBLoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        adminBLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminBLoginJson = objectMapper.readTree(
                adminBLoginResult.getResponse().getContentAsString()
        );

        // get accessToken from results (company B)
        String adminBAccessToken = adminBLoginJson
                .get("accessToken")
                .asString();

        String employeeBEmail = "detail-employee-b-"
                + UUID.randomUUID()
                + "@labortrack.test";

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeBRequest =
                new EmployeeCreationRequest(
                        "Other",
                        "Company",
                        "973-555-0203",
                        new BigDecimal("28.00"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        employeeBEmail
                );

        // send that request through API using company B's access token
        // and expect created status back and save results
        MvcResult employeeBResult = mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyBId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminBAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeBRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode employeeBJson = objectMapper.readTree(
                employeeBResult.getResponse().getContentAsString()
        );

        // get employeeId from results (company B)
        Long employeeBId = employeeBJson
                .get("employeeId")
                .asLong();

        /*
        admin from company A send a request to read an employee from company B,
        uses JWT from company A. Expect is not found exception because company A
        cannot read employees from company B.
         */
        mockMvc.perform(
                        get(
                                "/api/admin/employees/{employeeId}",
                                employeeBId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAAccessToken
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Employee not found with id: " + employeeBId
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/admin/employees/" + employeeBId
                ))
                .andExpect(jsonPath("$.employeeId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    /**
     * This method-test verifies that an authenticated admin receives
     * only employees belonging to their own company when requesting
     * the admin employee list.
     */
    @Test
    void adminEmployeeListReturnsOnlyOwnCompanyEmployees() throws Exception {

        String adminAEmail = "dashboard-admin-a-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminAPassword = "DashboardAdminAPassword123$";

        // create a (company A + adminUser A registration request)
        CompanyRegistrationRequest companyARequest =
                new CompanyRegistrationRequest(
                        "Dashboard Company A",
                        adminAEmail,
                        adminAPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult companyAResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        companyARequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode companyAJson = objectMapper.readTree(
                companyAResult.getResponse().getContentAsString()
        );

        // get companyId from results (Company A)
        Long companyAId = companyAJson
                .get("companyId")
                .asLong();

        // create admin-login request for company A
        LoginRequest adminALoginRequest =
                new LoginRequest(
                        adminAEmail,
                        adminAPassword
                );

        // send that request through API and expect OK status back and saved results
        MvcResult adminALoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        adminALoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminALoginJson = objectMapper.readTree(
                adminALoginResult.getResponse().getContentAsString()
        );

        // get accessToken from results (company A)
        String adminAAccessToken = adminALoginJson
                .get("accessToken")
                .asString();

        String employeeAEmail = "dashboard-employee-a-"
                + UUID.randomUUID()
                + "@labortrack.test";

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeARequest =
                new EmployeeCreationRequest(
                        "Employee",
                        "Alpha",
                        "973-555-0201",
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        employeeAEmail
                );

        // send that request through API using JWT from company A
        // and expect created status back and saved results
        MvcResult employeeAResult = mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyAId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeARequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode employeeAJson = objectMapper.readTree(
                employeeAResult.getResponse().getContentAsString()
        );

        // get employeeId from results (employee from company A)
        Long employeeAId = employeeAJson
                .get("employeeId")
                .asLong();

        // ---------- Company B ----------

        String adminBEmail = "dashboard-admin-b-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminBPassword = "DashboardAdminBPassword123$";

        // create a (company B + adminUser B registration request)
        CompanyRegistrationRequest companyBRequest =
                new CompanyRegistrationRequest(
                        "Dashboard Company B",
                        adminBEmail,
                        adminBPassword
                );

        // send that request through API and expect created status back and saved results
        MvcResult companyBResult = mockMvc.perform(
                        post("/api/companies/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        companyBRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode companyBJson = objectMapper.readTree(
                companyBResult.getResponse().getContentAsString()
        );

        // get companyId from results (company B)
        Long companyBId = companyBJson
                .get("companyId")
                .asLong();

        // create an admin-login request for company B
        LoginRequest adminBLoginRequest =
                new LoginRequest(
                        adminBEmail,
                        adminBPassword
                );

        // send that request through API and expect OK status back and save results
        MvcResult adminBLoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        adminBLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminBLoginJson = objectMapper.readTree(
                adminBLoginResult.getResponse().getContentAsString()
        );

        // get companyB-access-token from results
        String adminBAccessToken = adminBLoginJson
                .get("accessToken")
                .asString();

        String employeeBEmail = "dashboard-employee-b-"
                + UUID.randomUUID()
                + "@labortrack.test";

        // create an (employee profile + employee user creation request) (employee for company B)
        EmployeeCreationRequest employeeBRequest =
                new EmployeeCreationRequest(
                        "Employee",
                        "Beta",
                        "973-555-0202",
                        new BigDecimal("27.00"),
                        LocalDate.of(2026, 8, 4),
                        null,
                        employeeBEmail
                );

        // send that request through API using companyB accessToken (JWT)
        // and expect created status back
        mockMvc.perform(
                        post(
                                "/api/companies/{companyId}/employees",
                                companyBId
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminBAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeBRequest
                                ))
                )
                .andExpect(status().isCreated());

        /*
         * request the employee list using company A's admin JWT.
         * The response must contain employee A only. Employee B belongs
         * to another company and must not appear.
         */
        mockMvc.perform(
                        get("/api/admin/employees")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + adminAAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(employeeAId))
                .andExpect(jsonPath("$[0].email").value(employeeAEmail))
                .andExpect(jsonPath("$[0].firstName").value("Employee"))
                .andExpect(jsonPath("$[0].lastName").value("Alpha"));
    }

    /**
     * This method-test verifies an authenticated EMPLOYEE cannot
     * access the ADMIN-only dashboard endpoint. The employee
     * changes the temporary password first so the request
     * reaches role authorization instead of being stopped
     * by the password-change required filter.
     */
    @Test
    void employeeCannotAccessAdminDashboard() throws Exception {
        String adminEmail = "dashboard-role-admin-"
                + UUID.randomUUID()
                + "@labortrack.test";
        String adminPassword = "DashboardAdminPassword123$";

        // create a (company + adminUser registration request)
        CompanyRegistrationRequest registrationRequest =
                new CompanyRegistrationRequest(
                        "Dashboard Role Test Company",
                        adminEmail,
                        adminPassword
                );

        // send that request through API and expect created status back and save results
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
        Long companyId = registrationJson.get("companyId").asLong();

        // create an admin-login request
        LoginRequest adminLoginRequest = new LoginRequest(
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

        // get admin-access-token from results
        String adminAccessToken = adminLoginJson
                .get("accessToken").asString();

        String employeeEmail = "dashboard-role-employee-"
                + UUID.randomUUID()
                + "@labortrack.test";

        // create an (employee profile + employee user creation request)
        EmployeeCreationRequest employeeRequest = new EmployeeCreationRequest(
                "Dashboard",
                "Employee",
                "973-555-0200",
                new BigDecimal("25.00"),
                LocalDate.of(2026, 8, 4),
                null,
                employeeEmail
        );

        // send that request through API using admin-access-token (JWT)
        // and expect created status back and save results
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
                .andReturn();

        JsonNode employeeCreationJson = objectMapper.readTree(
                employeeCreationResult.getResponse().getContentAsString()
        );

        // get employee temporaryPassword from results
        String temporaryPassword = employeeCreationJson
                .get("temporaryPassword").asString();

        // create an employee-login request using temp password
        LoginRequest employeeLoginRequest =
                new LoginRequest(
                        employeeEmail,
                        temporaryPassword
                );

        // send that request through API and expect OK status back and save results
        MvcResult employeeLoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        employeeLoginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andReturn();

        JsonNode employeeLoginJson = objectMapper.readTree(
                employeeLoginResult.getResponse().getContentAsString()
        );

        // get employee-access-token from results
        String employeeAccessToken = employeeLoginJson
                .get("accessToken")
                .asString();

        // create a password-change request for (employee)
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest(
                        temporaryPassword,
                        "DashboardEmployeePassword123$"
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
                .andExpect(status().isOk());

        // Try accessing admin/dashboard endpoint using employee JWT
        // expect forbidden status back because admin/dashboard can only
        // being access my ADMIN_ROLE even if they are authenticated already
        mockMvc.perform(
                        get("/api/admin/dashboard")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + employeeAccessToken
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
                        "/api/admin/dashboard"
                ));
    }
}
