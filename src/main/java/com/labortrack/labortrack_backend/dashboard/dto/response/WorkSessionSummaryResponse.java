package com.labortrack.labortrack_backend.dashboard.dto.response;

import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;

import java.time.OffsetDateTime;

/**
 * This DTO response holds a simplified
 * version of a work session used across
 * dashboard responses.
 */
public record WorkSessionSummaryResponse(
        Long workSessionId,
        Long employeeId,
        String employeeName,
        OffsetDateTime clockInTime,
        OffsetDateTime clockOutTime,
        WorkSessionStatus status,
        Long workedMinutes
) {
}
