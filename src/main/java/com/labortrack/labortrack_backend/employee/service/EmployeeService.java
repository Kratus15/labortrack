package com.labortrack.labortrack_backend.employee.service;

import com.labortrack.labortrack_backend.common.exception.ResourceNotFoundException;
import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.employee.repository.EmployeeRepository;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import com.labortrack.labortrack_backend.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class EmployeeService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final UserService userService;

    public EmployeeService(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository, UserService userService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.userService = userService;
    }


    /**
     * Created an employee (employee profile + employeeUser)
     * This operation must only be done by admin user that
     * has a company. The employee user must have unique email,
     * and will have a temp initial random hashed password,
     * needed to change it once login.
     */
    @Transactional
    public Employee createEmployee(
            Long companyId,
            String userEmployeeEmail,
            String firstName,
            String lastName,
            String phone,
            BigDecimal hourlyRate,
            LocalDate hireDate,
            String profileImageUrl
            ) {

        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        validateHourlyRate(hourlyRate);

        Company company = companyRepository.findById(companyId).orElseThrow(
                () -> new ResourceNotFoundException("Company with id=" + companyId + " not found.")
        );

        String normalizedEmail = userService.normalizeEmail(userEmployeeEmail);
        userService.ensureEmailIsAvailable(normalizedEmail);
        String tempPasswordGenerated = userService.generateTemporaryPasswordHashedPlaceholder();


        // register employee user
        User employeeUser = new User();
        employeeUser.setEmail(normalizedEmail);
        employeeUser.setPasswordHash(tempPasswordGenerated);
        employeeUser.setEnabled(true);
        employeeUser.setMustChangePassword(true);
        employeeUser.setCompany(company);
        employeeUser.setRole(UserRole.EMPLOYEE);

        User savedEmployeeUser = userRepository.save(employeeUser);

        // register and connect to employee profile
        Employee employee = new Employee();
        employee.setFirstName(firstName.trim());
        employee.setLastName(lastName.trim());
        employee.setPhone(
                phone == null || phone.isBlank()
                        ? null
                        : phone.trim()
        ); // make phone null if blank or null otherwise trim
        employee.setHourlyRate(hourlyRate);
        employee.setHireDate(hireDate);
        employee.setProfileImageUrl(
                profileImageUrl == null || profileImageUrl.isBlank()
                        ? null
                        : profileImageUrl.trim()
        ); // make profImg null if blank or null otherwise trim
        employee.setCompany(company); // connect employee to company
        employee.setUser(savedEmployeeUser); // connect employee to user
        employee.setStatus(EmployeeStatus.ACTIVE);

        return employeeRepository.save(employee);
    }

    // HELPER METHODS

    private void validateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required, cannot be null or blank.");
        }
    }
    private void validateHourlyRate(BigDecimal hourlyRate) {
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Hourly rate is required and cannot be negative.");
        }
    }
}
