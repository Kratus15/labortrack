package com.labortrack.labortrack_backend.worksession.controller;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.service.EmployeeService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeService employeeService;

    /**
     * This method-test verifies that an employee's work-session
     * history is correctly paginated. Three closed work sessions
     * are created and requested using a page size of two, which
     * should split the history across two pages.
     */
    @Test
    void getWorkSessionSupportsPagination() throws Exception {
        // create test employee
        Employee testEmployee = createTestEmployee();

        // create authenticated employee
        LaborTrackUserDetails authenticatedEmployee =
                createEmployeePrincipal(testEmployee);

        /*
        create three CLOSED work sessions by clocking the
        employee in and out three separate times.
         */
        for (int i = 0; i < 3; i++) {

            mockMvc.perform(
                            post(
                                    "/api/employees/{employeeId}/clock-in",
                                    testEmployee.getId()
                            )
                                    .with(user(authenticatedEmployee))
                    )
                    .andExpect(status().isCreated());

            mockMvc.perform(
                            post(
                                    "/api/employees/{employeeId}/clock-out",
                                    testEmployee.getId()
                            )
                                    .with(user(authenticatedEmployee))
                    )
                    .andExpect(status().isOk());
        }

        /*
        send a request to /api/employees/{employeeId}/work-sessions
        using authenticated employee with page: 0 and size: 2; Two
        of the three work sessions should be returned. First and second.
         */
        mockMvc.perform(
                        get(
                                "/api/employees/{employeeId}/work-sessions",
                                testEmployee.getId()
                        )
                                .param("page", "0")
                                .param("size", "2")
                                .with(user(authenticatedEmployee))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        /*
        send a request to /api/employees/{employeeId}/work-sessions again
        using authenticated employee but now with page: 1 and size: 2; Two
        of the three work sessions should be returned. Second and third.
         */
        mockMvc.perform(
                        get(
                                "/api/employees/{employeeId}/work-sessions",
                                testEmployee.getId()
                        )
                                .param("page", "1")
                                .param("size", "2")
                                .with(user(authenticatedEmployee))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    /**
     * This test checks if an active employee can clock-in through the API.
     * So it's testing functionality of workSession service and repository layers.
     * First create a test company, save it, then create a test employee that belongs
     * to the test company. Perform a clock-in to the employee and expect a 201 status
     * code and Open work-session in return.
     */
    @Test
    void clockInReturnsCreatedOpenWorkSession() throws Exception {
        // create test employee
        Employee testEmployee = createTestEmployee();

        // create an authenticated employee
        LaborTrackUserDetails authenticatedEmployee = createEmployeePrincipal(testEmployee);

        mockMvc.perform(post(
                        "/api/employees/{employeeId}/clock-in",
                        testEmployee.getId()
                )
                // send authenticated user with the requests
                .with(user(authenticatedEmployee)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.workSessionId").isNumber())
                .andExpect(jsonPath("$.employeeId")
                        .value(testEmployee.getId()))
                .andExpect(jsonPath("$.clockInTime").exists())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.message")
                        .value("Employee with id: "
                                + testEmployee.getId()
                                + ", Clocked in successfully."));
    }

    /**
     * This test checks that an employee cannot clock in twice.
     * It clocks in the employee once successfully, then sends another
     * clock-in request. The second request should return status 400
     * with the standard error response.
     */
    @Test
    void clockInReturnsBadRequestWhenEmployeeAlreadyHasOpenSession() throws Exception {
        // create test employee
        Employee testEmployee = createTestEmployee();

        // create an authenticated employee
        LaborTrackUserDetails authenticatedEmployee = createEmployeePrincipal(testEmployee);

        // send authenticated user with every request
        // clock-in employee, 201 (created) expected, open a work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in",  testEmployee.getId())
                        .with(user(authenticatedEmployee)))
                .andExpect(status().isCreated());

        // clock-in employee again, but this time you expect an error because you can't clock-in while having an open work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in", testEmployee.getId())
                        .with(user(authenticatedEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path")
                        .value("/api/employees/"
                                + testEmployee.getId()
                                + "/clock-in"))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    /**
     * This test checks if an employee with an open work session can clock out
     * through the API. It creates an employee and clocks the employee in first.
     * Then it sends a clock-out request and checks that status 200 is returned
     * with a CLOSED work session, clock-out time, and worked minutes.
     */
    @Test
    void clockOutReturnsClosedWorkSession() throws Exception {
        // create test employee
        Employee testEmployee = createTestEmployee();

        // create an authenticated employee
        LaborTrackUserDetails authenticatedEmployee = createEmployeePrincipal(testEmployee);

        // send authenticated user with every request
        // clock-in the employee first to create open work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in", testEmployee.getId())
                        .with(user(authenticatedEmployee)))
                        .andExpect(status().isCreated());

        // then clock-out and expect a closed work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-out", testEmployee.getId())
                        .with(user(authenticatedEmployee)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.workSessionId").isNumber())
                .andExpect(jsonPath("$.employeeId")
                        .value(testEmployee.getId()))
                .andExpect(jsonPath("$.clockInTime").exists())
                .andExpect(jsonPath("$.clockOutTime").exists())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.workedMinutes").isNumber())
                .andExpect(jsonPath("$.message")
                        .value("Employee with id: "
                                + testEmployee.getId()
                                + ", Clocked out successfully."));
    }

    /**
     * This test checks what happens when an employee tries to clock out
     * without having an open work session. It creates a test employee
     * without clocking them in first. The API should return status 400
     * with the standard error response.
     */
    @Test
    void clockOutReturnsBadRequestWhenEmployeeHasNoOpenSession() throws Exception {
        // create test employee
        Employee testEmployee = createTestEmployee();

        // create an authenticated employee
        LaborTrackUserDetails authenticatedEmployee = createEmployeePrincipal(testEmployee);

        // send authenticated user with every request
        // The employee does not have open work-session. Try to clock out and expect bad request back.
        mockMvc.perform(post("/api/employees/{employeeId}/clock-out", testEmployee.getId())
                        .with(user(authenticatedEmployee)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Employee with id="
                                + testEmployee.getId()
                                + " does not have an open work session to clock out."))
                .andExpect(jsonPath("$.path")
                        .value("/api/employees/"
                                + testEmployee.getId()
                                + "/clock-out"))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    /**
     * This method-test verifies that the employee's work-session
     * history endpoint correctly returns an OPEN work session. The
     * employee is clocked in, then their history is retrieved and
     * should contain the open session with no clock-out time or
     * worked minutes.
     */
    @Test
    void getWorkSessionsReturnsOpenWorkSessionHistory() throws Exception {
        // create test employee
        Employee testEmployee = createTestEmployee();

        // create an authenticated employee
        LaborTrackUserDetails authenticatedEmployee = createEmployeePrincipal(testEmployee);

        // send authenticated user with every request

        // clock the employee in to create one OPEN work session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in", testEmployee.getId())
                .with(user(authenticatedEmployee)))
                .andExpect(status().isCreated());

        /*
        retrieve employee's work-sessions history and expect the
        OPEN work-session is returned with pagination metadata.
         */
        mockMvc.perform(get("/api/employees/{employeeId}/work-sessions", testEmployee.getId())
                .with(user(authenticatedEmployee)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].workSessionId").isNumber())
                .andExpect(jsonPath("$.content[0].companyId")
                        .value(testEmployee.getCompany().getId()))
                .andExpect(jsonPath("$.content[0].employeeId")
                        .value(testEmployee.getId()))
                .andExpect(jsonPath("$.content[0].clockInTime").exists())
                .andExpect(jsonPath("$.content[0].clockOutTime").doesNotExist())
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].workedMinutes").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    /**
     * This method-test verifies that the employee work-session history
     * endpoint correctly return a CLOSED work-session. The employee is
     * clocked in and then clock out, and the returned session should
     * include a clock out time and worked minutes.
     */
    @Test
    void getWorkSessionsReturnsClosedWorkSessionHistory() throws Exception {
        Employee testEmployee = createTestEmployee();

        // create an authenticated employee
        LaborTrackUserDetails authenticatedEmployee = createEmployeePrincipal(testEmployee);

        // send authenticated user with every request

        // clock the employee in to create an OPEN work session
        mockMvc.perform(post(
                        "/api/employees/{employeeId}/clock-in",
                        testEmployee.getId()
                )
                        .with(user(authenticatedEmployee)))
                .andExpect(status().isCreated());

        // clock the employee out to close the current work session
        mockMvc.perform(post(
                        "/api/employees/{employeeId}/clock-out",
                        testEmployee.getId()
                )
                        .with(user(authenticatedEmployee)))
                .andExpect(status().isOk());

        /*
        retrieve the employee's work-session history and verify that
        the CLOSED session is returned with its metadata
         */
        mockMvc.perform(get(
                        "/api/employees/{employeeId}/work-sessions",
                        testEmployee.getId()
                )
                        .with(user(authenticatedEmployee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].workSessionId").isNumber())
                .andExpect(jsonPath("$.content[0].companyId")
                        .value(testEmployee.getCompany().getId()))
                .andExpect(jsonPath("$.content[0].employeeId")
                        .value(testEmployee.getId()))
                .andExpect(jsonPath("$.content[0].clockInTime").exists())
                .andExpect(jsonPath("$.content[0].clockOutTime").exists())
                .andExpect(jsonPath("$.content[0].status").value("CLOSED"))
                .andExpect(jsonPath("$.content[0].workedMinutes").isNumber())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    /**
     * This test checks what happens when work-session history is requested
     * for an employee that does not exist. The API should return status (404)
     * and custom dto error response.
     */
    @Test
    void getWorkSessionsReturnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        Company testCompany = createTestCompany();

        // create an authenticated employee
        LaborTrackUserDetails authenticatedAdmin = createAdminPrincipal(testCompany.getId());

        // send authenticated user with every request
        mockMvc.perform(get(
                        "/api/employees/{employeeId}/work-sessions",
                        999999L
                )
                        .with(user(authenticatedAdmin)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Employee with id=999999 not found."))
                .andExpect(jsonPath("$.path")
                        .value("/api/employees/999999/work-sessions"))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    // HELPER METHODS
    private Employee createTestEmployee() {
        Company testCompany = createTestCompany();

        String employeeEmail = "worksession-employee-" + UUID.randomUUID() + "@labortrack.test";
        return employeeService.createEmployee(
                testCompany.getId(),
                employeeEmail,
                "Yonelvyn",
                "Morel",
                "973-555-0100",
                new BigDecimal("27.5"),
                LocalDate.of(2026, 7, 21),
                "https://example.com/images/yonelvyn-morel.jpg"
        ).employee();
    }
    private Company createTestCompany() {
        Company company = new Company();
        company.setName("Work Session Controller Test");
        company.setTimezone("America/New_York");
        company.setEmail("worksession-company-" + UUID.randomUUID() + "@labortrack.test");
        return companyRepository.save(company);
    }
    private LaborTrackUserDetails createEmployeePrincipal(Employee employee) {
        return new LaborTrackUserDetails(
                employee.getUser().getId(),
                employee.getCompany().getId(),
                employee.getId(),
                employee.getUser().getEmail(),
                "{noop}unused",
                UserRole.EMPLOYEE,
                true,
                false
        );
    }
    private LaborTrackUserDetails createAdminPrincipal(Long companyId) {
        return new LaborTrackUserDetails(
                2000L,
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
