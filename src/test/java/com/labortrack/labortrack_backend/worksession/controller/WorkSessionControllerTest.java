package com.labortrack.labortrack_backend.worksession.controller;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.service.EmployeeService;
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

        mockMvc.perform(post(
                        "/api/employees/{employeeId}/clock-in",
                        testEmployee.getId()
                ))
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

        // clock-in employee, 201 (created) expected, open a work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in",  testEmployee.getId()))
                .andExpect(status().isCreated());
        // clock-in employee again, but this time you expect an error because you can't clock-in while having an open work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in", testEmployee.getId()))
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

        // clock-in the employee first to create open work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in", testEmployee.getId()))
                        .andExpect(status().isCreated());

        // then clock-out and expect a closed work-session
        mockMvc.perform(post("/api/employees/{employeeId}/clock-out", testEmployee.getId()))
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

        // The employee does not have open work-session. Try to clock out and expect bad request back.
        mockMvc.perform(post("/api/employees/{employeeId}/clock-out", testEmployee.getId()))
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
     * This test checks if the API can return an employee's work-session history
     * while the employee still has an open work session. It clocks the employee
     * in and retrieves the history. The response should return status 200 with
     * the OPEN session, no clock-out time, and no worked minutes yet.
     */
    @Test
    void getWorkSessionsReturnsOpenWorkSessionHistory() throws Exception {
        // create test employee
        Employee testEmployee = createTestEmployee();

        // create one open workSession
        mockMvc.perform(post("/api/employees/{employeeId}/clock-in", testEmployee.getId()))
                .andExpect(status().isCreated());

        // retrieve employee work-sessions and expect the open work-session above first.
        mockMvc.perform(get("/api/employees/{employeeId}/work-sessions", testEmployee.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workSessionId").isNumber())
                .andExpect(jsonPath("$[0].companyId")
                        .value(testEmployee.getCompany().getId()))
                .andExpect(jsonPath("$[0].employeeId")
                        .value(testEmployee.getId()))
                .andExpect(jsonPath("$[0].clockInTime").exists())
                .andExpect(jsonPath("$[0].clockOutTime").doesNotExist())
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].workedMinutes").doesNotExist());
    }

    /**
     * This test checks if a closed work session appears correctly in the
     * employee's work-session history. It clocks the employee in and out,
     * then retrieves the history. The returned session should be CLOSED
     * and include the clock-out time and worked minutes.
     */
    @Test
    void getWorkSessionsReturnsClosedWorkSessionHistory() throws Exception {
        Employee testEmployee = createTestEmployee();

        // Create an open work session
        mockMvc.perform(post(
                        "/api/employees/{employeeId}/clock-in",
                        testEmployee.getId()
                ))
                .andExpect(status().isCreated());

        // Close the work session
        mockMvc.perform(post(
                        "/api/employees/{employeeId}/clock-out",
                        testEmployee.getId()
                ))
                .andExpect(status().isOk());

        // Retrieve the closed work session from the employee history.
        mockMvc.perform(get(
                        "/api/employees/{employeeId}/work-sessions",
                        testEmployee.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workSessionId").isNumber())
                .andExpect(jsonPath("$[0].companyId")
                        .value(testEmployee.getCompany().getId()))
                .andExpect(jsonPath("$[0].employeeId")
                        .value(testEmployee.getId()))
                .andExpect(jsonPath("$[0].clockInTime").exists())
                .andExpect(jsonPath("$[0].clockOutTime").exists())
                .andExpect(jsonPath("$[0].status").value("CLOSED"))
                .andExpect(jsonPath("$[0].workedMinutes").isNumber());
    }

    /**
     * This test checks what happens when work-session history is requested
     * for an employee that does not exist. The API should return status (404)
     * and custom dto error response.
     */
    @Test
    void getWorkSessionsReturnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        mockMvc.perform(get(
                        "/api/employees/{employeeId}/work-sessions",
                        999999L
                ))
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
        Company company = new Company();
        company.setName("Work Session Controller Test");
        company.setTimezone("America/New_York");
        company.setEmail("worksession-company-" + UUID.randomUUID() + "@labortrack.test");
        Company savedCompany = companyRepository.save(company);

        String employeeEmail = "worksession-employee-" + UUID.randomUUID() + "@labortrack.test";
        return employeeService.createEmployee(
                savedCompany.getId(),
                employeeEmail,
                "Yonelvyn",
                "Morel",
                "973-555-0100",
                new BigDecimal("27.5"),
                LocalDate.of(2026, 7, 21),
                "https://example.com/images/yonelvyn-morel.jpg"
        );
    }
}
