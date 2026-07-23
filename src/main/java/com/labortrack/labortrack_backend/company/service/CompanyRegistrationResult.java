package com.labortrack.labortrack_backend.company.service;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.user.entity.User;

/**
 * Return both company and admin user from the service.
 * The registration response needs id from both records,
 * service should return both created objects, without making
 * controller perform another db query.
 */
public record CompanyRegistrationResult(
        Company company,
        User adminUser
) {
}
