package com.labortrack.labortrack_backend.worksession.service;

import com.labortrack.labortrack_backend.common.exception.InvalidWorkSessionStateException;
import com.labortrack.labortrack_backend.common.exception.ResourceNotFoundException;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.employee.repository.EmployeeRepository;
import com.labortrack.labortrack_backend.worksession.entity.WorkSession;
import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;
import com.labortrack.labortrack_backend.worksession.repository.WorkSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class WorkSessionService {

    private final WorkSessionRepository workSessionRepository;
    private final EmployeeRepository employeeRepository;

    public WorkSessionService(
            WorkSessionRepository workSessionRepository,
            EmployeeRepository employeeRepository) {
        this.workSessionRepository = workSessionRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Employee clocks in (action). This action should only
     * be performed by ACTIVE employees, does not have an open
     * work-session already, then create an open work-session.
     * Again, employee must exist, be active, and must not have
     * an open punch already.
     */
    @Transactional
    public WorkSession clockIn(Long employeeId) {

        // validate employee exists
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(
                () -> new ResourceNotFoundException("Employee with id=" + employeeId + " not found.")
        );

        // validate employee is active
        if (!employee.getStatus().equals(EmployeeStatus.ACTIVE)) {
            throw new InvalidWorkSessionStateException("Employee with id=" + employeeId + " is not active and can't clock in.");
        }

        // validate employee doesn't have an open worksession already
        boolean hasOpenWorkSession= workSessionRepository.findByEmployeeAndStatus(employee, WorkSessionStatus.OPEN).isPresent();
        if (hasOpenWorkSession) {
            throw new InvalidWorkSessionStateException("Employee with id=" + employeeId + " already has an open work session.");
        }

        WorkSession clockInWorkSession = new WorkSession();
        clockInWorkSession.setEmployee(employee);
        clockInWorkSession.setCompany(employee.getCompany());
        clockInWorkSession.setClockInTime(OffsetDateTime.now(ZoneOffset.UTC));
        clockInWorkSession.setStatus(WorkSessionStatus.OPEN);

        return  workSessionRepository.save(clockInWorkSession);
    }

    /**
     * Employee clocks out (action). This action should only
     * be performed by existing employees which current has
     * an open work session. Find that open work session,
     * clock out using time/zone now, then closed the work
     * session using the status.
     */
    @Transactional
    public WorkSession clockOut(Long employeeId) {

        // validate employee exists
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(
                () -> new ResourceNotFoundException("Employee with id=" + employeeId + " not found.")
        );

        // validate employee has an open work session in order to clock out
        WorkSession openWorkSession  = workSessionRepository.findByEmployeeAndStatus(
                employee,
                WorkSessionStatus.OPEN
        ).orElseThrow(
                () -> new InvalidWorkSessionStateException("Employee with id=" + employeeId + " does not have an open work session to clock out.")
        );

        openWorkSession.setClockOutTime(OffsetDateTime.now(ZoneOffset.UTC));
        openWorkSession.setStatus(WorkSessionStatus.CLOSED);

        return workSessionRepository.save(openWorkSession);
    }

    /**
     * Calculate work session duration. To perform this operation
     * work session must not be null, can't have open status,
     * clock-in can't be null or clock-out, and clock-in must
     * be before clock-out in terms of time logic. Returns the
     * duration of a work session with valid clock-in and clock-out
     * time, throw otherwise.
     */
    @Transactional(readOnly = true)
    public Duration calculateWorkedDuration(WorkSession workSession) {
        if (workSession == null) {
            throw new IllegalArgumentException("Work session is required, can't be null.");
        }
        if (workSession.getStatus() == WorkSessionStatus.OPEN) {
            throw new InvalidWorkSessionStateException("Work session is not closed, can't calculate worked duration.");
        }
        if (workSession.getClockInTime() == null) {
            throw new InvalidWorkSessionStateException("Work session is missing clock-in time.");
        }
        if (workSession.getClockOutTime() == null) {
            throw new InvalidWorkSessionStateException("Work session is missing clock-out time.");
        }
        if (workSession.getClockOutTime().isBefore(workSession.getClockInTime())) {
            throw new InvalidWorkSessionStateException("Work session clock-out time can't be before clock-in time.");
        }

        return Duration.between(
                workSession.getClockInTime(),
                workSession.getClockOutTime()
        );
    }

    /**
     * Calculate duration of a work session by workSessionId, use helper method
     * for partial operation.
     */
    @Transactional(readOnly = true)
    public Duration calculateWorkedDurationByWorkSessionId(Long workSessionId) {
        WorkSession workSession = workSessionRepository.findById(workSessionId).orElseThrow(
                () -> new ResourceNotFoundException("Work session with id=" + workSessionId + " not found.")
        );

        return calculateWorkedDuration(workSession);
    }
}
