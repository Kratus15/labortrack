package com.labortrack.labortrack_backend.repository;

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
import com.labortrack.labortrack_backend.worksession.repository.WorkSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // not replace with h2, use real configure test db
public class RepositoryDataJpaTest {

    // load and inject entities

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WorkSessionRepository workSessionRepository;

    /**
     * Test that spring boots can load JPA entities dependencies
     */
    @Test
    void repositoriesLoadSuccessfully() {
        assertThat(companyRepository).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(employeeRepository).isNotNull();
        assertThat(workSessionRepository).isNotNull();
    }

    /**
     * Method proves that JPA entities can create real data and save it on db
     */
    @Test
    void canInsertBasicLaborTrackData() {
        // company
        Company company = new Company();
        company.setName("Test Company");
        company.setEmail("company@test.com");
        company.setTimezone("America/New_York");

        Company savedCompany = companyRepository.save(company);

        // admin user
        User adminUser = new User();
        adminUser.setCompany(savedCompany);
        adminUser.setEmail("admin@test.com");
        adminUser.setPasswordHash("hashed-password");
        adminUser.setRole(UserRole.ADMIN);

        User savedAdminUser = userRepository.save(adminUser);

        // Employee user
        User employeeUser = new User();
        employeeUser.setCompany(savedCompany);
        employeeUser.setEmail("employee@test.com");
        employeeUser.setPasswordHash("hashed-password");
        employeeUser.setRole(UserRole.EMPLOYEE);

        User savedEmployeeUser = userRepository.save(employeeUser);

        // Employee profile
        Employee employee = new Employee();
        employee.setCompany(savedCompany);
        employee.setUser(savedEmployeeUser);
        employee.setFirstName("John");
        employee.setLastName("Worker");
        employee.setStatus(EmployeeStatus.ACTIVE);

        Employee savedEmployee = employeeRepository.save(employee);

        // Work session
        WorkSession workSession = new WorkSession();
        workSession.setCompany(savedCompany);
        workSession.setEmployee(savedEmployee);
        workSession.setClockInTime(OffsetDateTime.now(ZoneOffset.UTC));
        workSession.setStatus(WorkSessionStatus.OPEN);
        workSession.setNote("Test work session.");

        WorkSession savedWorkSession = workSessionRepository.save(workSession);

        assertThat(savedCompany.getId()).isNotNull();
        assertThat(savedAdminUser.getId()).isNotNull();
        assertThat(savedEmployeeUser.getId()).isNotNull();
        assertThat(savedEmployee.getId()).isNotNull();
        assertThat(savedWorkSession.getId()).isNotNull();
    }

    /**
     * Meant to test custom queries from UserRepository.
     * Specifically test findByEmail, and emailExists are
     * functioning correctly.
     */
    @Test
    void canFindUserByEmailAndCheckEmailExists() {
        // Company
        Company company = new Company();
        company.setName("Test Company");
        company.setEmail("company@test.com");
        company.setTimezone("America/New_York");

        Company savedCompany = companyRepository.save(company);

        // User
        User user = new User();
        user.setCompany(savedCompany);
        user.setEmail("admin@test.com");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.ADMIN);

        User savedUser = userRepository.save(user);

        Optional<User> foundUser = userRepository.findByEmail(savedUser.getEmail());

        assertThat(foundUser).isPresent();

        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());

        boolean emailExists = userRepository.existsByEmail(savedUser.getEmail());
        boolean missingEmailExists = userRepository.existsByEmail("missing-email@test.com");

        assertThat(emailExists).isTrue();
        assertThat(missingEmailExists).isFalse();
    }

    /**
     * Meant to test custom queries from EmployeeRepository.
     * Specifically findByCompany, and findByCompanyAndStatus
     */
    @Test
    void canFindEmployeesByCompanyAndStatus() {
        // company
        Company company = new Company();
        company.setName("Test Company");
        company.setEmail("company@test.com");
        company.setTimezone("America/New_York");

        Company savedCompany = companyRepository.save(company);

        // employee user
        User employeeUser = new User();
        employeeUser.setCompany(savedCompany);
        employeeUser.setEmail("employeeUser@test.com");
        employeeUser.setPasswordHash("hashed-password");
        employeeUser.setRole(UserRole.EMPLOYEE);

        User savedEmployeeUser = userRepository.save(employeeUser);

        // employee profile
        Employee employee = new Employee();
        employee.setCompany(savedCompany);
        employee.setUser(savedEmployeeUser);
        employee.setFirstName("John");
        employee.setLastName("Worker");
        employee.setStatus(EmployeeStatus.ACTIVE);

        Employee savedEmployee = employeeRepository.save(employee);

        List<Employee> companyEmployees = employeeRepository.findByCompany(savedEmployee.getCompany());

        List<Employee> companyActiveEmployees = employeeRepository.findByCompanyAndStatus(savedEmployee.getCompany(), EmployeeStatus.ACTIVE);

        List<Employee> companyInactiveEmployees = employeeRepository.findByCompanyAndStatus(savedEmployee.getCompany(), EmployeeStatus.INACTIVE);

        assertThat(companyEmployees).hasSize(1);
        assertThat(companyEmployees.getFirst().getId()).isEqualTo(savedEmployee.getId());

        assertThat(companyActiveEmployees).hasSize(1);
        assertThat(companyActiveEmployees.getFirst().getId()).isEqualTo(savedEmployee.getId());

        assertThat(companyInactiveEmployees).isEmpty();
    }

    /**
     * Meant to test custom queries from WorkSessionRepository.
     */
    @Test
    void canFindWorkSessionsByEmployeeCompanyAndStatus() {
        // company
        Company company = new Company();
        company.setName("Test Company");
        company.setEmail("company-worksession@test.com");
        company.setTimezone("America/New_York");

        Company savedCompany = companyRepository.save(company);

        // employee user
        User employeeUser = new User();
        employeeUser.setCompany(savedCompany);
        employeeUser.setEmail("employee-worksession@test.com");
        employeeUser.setPasswordHash("hashed-password");
        employeeUser.setRole(UserRole.EMPLOYEE);

        User savedEmployeeUser = userRepository.save(employeeUser);

        // employee profile
        Employee employee = new Employee();
        employee.setCompany(savedCompany);
        employee.setUser(savedEmployeeUser);
        employee.setFirstName("John");
        employee.setLastName("Worker");
        employee.setStatus(EmployeeStatus.ACTIVE);

        Employee savedEmployee = employeeRepository.save(employee);

        // older closed work session
        WorkSession closedSession = new WorkSession();
        closedSession.setCompany(savedCompany);
        closedSession.setEmployee(savedEmployee);
        closedSession.setClockInTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        closedSession.setClockOutTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).plusHours(8));
        closedSession.setStatus(WorkSessionStatus.CLOSED);
        closedSession.setNote("Closed test work session.");

        WorkSession savedClosedSession = workSessionRepository.save(closedSession);

        // newer open work session
        WorkSession openSession = new WorkSession();
        openSession.setCompany(savedCompany);
        openSession.setEmployee(savedEmployee);
        openSession.setClockInTime(OffsetDateTime.now(ZoneOffset.UTC));
        openSession.setStatus(WorkSessionStatus.OPEN);
        openSession.setNote("Open test work session.");

        WorkSession savedOpenSession = workSessionRepository.save(openSession);

        Optional<WorkSession> employeeOpenSession =
                workSessionRepository.findByEmployeeAndStatus(savedEmployee, WorkSessionStatus.OPEN);

        List<WorkSession> employeeWorkSessions =
                workSessionRepository.findByEmployeeOrderByClockInTimeDesc(savedEmployee);

        List<WorkSession> companyWorkSessions =
                workSessionRepository.findByCompanyOrderByClockInTimeDesc(savedCompany);

        List<WorkSession> companyOpenSessions =
                workSessionRepository.findByCompanyAndStatus(savedCompany, WorkSessionStatus.OPEN);

        List<WorkSession> companyClosedSessions =
                workSessionRepository.findByCompanyAndStatus(savedCompany, WorkSessionStatus.CLOSED);

        assertThat(employeeOpenSession).isPresent();
        assertThat(employeeOpenSession.get().getId()).isEqualTo(savedOpenSession.getId());

        assertThat(employeeWorkSessions).hasSize(2);
        assertThat(employeeWorkSessions.get(0).getId()).isEqualTo(savedOpenSession.getId());
        assertThat(employeeWorkSessions.get(1).getId()).isEqualTo(savedClosedSession.getId());

        assertThat(companyWorkSessions).hasSize(2);
        assertThat(companyWorkSessions.get(0).getId()).isEqualTo(savedOpenSession.getId());
        assertThat(companyWorkSessions.get(1).getId()).isEqualTo(savedClosedSession.getId());

        assertThat(companyOpenSessions).hasSize(1);
        assertThat(companyOpenSessions.get(0).getId()).isEqualTo(savedOpenSession.getId());

        assertThat(companyClosedSessions).hasSize(1);
        assertThat(companyClosedSessions.get(0).getId()).isEqualTo(savedClosedSession.getId());
    }
}
