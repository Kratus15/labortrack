package com.labortrack.labortrack_backend.security.user;

import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * This class represents the user information that Spring Security needs during authentication.
 * After the user logs in, Spring Security stores this object in the security context so the application
 * can identify the current user. We use a custom UserDetails class so we only store the information that
 * is needed,such as the user's ID, company ID, employee ID, email, role, password, and enabled status.
 * Rather than loading the entire JPA entity. This is our custom UserDetails DTO.
 */
public final class LaborTrackUserDetails implements UserDetails {
    private final Long userId;
    private final Long companyId;
    private final Long employeeId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean enabled;
    private final boolean mustChangePassword;

    public LaborTrackUserDetails(Long userId, Long companyId, Long employeeId, String email, String passwordHash, UserRole role, boolean enabled, boolean mustChangePassword) {
        this.userId = userId;
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
       this.mustChangePassword = mustChangePassword;
    }

    /**
     * This method creates and returns an object of this class just by passing
     * the User object. Very useful.
     */
    public static LaborTrackUserDetails from(User user) {
        Long employeeId = user.getEmployee() == null
                ? null
                : user.getEmployee().getId();

        return new LaborTrackUserDetails(
                user.getId(),
                user.getCompany().getId(),
                employeeId,
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isEnabled(),
                user.isMustChangePassword()
        );
    }
    public Long getUserId() {
        return userId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /**
     * This method set the authority for each user in a spring security
     * format using ROLE_ + role.name;
     * */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Our authentification does not use username, it uses email,
     * so we use email as username.
     */
    @Override
    public String getUsername() {
        return email;
    }
}
