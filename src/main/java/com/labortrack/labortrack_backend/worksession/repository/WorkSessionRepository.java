package com.labortrack.labortrack_backend.worksession.repository;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.worksession.entity.WorkSession;
import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {

    /**
     * Find all employee work sessions, order by clock in from newest to oldest
     */
    List<WorkSession> findByEmployeeOrderByClockInTimeDesc(Employee employee);

    /**
     * Find the employee's currently [status] work sessions.
     * DB constraint guarantees at most one OPEN work session per employee.
     * This method likely will be used for WorkSessionStatus.OPEN
     */
    Optional<WorkSession> findByEmployeeAndStatus(
            Employee employee,
            WorkSessionStatus status
    );

    /**
     * Find all company work sessions, order by clock in from newest to oldest
     */
    List<WorkSession> findByCompanyOrderByClockInTimeDesc(Company company);

    /**
     * Find the company's currently [status] work sessions.
     */
    List<WorkSession> findByCompanyAndStatus(
            Company company,
            WorkSessionStatus status
    );
}
