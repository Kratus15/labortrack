package com.labortrack.labortrack_backend.dashboard.service;

import com.labortrack.labortrack_backend.common.exception.ResourceNotFoundException;
import com.labortrack.labortrack_backend.company.entity.Company;
import com.labortrack.labortrack_backend.company.repository.CompanyRepository;
import com.labortrack.labortrack_backend.dashboard.dto.response.*;
import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.employee.enums.EmployeeStatus;
import com.labortrack.labortrack_backend.employee.repository.EmployeeRepository;
import com.labortrack.labortrack_backend.worksession.entity.WorkSession;
import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;
import com.labortrack.labortrack_backend.worksession.repository.WorkSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This service is used to collect, calculate, and prepare
 * all dashboard data before the controllers return it.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkSessionRepository workSessionRepository;
    public DashboardService(
            CompanyRepository companyRepository,
            EmployeeRepository employeeRepository,
            WorkSessionRepository workSessionRepository
    ) {
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.workSessionRepository = workSessionRepository;
    }

    /**
     * This method builds dashboard data for the authenticated employee.
     * The employee ID will come from authenticated JWT not from request
     * param or body.
     */
    public EmployeeDashboardResponse getEmployeeDashboard(Long employeeId) {

        // load the authenticated employee if exists otherwise throw
        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee with id: " + employeeId + " not found."
                ));

        // use fixed current time
        OffsetDateTime currentTime = OffsetDateTime.now(ZoneOffset.UTC);

        // retrieve employee's current open session if exists otherwise null.
        WorkSessionSummaryResponse currentOpenSession =
                workSessionRepository
                        .findFirstByEmployee_IdAndStatusOrderByClockInTimeDesc(
                                employeeId,
                                WorkSessionStatus.OPEN
                        )
                        .map(session ->
                                toWorkSessionSummary(session, currentTime)
                        )
                        .orElse(null);

        // calculate given employee worked minutes during company local calendar today.
        long todayWorkedMinutes = calculateEmployeeTodayWorkedMinutes(
                employee,
                currentTime
        );

        // retrieve the ten most recent work sessions of the employee
        // convert each into a more lightweight response DTO
        List<WorkSessionSummaryResponse> recentWorkSessions =
                workSessionRepository
                        .findTop10ByEmployee_IdOrderByClockInTimeDesc(employeeId)
                        .stream()
                        .map(session ->
                                toWorkSessionSummary(
                                        session,
                                        currentTime
                                )).toList();

        return new  EmployeeDashboardResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getUser().getEmail(),
                employee.getPhone(),
                employee.getHourlyRate(),
                employee.getHireDate(),
                employee.getStatus(),
                employee.getProfileImageUrl(),
                currentOpenSession != null,
                currentOpenSession,
                todayWorkedMinutes,
                recentWorkSessions
        );
    }

    /**
     * This method returns all currently open work sessions for the
     * authenticated company. Company filtering ensures that an
     * admin can only see employees from their own company.
     */
    public List<WorkSessionSummaryResponse> getOpenWorkSessions(Long companyId) {

        // using fixed time
        OffsetDateTime currentTime = OffsetDateTime.now(ZoneOffset.UTC);

        // retrieve only OPEN sessions that belong to the company.
        // ordered from the most recent clock-in to the oldest
        return workSessionRepository
                .findByCompany_IdAndStatusOrderByClockInTimeDesc(
                        companyId,
                        WorkSessionStatus.OPEN
                )
                .stream()
                .map(session ->
                        toWorkSessionSummary(
                                session,
                                currentTime
                        )).toList();
    }

    /**
     * This method returns detailed information about ONE employee belonging to the
     * authenticated admin's company. Using both employeeId and companyId enforces
     * company isolation, employee can only be load if matches the company.
     */
    public AdminEmployeeDetailResponse getAdminEmployeeDetail(Long companyId, Long employeeId) {

        // load the employee only if employee and company match
        Employee employee = employeeRepository
                .findByIdAndCompany_Id(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId
                ));

        OffsetDateTime currentTime = OffsetDateTime.now(ZoneOffset.UTC);

        // find the employee's current open session if exists and convert it
        // to dashboard response DTO
        WorkSessionSummaryResponse currentOpenSession =
                workSessionRepository
                        .findFirstByEmployee_IdAndStatusOrderByClockInTimeDesc(
                                employeeId,
                                WorkSessionStatus.OPEN
                        )
                        .map(session ->
                                toWorkSessionSummary(session, currentTime)
                        )
                        .orElse(null);

        // load and convert to dashboard response DTO ten most recent work sessions for this emp
        List<WorkSessionSummaryResponse> recentWorkSessions =
                workSessionRepository
                        .findTop10ByEmployee_IdOrderByClockInTimeDesc(employeeId)
                        .stream()
                        .map(session ->
                                toWorkSessionSummary(session, currentTime)
                        ).toList();

        return new AdminEmployeeDetailResponse(
                employee.getId(),
                employee.getUser().getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getUser().getEmail(),
                employee.getPhone(),
                employee.getHourlyRate(),
                employee.getHireDate(),
                employee.getStatus(),
                employee.getProfileImageUrl(),
                currentOpenSession != null,
                currentOpenSession,
                recentWorkSessions
        );
    }
    /**
     * This method builds the dashboard data for an admin's company.
     * The companyId would come from the authenticated user's JWT
     * not from a request param. This method return a response, with
     * basic but important data the company dashboard would have like
     * total num of employees, currently active employees, etc.
     */
    public AdminDashboardResponse getAdminDashboard(Long companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + companyId
                ));

        OffsetDateTime currentTime = OffsetDateTime.now(ZoneOffset.UTC);

        long totalEmployees = employeeRepository.
                countByCompany_Id(companyId);

        long activeEmployees = employeeRepository.
                countByCompany_IdAndStatus(companyId, EmployeeStatus.ACTIVE);

        long inactiveEmployees = employeeRepository.
                countByCompany_IdAndStatus(companyId, EmployeeStatus.INACTIVE);

        long currentlyClockedInEmployees = workSessionRepository.
                countByCompany_IdAndStatus(companyId, WorkSessionStatus.OPEN);

        long todayWorkedMinutes = calculateTodayWorkedMinutes(company, currentTime);

        // load ten most recent company's work sessions and convert them to lightweight
        // DTO response
        List<WorkSessionSummaryResponse> recentWorkSessions =
                workSessionRepository.
                        findTop10ByCompany_IdOrderByClockInTimeDesc(companyId)
                        .stream()
                        .map(session ->
                                toWorkSessionSummary(session, currentTime)).toList();

        return new AdminDashboardResponse(
                totalEmployees,
                activeEmployees,
                inactiveEmployees,
                currentlyClockedInEmployees,
                todayWorkedMinutes,
                recentWorkSessions
        );
    }

    /**
     * This method returns employees belonging only to the authenticated
     * given company. This return a list of employees that each would have
     * the necessary information because of compact DTO response.
     */
    public List<AdminEmployeeListItemResponse> getAdminEmployees(
            Long companyId,
            EmployeeStatus status
    ) {
        // load all employees of the given company. If status was provided then filter.
        List<Employee> employees = status == null
                ? employeeRepository
                  .findByCompany_IdOrderByLastNameAscFirstNameAsc(companyId)
                : employeeRepository
                  .findByCompany_IdAndStatusOrderByLastNameAscFirstNameAsc(
                          companyId,
                          status
                  );

        // Load all the open work sessions of the given company and saved each work-session employee's id.
        Set<Long> clockedInEmployeeIds = workSessionRepository
                .findByCompany_IdAndStatusOrderByClockInTimeDesc(
                        companyId,
                        WorkSessionStatus.OPEN
                )
                .stream()
                .map(session -> session.getEmployee().getId())
                .collect(Collectors.toSet());

        // convert each employee of the company into a more lightweight and compact DTO response.
        return employees.stream()
                .map(employee -> new AdminEmployeeListItemResponse(
                        employee.getId(),
                        employee.getFirstName(),
                        employee.getLastName(),
                        employee.getUser().getEmail(),
                        employee.getPhone(),
                        employee.getHourlyRate(),
                        employee.getHireDate(),
                        employee.getStatus(),
                        employee.getProfileImageUrl(),
                        clockedInEmployeeIds.contains(employee.getId())
                ))
                .toList();
    }

    // HELPER METHODS
    /**
     * This method converts a WorkSession entity into the lightweight response
     * model used by dashboard and read endpoints. Only providing what is needed.
     */
    private WorkSessionSummaryResponse toWorkSessionSummary(
            WorkSession workSession,
            OffsetDateTime currentTime
    ) {
        Employee employee = workSession.getEmployee();

        // get clock-out
        OffsetDateTime effectiveClockOutTime =
                workSession.getClockOutTime() != null
                ? workSession.getClockOutTime()
                        : currentTime;

        // get work-session duration
        long workedMinutes = Math.max(
                0,
                Duration.between(
                        workSession.getClockInTime(),
                        effectiveClockOutTime
                ).toMinutes()
        );

        String employeeName =
                employee.getFirstName() + " " + employee.getLastName();

        return new WorkSessionSummaryResponse(
                workSession.getId(),
                employee.getId(),
                employeeName,
                workSession.getClockInTime(),
                workSession.getClockOutTime(),
                workSession.getStatus(),
                workedMinutes
        );
    }

    /**
     * This method calculates the total number of minutes worked
     * to the given company at a current local calendar day.
     * Work sessions that cross midnight are trimmed so only
     * the portion inside today's time range is counted.
     */
    private long calculateTodayWorkedMinutes(
            Company company,
            OffsetDateTime currentTime
    ) {

        // use company's timezone
        ZoneId companyZone = ZoneId.of(company.getTimezone());
        // convert the current time into company's timezone, the extract the
        // company's current calendar date
        LocalDate today = currentTime.atZoneSameInstant(companyZone).toLocalDate();

        // create exact beginning of today using company's timezone. E.g. today at 12:00AM
        OffsetDateTime dayStart = today
                .atStartOfDay(companyZone)
                .toOffsetDateTime();

        // create exact end and beginning of tomorrow. This acts
        // as the exclusive ending boundary for today
        OffsetDateTime dayEnd = today
                .plusDays(1)
                .atStartOfDay(companyZone)
                .toOffsetDateTime();

        /*
        Retrieve any work sessions that overlaps today's range.
        Sessions that started today, yesterday but ended today,
        and open session that are still running.
         */
        List<WorkSession> sessions =  workSessionRepository.
                findCompanySessionsOverlappingRange(
                        company.getId(),
                        dayStart,
                        dayEnd
                );

        long totalMinutes = 0;

        // process each overlapping session separately
        for (WorkSession session : sessions) {

            // if session started before today, begin counting today
            // otherwise use the real clock-in time
            OffsetDateTime effectiveStart =
                    session.getClockInTime().isBefore(dayStart)
                    ? dayStart
                            : session.getClockInTime();

            // closed session use real clock-out time otherwise
            // use current time because is an open session (still running)
            OffsetDateTime effectiveEnd =
                    session.getClockOutTime() != null
                    ? session.getClockOutTime()
                            : currentTime;

            // skip any time after today
            if (effectiveEnd.isAfter(dayEnd)) {
                effectiveEnd = dayEnd;
            }

            // can reversely calculate
            if (effectiveEnd.isAfter(effectiveStart)) {
                totalMinutes += Duration.between(
                        effectiveStart,
                        effectiveEnd
                ).toMinutes();
            }
        }

        return totalMinutes;
    }

    /**
     * This method calculates how many minutes on employee has worked
     * during the company's current local calendar day.
     */
    private long calculateEmployeeTodayWorkedMinutes(
            Employee employee,
            OffsetDateTime currentTime) {

        ZoneId companyZone = ZoneId.of(employee.getCompany().getTimezone());

        LocalDate today = currentTime
                .atZoneSameInstant(companyZone)
                .toLocalDate();

        // start of today parameter
        OffsetDateTime dayStart = today
                .atStartOfDay(companyZone)
                .toOffsetDateTime();
        // end of today parameter
        OffsetDateTime dayEnd = today
                .plusDays(1)
                .atStartOfDay(companyZone)
                .toOffsetDateTime();

        // retrieve only the given employee's sessions that overlap today.
        List<WorkSession> sessions =
                workSessionRepository
                        .findEmployeeSessionsOverlappingRange(
                                employee.getId(),
                                dayStart,
                                dayEnd
                        );

        long totalMinutes = 0;

        for (WorkSession session : sessions) {

            // if the session began before today, make default today, otherwise clock-in
            OffsetDateTime effectiveStart =
                    session.getClockInTime().isBefore(dayStart)
                    ? dayStart
                            : session.getClockInTime();

            // if open session used current time, otherwise use clock-out.
            OffsetDateTime effectiveEnd =
                    session.getClockOutTime() != null
                    ? session.getClockOutTime()
                            : currentTime;

            // skip anything after today
            if (effectiveEnd.isAfter(dayEnd)) {
                effectiveEnd = dayEnd;
            }

            // only add valid positive durations, never reversely
            if (effectiveEnd.isAfter(effectiveStart)) {
                totalMinutes += Duration.between(
                        effectiveStart,
                        effectiveEnd
                ).toMinutes();
            }
        }

        return totalMinutes;
    }
}
