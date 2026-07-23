package com.labortrack.labortrack_backend.company.service;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import com.labortrack.labortrack_backend.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public CompanyService(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a company + adminUser together. Both must be created.
     * If Company gets created and adminUser fail rollback.
     * If adminUser gets created and company fails rollback.
     * Transactional handles rollback for us if a runtime exception happens.
     * No need to validate if both gets register, transactional automatically
     * rollback if both aren't created.
     */
    @Transactional
    public CompanyRegistrationResult registerCompanyWithAdminUser(
            String companyName,
            String adminEmail,
            String adminPassword) {

        validateCompanyName(companyName);
        validatePassword(adminPassword);

        String encodedPassword = passwordEncoder.encode(adminPassword);

        String normalizedEmail = userService.normalizeEmail(adminEmail);
        userService.ensureEmailIsAvailable(normalizedEmail);

        // register company
        Company company = new Company();
        company.setName(companyName.trim());
        company.setEmail(normalizedEmail);
        company.setTimezone("America/New_York");

        Company registeredCompany = companyRepository.save(company);

        // register adminUser
        User adminUser = new User();
        adminUser.setCompany(registeredCompany);
        adminUser.setEmail(normalizedEmail);
        adminUser.setPasswordHash(encodedPassword);
        adminUser.setEnabled(true);
        adminUser.setMustChangePassword(false);
        adminUser.setRole(UserRole.ADMIN);

        User savedAdminUser = userRepository.save(adminUser);

        return new CompanyRegistrationResult(
                registeredCompany,
                savedAdminUser
        );
    }

    // HELPER METHODS

    private void validateCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("company name is required, cannot be null or blank.");
        }
    }

    private void validatePassword(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required, cannot be null or blank.");
        }
    }
}
