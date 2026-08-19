package com.labortrack.labortrack_backend.employee.repository;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find all employees that belong to the given company.
     */
    List<Employee> findByCompany(Company company);

    /**
     * Find employees in the given company with the given status.
     */
   List<Employee> findByCompanyAndStatus(Company company, EmployeeStatus status);

    /**
     * Counts all employees that belong to the given company.
     * Mainly used for the admin dashboard total employee counts.
     */
   long countByCompany_Id(Long companyId);

    /**
     * Counts all employees that belong to the given company with
     * the requested status [active, inactive] also mainly used for
     * active/inactive employee dashboard totals.
     */
   long countByCompany_IdAndStatus(Long companyId, EmployeeStatus status);

    /**
     * Finds all employees that belongs to the given company.
     * Pagination and sorting are controlled by the provided Pageable
     * object
     */
   Page<Employee> findByCompany_Id(
           Long companyId,
           Pageable pageable
   );

    /**
     * Finds all employees that belong to the given company
     * and match the requested employee status [active, inactive, terminated].
     * Pagination and sorting are controlled by the provided Pageable
     * object
     */
    Page<Employee> findByCompany_IdAndStatus(
            Long companyId,
            EmployeeStatus status,
            Pageable pageable
    );

    /**
     * Finds an employee only when both employeeId and companyId match.
     * This supports company isolation by preventing an admin from
     * reading an employee who belong to another company.
     */
   Optional<Employee> findByIdAndCompany_Id(Long employeeId, Long companyId);
}
