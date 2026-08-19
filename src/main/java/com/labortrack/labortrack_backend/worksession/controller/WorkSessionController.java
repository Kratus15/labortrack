package com.labortrack.labortrack_backend.worksession.controller;

import com.labortrack.labortrack_backend.common.dto.response.PageResponse;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import com.labortrack.labortrack_backend.user.enums.UserRole;
import com.labortrack.labortrack_backend.worksession.dto.response.ClockInResponse;
import com.labortrack.labortrack_backend.worksession.dto.response.ClockOutResponse;
import com.labortrack.labortrack_backend.worksession.dto.response.WorkSessionResponse;
import com.labortrack.labortrack_backend.worksession.entity.WorkSession;
import com.labortrack.labortrack_backend.worksession.service.WorkSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/employees")
public class WorkSessionController {

    /**
     * Handles workSession-related http requests under /api/employees.
     */

    private final WorkSessionService workSessionService;
    public  WorkSessionController(WorkSessionService workSessionService) {
        this.workSessionService = workSessionService;
    }

    /**
     * Perform clock-in for the specify employee. Redelegate the requests to service layer,
     * then generate a dto custom response back to the user. This action would  be thrown in
     * service layer if employee does not exist, is not currently active, or if already have
     * an open punch otherwise operation would be performed.
     */
    @PostMapping("/{employeeId}/clock-in")
    public ResponseEntity<ClockInResponse> clockIn(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal LaborTrackUserDetails authenticatedUser
            ) {

        // check that param that was pass in match the actual authenticated user
        validateEmployeeOwnership(authenticatedUser, employeeId);

        // use the authenticated employee object id instead of trusting the url value
        Long authenticatedEmployeeId = authenticatedUser.getEmployeeId();

        // clock-in using authenticated object employeeId
        WorkSession openWorkSession = workSessionService.clockIn(authenticatedEmployeeId);

        // generate clock-in response
        ClockInResponse response = new ClockInResponse(
                openWorkSession.getId(),
                openWorkSession.getEmployee().getId(),
                openWorkSession.getClockInTime(),
                openWorkSession.getStatus(),
                "Employee with id: " + authenticatedEmployeeId + ", Clocked in successfully."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Perform clock-out for specific employee. Redelegate the request to service layer,
     * then generate a dto custom response back to the user. This action would be thrown
     * in service layer if employee does not exist, or does not have an open worksession
     * to be closed.
     */
    @PostMapping("/{employeeId}/clock-out")
    public ResponseEntity<ClockOutResponse> clockOut(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal LaborTrackUserDetails authenticatedUser
    ) {

        // check that param that was pass in match the actual authenticated user
        validateEmployeeOwnership(authenticatedUser, employeeId);

        // use the authenticated employee object id instead of trusting the url value
        Long authenticatedEmployeeId = authenticatedUser.getEmployeeId();

        // clock-out
        WorkSession closedWorkSession = workSessionService.clockOut(authenticatedEmployeeId);

        // generate clock-out response
        ClockOutResponse response = new ClockOutResponse(
                closedWorkSession.getId(),
                closedWorkSession.getEmployee().getId(),
                closedWorkSession.getClockInTime(),
                closedWorkSession.getClockOutTime(),
                closedWorkSession.getStatus(),
                calculateWorkedDurationToMinutes(closedWorkSession),
                "Employee with id: " + authenticatedEmployeeId + ", Clocked out successfully."
        );

        return ResponseEntity.ok(response);
    }

    /**
     * This method-endpoint retrieves a paginated employee's
     * work-session history. Work sessions are ordered by
     * clock-in time, newest first. Employees can only retrieve
     * history only for employees in their company.
     */
    @GetMapping("/{employeeId}/work-sessions")
    public ResponseEntity<PageResponse<WorkSessionResponse>> getWorkSessions(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal LaborTrackUserDetails authenticatedUser,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "clockInTime",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        // employee can only request for their own work-session history.
        if (authenticatedUser.getRole() == UserRole.EMPLOYEE) {
            validateEmployeeOwnership(authenticatedUser, employeeId);
        }

        /*
        instead of using the param employeeId, use the authenticated user employeeId (SAFETY)
        an admin can select an employee from the url, but the service must verifies that employee
        belongs to the admin's company
         */
        Long authorizedEmployeeId =
                authenticatedUser.getRole() == UserRole.EMPLOYEE
                ? authenticatedUser.getEmployeeId()
                        : employeeId;

       Page<WorkSession> workSessionPage =
               workSessionService.getWorkSessionsForEmployee(
                       authorizedEmployeeId,
                       authenticatedUser.getCompanyId(),
                       pageable
               );

       // convert raw entity into lightweight DTO
       Page<WorkSessionResponse> responsePage =
               workSessionPage.map(this::toWorkSessionResponse);

       // send that DTO using Page format DTO
       PageResponse<WorkSessionResponse> response =
            new PageResponse<>(
            responsePage.getContent(),
            responsePage.getNumber(),
            responsePage.getSize(),
            responsePage.getTotalElements(),
            responsePage.getTotalPages(),
            responsePage.isFirst(),
            responsePage.isLast()
            );

        return ResponseEntity.ok(response);
    }

    // HELPER METHOD
    private WorkSessionResponse toWorkSessionResponse(WorkSession workSession) {
        Long workedMinutes = calculateWorkedDurationToMinutes(workSession);
        return new WorkSessionResponse(
                workSession.getId(),
                workSession.getCompany().getId(),
                workSession.getEmployee().getId(),
                workSession.getClockInTime(),
                workSession.getClockOutTime(),
                workSession.getStatus(),
                workSession.getNote(),
                workedMinutes
        );
    }
    private Long calculateWorkedDurationToMinutes(WorkSession workSession) {
        return  workSession.getClockOutTime() == null
                ? null
                : workSessionService
                .calculateWorkedDuration(workSession)
                .toMinutes();
    }
    private void validateEmployeeOwnership(
            LaborTrackUserDetails authenticatedUser,
            Long requestedEmployeeId) {
        if (!Objects.equals(
                authenticatedUser.getEmployeeId(),
                requestedEmployeeId
        )) {
            throw new AccessDeniedException(
                    "You cannot manage another employee work sessions."
            );
        }
    }

}
