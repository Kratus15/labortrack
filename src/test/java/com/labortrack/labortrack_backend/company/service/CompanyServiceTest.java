package com.labortrack.labortrack_backend.company.service;

import com.labortrack.labortrack_backend.common.exception.DuplicateResourceException;
import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompanyServiceTest {

    @Autowired
    CompanyService companyService;

    @Autowired
    PasswordEncoder passwordEncoder;

    /**
     * Test the companyService.registerCompanyWithAdminUser() method
     * and its functionality. It must create a company + adminUser,
     * details must match.
     */
    @Test
    void registerCompanyWithAdminUserCreatesCompanyAndAdminUser() {
        // create random email
        String email = "admin-" + UUID.randomUUID() + "@labortrack.test";

        // register a company using service
        CompanyRegistrationResult result = companyService.registerCompanyWithAdminUser(
                "ABC Construction",
                email,
                "test-password");

        Company testCompany = result.company();
        User testAdminUser = result.adminUser();

        // confirm company was actually register
        assertThat(testCompany.getId()).isNotNull();
        assertThat(testCompany.getName()).isEqualTo("ABC Construction");
        assertThat(testCompany.getEmail()).isEqualTo(email);
        assertThat(testCompany.getTimezone()).isEqualTo("America/New_York");

        assertThat(testAdminUser.getId()).isNotNull();
        assertThat(testAdminUser.getCompany().getId()).isEqualTo(testCompany.getId());
        assertThat(testAdminUser.getPasswordHash()).isNotEqualTo("test-password");
        assertThat(passwordEncoder.matches("test-password", testAdminUser.getPasswordHash())).isTrue();
        assertThat(testAdminUser.getEmail()).isEqualTo(email);
        assertThat(testAdminUser.getRole()).isEqualTo(UserRole.ADMIN);

    }

    /**
     * Test the email already exists (must be unique) condition
     * before create register a company. Try to create two companies
     * with same email, first one should succeed, second should fail,
     * stating email already exists.
     */
    @Test
    void registerCompanyWithAdminUserFailsWhenEmailAlreadyExists() {
        String email = "duplicate-" + UUID.randomUUID() + "@labortrack.test";

        companyService.registerCompanyWithAdminUser(
                "First Company",
                email,
                "test-password1"
        );

        assertThatThrownBy(() -> companyService.registerCompanyWithAdminUser(
                "Second Company",
                email,
                "test-password2"
        )).isInstanceOf(DuplicateResourceException.class);
    }
}
