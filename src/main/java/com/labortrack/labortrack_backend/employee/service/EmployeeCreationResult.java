package com.labortrack.labortrack_backend.employee.service;

import com.labortrack.labortrack_backend.employee.entity.Employee;

/**
 * This DTO simply create an object that holds employee
 * and temp password, and it is returned after creating
 * an employee. It contains the db information and
 * the raw temp password is returned only for the
 * creation response.
 */
public record EmployeeCreationResult(
        Employee employee,
        String temporaryPassword
) {
}
