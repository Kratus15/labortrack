package com.labortrack.labortrack_backend.employee.service;

import com.labortrack.labortrack_backend.common.exception.DuplicateResourceException;
import com.labortrack.labortrack_backend.common.exception.ResourceNotFoundException;
import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeeServiceTest {

    @Autowired
    EmployeeService employeeService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CompanyRepository companyRepository;

    /**
     * Create Employee (action) test. Creates an employee,
     * using a test company and a random email, then we check
     * if data was accurate and correct. Create employee must
     * create an employee profile + employee user.
     */
    @Test
    void createEmployeeCreatesEmployeeProfileAndUserAccount() {
        Company testCompany = createTestCompany();

        String employeeEmail = "employee-" + UUID.randomUUID() + "@labortrack.test";

        Employee employeeCreated = employeeService.createEmployee(
                testCompany.getId(),
                employeeEmail,
                "Yonelvyn",
                "Morel",
                new BigDecimal("25.00")
        );

        assertThat(employeeCreated.getId()).isNotNull();
        assertThat(employeeCreated.getCompany().getId()).isEqualTo(testCompany.getId());
        assertThat(employeeCreated.getFirstName()).isEqualTo("Yonelvyn");
        assertThat(employeeCreated.getLastName()).isEqualTo("Morel");
        assertThat(employeeCreated.getHourlyRate()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(employeeCreated.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employeeCreated.getUser()).isNotNull();

        Optional<User> employeeUserOptional = userRepository.findById(employeeCreated.getUser().getId());
        assertThat(employeeUserOptional).isPresent();

        User employeeUser = employeeUserOptional.get();

        assertThat(employeeUser.getId()).isNotNull();
        assertThat(employeeUser.getCompany().getId()).isEqualTo(testCompany.getId());
        assertThat(employeeUser.getEmail()).isEqualTo(employeeEmail);
        assertThat(employeeUser.getRole()).isEqualTo(UserRole.EMPLOYEE);
        assertThat(employeeUser.isMustChangePassword()).isTrue();

    }

    /**
     * This test is meant to check constraints about the
     * duplicate emails. An email should be unique. When
     * you used an email you can't use it again, throw an
     * exception. Here we create two emails with different
     * information, except the email, they both use the same
     * email. First employee create should success, if success,
     * then second employee must fail, for it to pass the constraint
     * duplicate test.
     */
    @Test
    void createEmployeeFailsWhenEmailAlreadyExists() {
        Company company = createTestCompany();
        // will use same email to create both employees
        String employeeEmail = "duplicate-employee-" + UUID.randomUUID() + "@labortrack.test";

        employeeService.createEmployee(
                company.getId(),
                employeeEmail,
                "Yonelvyn",
                "Morel",
                new BigDecimal("25.00")
        );

        assertThatThrownBy(() -> employeeService.createEmployee(
                company.getId(),
                employeeEmail,
                "Nohelvyn",
                "Garcia",
                new BigDecimal("30.00")
        )).isInstanceOf(DuplicateResourceException.class);
    }

    /**
     * This test is meant to test if we get a throw exception
     * when we try to create an employee using a company that doesn't
     * exist. To create an employee, the company must already exist.
     * otherwise, can't create a company. Therefore, company must exist.
     */
    @Test
    void createEmployeeFailsWhenCompanyDoesNotExist() {
        String employeeEmail = "missing-company-" + UUID.randomUUID() + "@labortrack.test";

        assertThatThrownBy(() -> employeeService.createEmployee(
                999999L,
                employeeEmail,
                "Yonelvyn",
                "Morel",
                new BigDecimal("25.00")
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    // HELPER METHODS

    private Company createTestCompany() {
        String email = "company-" + UUID.randomUUID() + "@labortrack.test";

        Company company = new Company();
        company.setName("Test Company");
        company.setEmail(email);
        company.setTimezone("America/New_York");

        return companyRepository.save(company);
    }
}
