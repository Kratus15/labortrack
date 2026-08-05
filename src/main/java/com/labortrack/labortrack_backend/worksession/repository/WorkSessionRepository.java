package com.labortrack.labortrack_backend.worksession.repository;

import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.worksession.entity.WorkSession;
import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
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

    /**
     * Count open work sessions for the given company. Because
     * each employee can have only one open session, this also
     * represents the number of employees currently clocked in.
     */
    long countByCompany_IdAndStatus(Long companyId, WorkSessionStatus status);

    /**
     * Find all open work sessions for the given company, ordered with the most
     * recent clock-in first.
     */
    List<WorkSession> findByCompany_IdAndStatusOrderByClockInTimeDesc(
            Long companyId,
            WorkSessionStatus status
    );

    /**
     * Find the ten most recent work sessions for the given company.
     */
    List<WorkSession> findTop10ByCompany_IdOrderByClockInTimeDesc(Long companyId);

    /**
     * Find the ten most recent work sessions for the given employee.
     */
    List<WorkSession> findTop10ByEmployee_IdOrderByClockInTimeDesc(Long employeeId);

    /**
     * Find the employee's current open work session if exists.
     */
    Optional<WorkSession> findFirstByEmployee_IdAndStatusOrderByClockInTimeDesc(
            Long employeeId,
            WorkSessionStatus status
    );

    /**
     * Finds company work sessions that overlap the requested time range.
     * This includes sessions that started before the range but continued
     * into it, and open sessions that are still running. This is important
     * when start time was day or meridian before, and you still count them.
     */
    @Query("""
        SELECT ws
        FROM WorkSession ws
        WHERE ws.company.id = :companyId
          AND ws.clockInTime < :rangeEnd
          AND (ws.clockOutTime IS NULL OR ws.clockOutTime > :rangeStart)
        """)
    List<WorkSession> findCompanySessionsOverlappingRange(
            @Param("companyId") Long companyId,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    /**
     * Finds ONE employee's work sessions that overlap the requested time range.
     * This includes sessions that started before the range but continued
     * into it, and open sessions that are still running. This is important
     * when start time was day or meridian before, and you still count them.
     */
    @Query("""
        SELECT ws
        FROM WorkSession ws
        WHERE ws.employee.id = :employeeId
          AND ws.clockInTime < :rangeEnd
          AND (ws.clockOutTime IS NULL OR ws.clockOutTime > :rangeStart)
        """)
    List<WorkSession> findEmployeeSessionsOverlappingRange(
            @Param("employeeId") Long employeeId,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );
}
