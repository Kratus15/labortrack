package com.labortrack.labortrack_backend.employee.repository;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
