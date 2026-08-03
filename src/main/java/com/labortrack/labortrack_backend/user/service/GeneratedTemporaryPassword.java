package com.labortrack.labortrack_backend.user.service;

/**
 * This DTO holds a newly generated temp password and its
 * encoded form. The raw password is returned to the admin
 * once and the password hash is the value store in
 * postgreSQL.
 */
public record GeneratedTemporaryPassword(
        String rawPassword,
        String passwordHash
) {
}
