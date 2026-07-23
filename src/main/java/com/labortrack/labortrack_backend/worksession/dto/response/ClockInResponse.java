package com.labortrack.labortrack_backend.worksession.dto.response;

import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;

import java.time.OffsetDateTime;

public record ClockInResponse(
        Long workSessionId,
        Long employeeId,
        OffsetDateTime clockInTime,
        WorkSessionStatus status,
        String message
) {
}
