package com.labortrack.labortrack_backend.worksession.service;

import com.labortrack.labortrack_backend.common.exception.InvalidWorkSessionStateException;
import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.employee.repository.EmployeeRepository;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import com.labortrack.labortrack_backend.worksession.entity.WorkSession;
import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkSessionServiceTest {

    @Autowired
    private WorkSessionService workSessionService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * This tests WorkSessionService.clockIn() method functionality.
     * Simply call the method, using a test employee as dummy,
     * that implies that the WorkSession created must match a couple
     * of fields state to be functionality correct. This will create
     * an OPEN workSession under the dummy employee and dummy employee's
     * company, clock can't be null, but clock out must be null, etc
     */
    @Test
    void clockInCreatesOpenWorkSession() {
        Employee employee = createTestEmployee(EmployeeStatus.ACTIVE);

        WorkSession workSession = workSessionService.clockIn(employee.getId());

        assertThat(workSession.getId()).isNotNull();
        assertThat(workSession.getEmployee().getId()).isEqualTo(employee.getId());
        assertThat(workSession.getCompany().getId()).isEqualTo(employee.getCompany().getId());
        assertThat(workSession.getClockInTime()).isNotNull();
        assertThat(workSession.getClockOutTime()).isNull();
        assertThat(workSession.getStatus()).isEqualTo(WorkSessionStatus.OPEN);
    }

    /**
     * This tests WorkSessionService.clockIn() constraint when trying to clock in
     * when the employee already have an open workSession on his record. ClockIn
     * while the employee have an open workSession should fail, this test perform
     * the action twice, first one should succeed, if yes, then second must define
     * fail to have safe constraint.
     */
    @Test
    void clockInFailsWhenEmployeeAlreadyHasOpenWorkSession() {
        Employee employee = createTestEmployee(EmployeeStatus.ACTIVE);

        workSessionService.clockIn(employee.getId());

        assertThatThrownBy(() -> workSessionService.clockIn(employee.getId()))
                .isInstanceOf(InvalidWorkSessionStateException.class);
    }

    /**
     * This test WorkSessionService.clockIn() constraint when trying to clock in
     * but the employee is not active. An employee that is INACTIVE should not perform
     * clockIn, the clockIn must throw. Here we create a dummy employee with inactive
     * status, if employee perform clockIn action, this must throw to confirm
     * safe constraint.
     */
    @Test
    void clockInFailsWhenEmployeeIsNotActive() {
        Employee employee = createTestEmployee(EmployeeStatus.INACTIVE);

        assertThatThrownBy(() -> workSessionService.clockIn(employee.getId()))
                .isInstanceOf(InvalidWorkSessionStateException.class);
    }

    /**
     * This test workSessionService.clockOut() basically it clocks out a
     * valid employee, with an open workSession. When we perform the operation
     * the previous OPEN punch must now be CLOSED, and clockOut shouldn't be null
     * as previous.
     */
    @Test
    void clockOutClosesOpenWorkSession() {
        Employee employee = createTestEmployee(EmployeeStatus.ACTIVE);

        WorkSession openSession = workSessionService.clockIn(employee.getId());

        WorkSession closedSession = workSessionService.clockOut(employee.getId());

        assertThat(closedSession.getId()).isEqualTo(openSession.getId());
        assertThat(closedSession.getClockOutTime()).isNotNull();
        assertThat(closedSession.getStatus()).isEqualTo(WorkSessionStatus.CLOSED);
    }

    /**
     * This test workSessionService.clockOut() constraint, clockOut
     * should not be allowed if the employee does not have an OPEN
     * workSession. If a valid employee aims to clockOut when he doesn't
     * have a OPEN workSession, action must fail to ensure safe constraint.
     * Only valid employees with OPEN workSession can clockOut. Otherwise,
     * throw exception
     */
    @Test
    void clockOutFailsWhenEmployeeHasNoOpenWorkSession() {
        Employee employee = createTestEmployee(EmployeeStatus.ACTIVE);

        assertThatThrownBy(() -> workSessionService.clockOut(employee.getId()))
                .isInstanceOf(InvalidWorkSessionStateException.class);
    }

    /**
     * This test WorkSessionService.calculateWorkedDuration() basically it
     * calculates the duration of a CLOSED workSession in hours. So we
     * create an artificial workSession with random dates that have an
     * 8-hour gap. When we call the method we must expect the same
     * gap return for its functionality be correct.
     */
    @Test
    void calculateWorkedDurationReturnsCorrectDuration() {
        WorkSession workSession = new WorkSession();
        workSession.setStatus(WorkSessionStatus.CLOSED);
        workSession.setClockInTime(OffsetDateTime.parse("2026-01-01T09:00:00Z"));
        workSession.setClockOutTime(OffsetDateTime.parse("2026-01-01T17:00:00Z"));

        Duration duration = workSessionService.calculateWorkedDuration(workSession);

        assertThat(duration).isEqualTo(Duration.ofHours(8));
    }

    /**
     * This test WorkSessionService.calculateWorkedDuration() constraint,
     * calculateWorkedDuration() can't be performed with OPEN workSession or
     * clockOut null. if calculateWorkedDuration() gets triggers to calculate
     * with OPEN workSession we must receive an Exception for safe constraint.
     * calculateWorkedDuration() should only be allowed to perform when workSession
     * status is CLOSED, clockIn and clockOut aren't null, clockIn is before clockOut.
     */
    @Test
    void calculateWorkedDurationFailsForOpenWorkSession() {
        WorkSession workSession = new WorkSession();
        workSession.setStatus(WorkSessionStatus.OPEN);
        workSession.setClockInTime(OffsetDateTime.parse("2026-01-01T09:00:00Z"));

        assertThatThrownBy(() -> workSessionService.calculateWorkedDuration(workSession))
                .isInstanceOf(InvalidWorkSessionStateException.class);
    }

    // HELPER METHODS
    private Employee createTestEmployee(EmployeeStatus status) {
        Company testCompany = createTestCompany();
        User testUser = createTestUser(testCompany);

        Employee testEmployee = new Employee();
        testEmployee.setStatus(status);
        testEmployee.setCompany(testCompany);
        testEmployee.setUser(testUser);
        testEmployee.setFirstName("test");
        testEmployee.setLastName("employee");
        testEmployee.setHourlyRate(new BigDecimal("25.00"));

        return employeeRepository.save(testEmployee);
    }

    private User createTestUser(Company testCompany) {
        String email = "user-" + UUID.randomUUID() + "@labortrack.test";

        User user = new User();
        user.setCompany(testCompany);
        user.setEmail(email);
        user.setPasswordHash("test-password");
        user.setRole(UserRole.EMPLOYEE);

        return userRepository.save(user);
    }

    private Company createTestCompany() {
        String companyEmail = "company-" + UUID.randomUUID() + "@labortrack.test";

        Company company = new Company();
        company.setEmail(companyEmail);
        company.setName("Company Test");
        company.setTimezone("America/New_York");

        return companyRepository.save(company);
    }

}
