package com.labortrack.labortrack_backend.company.service;

import com.labortrack.labortrack_backend.common.exception.DuplicateResourceException;
import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import com.labortrack.labortrack_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
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
    UserRepository userRepository;

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
        Company testCompany = companyService.registerCompanyWithAdminUser(
                "ABC Construction",
                email,
                "test-password");

        // confirm company was actually register
        assertThat(testCompany.getId()).isNotNull();
        assertThat(testCompany.getName()).isEqualTo("ABC Construction");
        assertThat(testCompany.getEmail()).isEqualTo(email);
        assertThat(testCompany.getTimezone()).isEqualTo("America/New_York");

        // confirm user was also register as well
        Optional<User> adminUserOptional = userRepository.findByEmail(email);
        assertThat(adminUserOptional).isPresent();

        User adminUser = adminUserOptional.get();

        assertThat(adminUser.getId()).isNotNull();
        assertThat(adminUser.getCompany().getId()).isEqualTo(testCompany.getId());
        assertThat(adminUser.getEmail()).isEqualTo(email);
        assertThat(adminUser.getRole()).isEqualTo(UserRole.ADMIN);

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
